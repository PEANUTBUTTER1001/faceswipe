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
 */
@Singleton
class ServiceController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var _isRunning = false

    fun isRunning(): Boolean = _isRunning

    fun start() {
        Intent(context, FaceSwipeForegroundService::class.java).also { intent ->
            intent.action = FaceSwipeForegroundService.ACTION_START
            context.startForegroundService(intent)
        }
        _isRunning = true
    }

    fun stop() {
        Intent(context, FaceSwipeForegroundService::class.java).also { intent ->
            intent.action = FaceSwipeForegroundService.ACTION_STOP
            context.startService(intent)
        }
        _isRunning = false
    }
}
