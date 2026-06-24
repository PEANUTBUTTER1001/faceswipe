package com.peanutbutter1001.faceswipe.data.faceswipe.service

import android.content.Context
import androidx.lifecycle.LifecycleOwner

/**
 * data 레이어 전용 인터페이스.
 *
 * FaceTrackingRepositoryImpl의 bind()를 domain 인터페이스 오염 없이
 * ForegroundService에서 호출할 수 있도록 분리한다.
 *
 * domain의 FaceTrackingRepository는 순수하게 유지되고,
 * 이 인터페이스는 data 모듈 내에서만 사용된다.
 */
interface LifecycleAwareFaceTracker {
    /** CameraX LifecycleOwner 바인딩 */
    fun bind(lifecycleOwner: LifecycleOwner, context: Context)

    /** 카메라 리소스 해제 */
    fun release()
}
