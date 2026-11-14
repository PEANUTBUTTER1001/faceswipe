package com.peanutbutter1001.faceswipe.data.faceswipe.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.AppPreferences
import com.peanutbutter1001.faceswipe.domain.faceswipe.repository.AppPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore Preferences 기반 앱 환경설정 저장소.
 *
 * 제스처 매핑(gesture_mapping_settings)과 파일을 분리해, 기존 매핑 데이터에 영향이 없다.
 */
@Singleton
class AppPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppPreferencesRepository {

    private val Context.appPreferencesDataStore: DataStore<Preferences>
            by preferencesDataStore(name = DATASTORE_NAME)

    override val preferences: Flow<AppPreferences> =
        context.appPreferencesDataStore.data
            .map { it.toAppPreferences() }
            .distinctUntilChanged()

    override suspend fun update(transform: (AppPreferences) -> AppPreferences) {
        // edit 블록 안에서 현재 값을 읽고 쓰므로 동시 갱신에도 원자적이다.
        context.appPreferencesDataStore.edit { prefs ->
            val updated = transform(prefs.toAppPreferences())
            prefs[KEY_ONBOARDING_DONE] = updated.onboardingDone
            prefs[KEY_SHOW_GESTURE_OVERLAY] = updated.showGestureOverlay
            prefs[KEY_TILE_ADDED] = updated.tileAdded
            prefs[KEY_CAMERA_REQUESTED] = updated.cameraPermissionRequested
            prefs[KEY_CAMERA_RATIONALE_SEEN] = updated.cameraRationaleSeen
            prefs[KEY_NOTIFICATION_REQUESTED] = updated.notificationPermissionRequested
        }
    }

    private fun Preferences.toAppPreferences(): AppPreferences {
        val default = AppPreferences()
        return AppPreferences(
            onboardingDone = this[KEY_ONBOARDING_DONE] ?: default.onboardingDone,
            showGestureOverlay = this[KEY_SHOW_GESTURE_OVERLAY] ?: default.showGestureOverlay,
            tileAdded = this[KEY_TILE_ADDED] ?: default.tileAdded,
            cameraPermissionRequested = this[KEY_CAMERA_REQUESTED] ?: default.cameraPermissionRequested,
            cameraRationaleSeen = this[KEY_CAMERA_RATIONALE_SEEN] ?: default.cameraRationaleSeen,
            notificationPermissionRequested = this[KEY_NOTIFICATION_REQUESTED]
                ?: default.notificationPermissionRequested,
        )
    }

    private companion object {
        const val DATASTORE_NAME = "app_preferences"
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_SHOW_GESTURE_OVERLAY = booleanPreferencesKey("show_gesture_overlay")
        val KEY_TILE_ADDED = booleanPreferencesKey("tile_added")
        val KEY_CAMERA_REQUESTED = booleanPreferencesKey("camera_permission_requested")
        val KEY_CAMERA_RATIONALE_SEEN = booleanPreferencesKey("camera_rationale_seen")
        val KEY_NOTIFICATION_REQUESTED = booleanPreferencesKey("notification_permission_requested")
    }
}
