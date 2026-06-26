package com.peanutbutter1001.faceswipe.data.faceswipe.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureAction
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureTrigger
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.TargetApp
import com.peanutbutter1001.faceswipe.domain.faceswipe.repository.GestureMappingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore Preferences 기반 제스처 매핑 저장소.
 *
 * 저장 형식:
 *   키 = "{appKey}_{triggerKey}" (예: "youtube_head_left")
 *   값 = 액션 식별자 문자열 (예: "swipe_up", "none")
 *
 * 단순 key-value 구조로 JSON 직렬화 없이 개별 매핑을 빠르게 읽고 쓸 수 있다.
 */
@Singleton
class GestureMappingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : GestureMappingRepository {

    private val Context.gestureMappingDataStore: DataStore<Preferences>
            by preferencesDataStore(name = DATASTORE_NAME)

    override fun getMappings(
        targetApp: TargetApp,
    ): Flow<Map<GestureTrigger, GestureAction?>> {
        return context.gestureMappingDataStore.data.map { preferences ->
            GestureTrigger.settingsEntries.associateWith { trigger ->
                val key = buildPreferencesKey(targetApp, trigger)
                val storedValue = preferences[key]
                storedValue?.toGestureAction()
            }
        }
    }

    override suspend fun updateMapping(
        targetApp: TargetApp,
        trigger: GestureTrigger,
        action: GestureAction?,
    ) {
        context.gestureMappingDataStore.edit { preferences ->
            val key = buildPreferencesKey(targetApp, trigger)
            preferences[key] = action.toStorageKey()
        }
    }

    override suspend fun resetMappings(targetApp: TargetApp) {
        context.gestureMappingDataStore.edit { preferences ->
            GestureTrigger.settingsEntries.forEach { trigger ->
                val key = buildPreferencesKey(targetApp, trigger)
                preferences[key] = ACTION_NONE
            }
        }
    }

    // -- 직렬화 헬퍼 --

    /**
     * DataStore 키를 "{appKey}_{triggerKey}" 형태로 생성한다.
     */
    private fun buildPreferencesKey(
        targetApp: TargetApp,
        trigger: GestureTrigger,
    ): Preferences.Key<String> {
        val triggerKey = trigger.toStorageKey()
        return stringPreferencesKey("${targetApp.key}_$triggerKey")
    }

    companion object {
        private const val DATASTORE_NAME = "gesture_mapping_settings"

        // -- Trigger 직렬화 키 --
        private const val TRIGGER_HEAD_LEFT = "head_left"
        private const val TRIGGER_HEAD_RIGHT = "head_right"
        private const val TRIGGER_WINK_LEFT = "wink_left"
        private const val TRIGGER_WINK_RIGHT = "wink_right"
        private const val TRIGGER_MOUTH_OPEN = "mouth_open"

        // -- Action 직렬화 키 --
        private const val ACTION_SWIPE_UP = "swipe_up"
        private const val ACTION_SWIPE_DOWN = "swipe_down"
        private const val ACTION_TAP = "tap"
        private const val ACTION_NONE = "none"

        private fun GestureTrigger.toStorageKey(): String = when (this) {
            GestureTrigger.HeadLeft -> TRIGGER_HEAD_LEFT
            GestureTrigger.HeadRight -> TRIGGER_HEAD_RIGHT
            GestureTrigger.WinkLeft -> TRIGGER_WINK_LEFT
            GestureTrigger.WinkRight -> TRIGGER_WINK_RIGHT
            GestureTrigger.MouthOpen -> TRIGGER_MOUTH_OPEN
            // deprecated 항목은 설정 화면에서 사용하지 않으므로 fallback 처리
            else -> "unknown"
        }

        private fun GestureAction?.toStorageKey(): String = when (this) {
            is GestureAction.SwipeVertical -> if (directionUp) ACTION_SWIPE_UP else ACTION_SWIPE_DOWN
            is GestureAction.Tap -> ACTION_TAP
            // SwipeHorizontal, Pause는 설정 UI에서 미사용
            else -> ACTION_NONE
        }

        private fun String.toGestureAction(): GestureAction? = when (this) {
            ACTION_SWIPE_UP -> GestureAction.SwipeVertical(directionUp = true)
            ACTION_SWIPE_DOWN -> GestureAction.SwipeVertical(directionUp = false)
            ACTION_TAP -> GestureAction.Tap
            else -> null
        }
    }
}
