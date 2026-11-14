package com.peanutbutter1001.faceswipe.data.faceswipe.service

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.core.content.ContextCompat
import com.peanutbutter1001.faceswipe.data.faceswipe.R

/** 타일 추가 요청 결과 */
enum class TileAddResult { Added, AlreadyAdded, NotAdded }

/**
 * 빠른 설정에 faceswipe 타일 추가를 요청한다 (Android 13+ StatusBarManager.requestAddTileService).
 * 그 미만 버전은 시스템 요청 창이 없으므로 화면에서 수동 추가 방법을 안내한다.
 * 앱이 포그라운드일 때만 시스템 창이 뜬다.
 */
object TileRequester {

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun request(context: Context, onResult: (TileAddResult) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onResult(TileAddResult.NotAdded)
            return
        }
        val statusBarManager = context.getSystemService(StatusBarManager::class.java)
            ?: return onResult(TileAddResult.NotAdded)
        statusBarManager.requestAddTileService(
            ComponentName(context, FaceSwipeTileService::class.java),
            context.getString(R.string.tile_label),
            Icon.createWithResource(context, R.drawable.ic_tile_face),
            ContextCompat.getMainExecutor(context),
        ) { result ->
            onResult(
                when (result) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> TileAddResult.Added
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> TileAddResult.AlreadyAdded
                    else -> TileAddResult.NotAdded
                }
            )
        }
    }
}
