package com.peanutbutter1001.faceswipe.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.peanutbutter1001.faceswipe.MainActivity
import com.peanutbutter1001.faceswipe.R
import com.peanutbutter1001.faceswipe.data.repository.FaceTrackingRepositoryImpl
import com.peanutbutter1001.faceswipe.domain.usecase.AnalyzeFaceMovementUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * 카메라 라이프사이클 유지 담당 포그라운드 서비스.
 *
 * LifecycleService를 상속하여 CameraX ProcessCameraProvider에 LifecycleOwner로 등록됨.
 * AppStateManager.isYouTubeActive를 구독하여:
 *  - true  → CameraX + ML Kit 파이프라인 Resume
 *  - false → 파이프라인 Pause (배터리 방어 핵심 로직)
 */
@AndroidEntryPoint
class faceswipeForegroundService : LifecycleService() {

    @Inject lateinit var repository: FaceTrackingRepositoryImpl
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
            ACTION_START -> startfaceswipe()
            ACTION_STOP -> stopfaceswipe()
        }
        return START_STICKY
    }

    private fun startfaceswipe() {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        // LifecycleService를 LifecycleOwner로 CameraX에 바인딩
        repository.bind(this, this)

        // [핵심 배터리 방어 로직]
        // YouTube가 포그라운드일 때만 카메라 활성화
        appStateManager.isYouTubeActive
            .onEach { isActive ->
                if (isActive) repository.startTracking()
                else repository.stopTracking()
            }
            .launchIn(lifecycleScope)

        // 제스처 트리거 발행 → GestureAccessibilityService가 수신하여 스와이프
        analyzeUseCase()
            .onEach { trigger -> appStateManager.gestureTrigger.tryEmit(trigger) }
            .launchIn(lifecycleScope)
    }

    private fun stopfaceswipe() {
        repository.stopTracking()
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
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("faceswipe 활성화")
        .setContentText("유튜브 실행 시 자동으로 제스처가 작동합니다")
        .setOngoing(true)
        .setSilent(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .addAction(
            0, "중지",
            PendingIntent.getService(
                this, 1,
                Intent(this, faceswipeForegroundService::class.java).apply {
                    action = ACTION_STOP
                },
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()
}
