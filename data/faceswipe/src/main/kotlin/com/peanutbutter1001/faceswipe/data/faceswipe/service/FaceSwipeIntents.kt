package com.peanutbutter1001.faceswipe.data.faceswipe.service

import android.content.Context
import android.content.Intent

/**
 * 모듈 경계를 넘는 인텐트 계약.
 *
 * data 모듈(타일 서비스)은 app 모듈의 MainActivity 클래스를 알 수 없으므로,
 * 런처 인텐트(명시적 컴포넌트)에 action만 바꿔 MainActivity가 처리하게 한다.
 */
object FaceSwipeIntents {
    /** MainActivity가 받으면 설정 마법사를 연다 */
    const val ACTION_OPEN_SETUP = "com.peanutbutter1001.faceswipe.action.OPEN_SETUP"

    /** 앱의 MainActivity를 설정 마법사 열기 요청과 함께 여는 인텐트. 런처가 없으면 null */
    fun openSetup(context: Context): Intent? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            action = ACTION_OPEN_SETUP
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
}
