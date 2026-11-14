package com.peanutbutter1001.faceswipe.data.faceswipe.service

import android.app.Activity
import android.content.Intent

/**
 * 빠른 설정 타일에서 faceswipe를 켤 때만 쓰는 투명 트램펄린 Activity.
 *
 * 카메라 타입 포그라운드 서비스는 앱이 백그라운드일 때 시작할 수 없으므로(Android 12+, 14+ 강화),
 * 화면에 보이는 이 Activity가 resume된 시점에 서비스를 시작하고 즉시 종료한다.
 * 화면 요소가 없고 최근 앱 목록에도 남지 않는다 (manifest: Translucent 테마, excludeFromRecents).
 */
class TileStartActivity : Activity() {

    override fun onResume() {
        super.onResume()
        startForegroundService(
            Intent(this, FaceSwipeForegroundService::class.java)
                .setAction(FaceSwipeForegroundService.ACTION_START)
        )
        finish()
    }
}
