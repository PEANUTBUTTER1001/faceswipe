package com.peanutbutter1001.faceswipe.data.faceswipe.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.peanutbutter1001.faceswipe.data.faceswipe.R
import com.peanutbutter1001.faceswipe.domain.faceswipe.repository.AppPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 빠른 설정 타일: faceswipe 켜기/끄기.
 *
 * 클릭 동작
 *  - 실행 중 → 바로 중지 (중지는 백그라운드 제약이 없다)
 *  - 꺼짐 + 권한 충족 → 투명 [TileStartActivity]를 거쳐 시작.
 *    Android 12+는 백그라운드에서 포그라운드 서비스 시작을 제한하고, 카메라 타입은 예외 상황에서도
 *    백그라운드 시작이 막히므로 화면에 보이는 Activity에서 시작한다.
 *  - 권한 부족 → 패널을 닫고 앱의 설정 마법사를 연다 ([FaceSwipeIntents.ACTION_OPEN_SETUP])
 *
 * 타일 추가·제거 콜백으로 AppPreferences.tileAdded를 갱신해 홈 행에 반영한다.
 */
@AndroidEntryPoint
class FaceSwipeTileService : TileService() {

    @Inject lateinit var appStateManager: AppStateManager
    @Inject lateinit var permissionChecker: PermissionChecker
    @Inject lateinit var serviceController: ServiceController
    @Inject lateinit var appPreferencesRepository: AppPreferencesRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var listeningJob: Job? = null

    override fun onTileAdded() {
        super.onTileAdded()
        scope.launch { appPreferencesRepository.update { it.copy(tileAdded = true) } }
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        scope.launch { appPreferencesRepository.update { it.copy(tileAdded = false) } }
    }

    override fun onStartListening() {
        super.onStartListening()
        // 패널이 보이는 동안 실행 상태를 따라 타일 모양을 갱신
        listeningJob = appStateManager.isServiceRunning
            .onEach { running -> renderTile(running) }
            .launchIn(scope)
    }

    override fun onStopListening() {
        listeningJob?.cancel()
        listeningJob = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        when {
            appStateManager.isServiceRunning.value -> serviceController.stop()
            isReady() -> launchActivity(Intent(this, TileStartActivity::class.java))
            else -> FaceSwipeIntents.openSetup(this)?.let(::launchActivity)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun isReady() =
        permissionChecker.hasAccessibilityPermission() && permissionChecker.hasCameraPermission()

    private fun renderTile(running: Boolean) {
        val tile = qsTile ?: return
        tile.label = getString(R.string.tile_label)
        // 권한 부족도 INACTIVE로 둔다: UNAVAILABLE이면 클릭이 막혀 설정 마법사로 안내할 수 없다.
        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(
                when {
                    running -> R.string.tile_subtitle_on
                    isReady() -> R.string.tile_subtitle_off
                    else -> R.string.tile_subtitle_setup
                }
            )
        }
        tile.updateTile()
    }

    /**
     * 패널을 닫고 Activity 실행.
     * API 34+는 PendingIntent 오버로드만 허용되고, Intent 오버로드는 예외를 던진다.
     */
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun launchActivity(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
