package com.peanutbutter1001.faceswipe.data.faceswipe.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureAction
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureTrigger
import com.peanutbutter1001.faceswipe.domain.faceswipe.repository.FaceTrackingRepository
import com.peanutbutter1001.faceswipe.domain.faceswipe.repository.GestureMappingRepository
import com.peanutbutter1001.faceswipe.domain.faceswipe.usecase.AnalyzeFaceMovementUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 카메라 라이프사이클 유지 담당 포그라운드 서비스.
 *
 * 제스처 처리 구조:
 *  - 감지(detection): analyzeUseCase()로 raw GestureTrigger 스트림을 한 번만 구독해 연속 실행.
 *  - 매핑(mapping): currentTargetApp의 DataStore 매핑을 StateFlow로 유지, trigger 시점에 .value 조회.
 *
 * 종료/재실행 대응:
 *  - startForeground()는 가드보다 먼저 호출(startForegroundService 후 5초 내 호출 보장 -> 크래시 방지).
 *  - isRunning 가드로 중복 시작(흐름 중복 구독) 방지.
 *  - 시작 시 requestForegroundResync()로 현재 포그라운드 앱 재동기화 요청.
 *  - null intent(START_STICKY 재시작) 시에도 자동 재개.
 *  - onTaskRemoved: recents에서 스와이프로 닫으면 깨끗하게 종료(좀비 상태 방지).
 */
@AndroidEntryPoint
class FaceSwipeForegroundService : LifecycleService() {

    @Inject lateinit var repository: FaceTrackingRepository
    @Inject lateinit var lifecycleAwareTracker: LifecycleAwareFaceTracker
    @Inject lateinit var analyzeUseCase: AnalyzeFaceMovementUseCase
    @Inject lateinit var appStateManager: AppStateManager
    @Inject lateinit var gestureMappingRepository: GestureMappingRepository

    private var isRunning = false

    companion object {
        const val ACTION_START = "com.peanutbutter1001.faceswipe.ACTION_START"
        const val ACTION_STOP = "com.peanutbutter1001.faceswipe.ACTION_STOP"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "faceswipe_channel"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START, null -> startFaceSwipe()
            ACTION_STOP -> stopFaceSwipe()
        }
        return START_STICKY
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startFaceSwipe() {
        // startForeground는 가드보다 먼저 — startForegroundService 후 5초 내 미호출 시 크래시 방지.
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        if (isRunning) {
            appStateManager.requestForegroundResync()
            return
        }
        isRunning = true
        appStateManager.setServiceRunning(true)

        // LifecycleService를 LifecycleOwner로 CameraX에 바인딩
        lifecycleAwareTracker.bind(this, this)

        // 대상 앱이 포그라운드일 때만 카메라 활성화 (배터리 방어)
        appStateManager.isTargetAppActive
            .onEach { isActive ->
                if (isActive) repository.startTracking()
                else repository.stopTracking()
            }
            .launchIn(lifecycleScope)

        // 현재 앱의 매핑을 StateFlow로 유지 (앱 전환 시 가벼운 매핑 Flow만 교체)
        val currentMappings: StateFlow<Map<GestureTrigger, GestureAction?>> =
            appStateManager.currentTargetApp
                .flatMapLatest { app ->
                    if (app != null) gestureMappingRepository.getMappings(app)
                    else flowOf(emptyMap())
                }
                .stateIn(lifecycleScope, SharingStarted.Eagerly, emptyMap())

        // 감지 파이프라인: 한 번만 구독되어 연속 실행. trigger 시점에 현재 앱 매핑 조회 -> action 발행.
        analyzeUseCase()
            .onEach { trigger ->
                val action = currentMappings.value[trigger]
                if (action != null) appStateManager.gestureAction.tryEmit(action)
            }
            .launchIn(lifecycleScope)

        // 시작 직후 현재 포그라운드 앱 재동기화
        appStateManager.requestForegroundResync()
    }

    private fun stopFaceSwipe() {
        isRunning = false
        appStateManager.setServiceRunning(false)
        repository.stopTracking()
        lifecycleAwareTracker.release()
        appStateManager.setTargetAppActive(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * recents에서 앱을 스와이프해 닫으면 호출. 좀비 상태 방지를 위해 깨끗하게 종료.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        stopFaceSwipe()
        super.onTaskRemoved(rootIntent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "faceswipe 실행 중",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "faceswipe이 백그라운드에서 실행 중입니다"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_menu_camera)
        .setContentTitle("faceswipe 활성화")
        .setContentText("대상 앱 실행 시 자동으로 제스처가 작동합니다")
        .setOngoing(true)
        .setSilent(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0,
                packageManager.getLaunchIntentForPackage(packageName),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .addAction(
            0, "중지",
            PendingIntent.getService(
                this, 1,
                Intent(this, FaceSwipeForegroundService::class.java).apply {
                    action = ACTION_STOP
                },
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()
}
