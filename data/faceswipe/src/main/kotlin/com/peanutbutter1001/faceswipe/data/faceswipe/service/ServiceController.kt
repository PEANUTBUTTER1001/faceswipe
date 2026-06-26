package com.peanutbutter1001.faceswipe.data.faceswipe.service

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ForegroundService 시작/중지 로직을 ViewModel에서 분리.
 *
 * ViewModel이 Context 및 서비스 클래스를 직접 참조하지 않도록 한다.
 *
 * 실행 상태(isRunning)는 자체 메모리 플래그가 아니라 AppStateManager.isServiceRunning을
 * 단일 소스로 사용한다. -> 프로세스가 살아있는 채 재시작돼도 UI와 실제 서비스 상태가 일치.
 */
@Singleton
class ServiceController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appStateManager: AppStateManager,
) {
    fun isRunning(): Boolean = appStateManager.isServiceRunning.value

    fun start() {
        Intent(context, FaceSwipeForegroundService::class.java).also { intent ->
            intent.action = FaceSwipeForegroundService.ACTION_START
            context.startForegroundService(intent)
        }
    }

    fun stop() {
        Intent(context, FaceSwipeForegroundService::class.java).also { intent ->
            intent.action = FaceSwipeForegroundService.ACTION_STOP
            context.startService(intent)
        }
    }
}
