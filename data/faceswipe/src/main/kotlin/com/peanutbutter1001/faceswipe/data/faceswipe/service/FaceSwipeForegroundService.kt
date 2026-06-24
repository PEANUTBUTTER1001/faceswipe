package com.peanutbutter1001.faceswipe.data.faceswipe.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.peanutbutter1001.faceswipe.domain.faceswipe.repository.FaceTrackingRepository
import com.peanutbutter1001.faceswipe.domain.faceswipe.usecase.AnalyzeFaceMovementUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * 카메라 라이프사이클 유지 담당 포그라운드 서비스.
 *
 * LifecycleService를 상속하여 CameraX ProcessCameraProvider에 LifecycleOwner로 등록됨.
 * AppStateManager.isYouTubeActive를 구독하여:
 *  - true  -> CameraX + ML Kit 파이프라인 Resume
 *  - false -> 파이프라인 Pause (배터리 방어 핵심 로직)
 */
@AndroidEntryPoint
class FaceSwipeForegroundService : LifecycleService() {

    @Inject lateinit var repository: FaceTrackingRepository
    @Inject lateinit var lifecycleAwareTracker: LifecycleAwareFaceTracker
    @Inject lateinit var analyzeUseCase: AnalyzeFaceMovementUseCase
    @Inject lateinit var appStateManager: AppStateManager

    companion object {
        const val ACTION_START = "com.peanutbutter1001.faceswipe.ACTION_START"
        const val ACTION_STOP = "com.peanutbutter1001.faceswipe.ACTION_STOP"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "faceswipe_channel"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> startFaceSwipe()
            ACTION_STOP -> stopFaceSwipe()
        }
        return START_STICKY
    }

    private fun startFaceSwipe() {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        // LifecycleService를 LifecycleOwner로 CameraX에 바인딩
        lifecycleAwareTracker.bind(this, this)

        // [핵심 배터리 방어 로직]
        // YouTube가 포그라운드일 때만 카메라 활성화
        appStateManager.isYouTubeActive
            .onEach { isActive ->
                if (isActive) repository.startTracking()
                else repository.stopTracking()
            }
            .launchIn(lifecycleScope)

        // 제스처 액션 발행 -> GestureAccessibilityService가 수신하여 디스패치
        analyzeUseCase()
            .onEach { action -> appStateManager.gestureAction.tryEmit(action) }
            .launchIn(lifecycleScope)
    }

    private fun stopFaceSwipe() {
        repository.stopTracking()
        lifecycleAwareTracker.release()
        appStateManager.setYouTubeActive(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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
        .setContentText("유튜브 실행 시 자동으로 제스처가 작동합니다")
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
