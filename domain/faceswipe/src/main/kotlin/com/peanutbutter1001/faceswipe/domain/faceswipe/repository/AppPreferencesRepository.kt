package com.peanutbutter1001.faceswipe.domain.faceswipe.repository

import com.peanutbutter1001.faceswipe.domain.faceswipe.model.AppPreferences
import kotlinx.coroutines.flow.Flow

/**
 * 앱 환경설정 저장소. 구현체는 data 레이어에서 DataStore 기반으로 제공한다.
 */
interface AppPreferencesRepository {

    /** 저장된 환경설정. 값이 바뀔 때마다 emit한다. */
    val preferences: Flow<AppPreferences>

    /** 현재 값을 받아 변경한 값을 원자적으로 저장한다. */
    suspend fun update(transform: (AppPreferences) -> AppPreferences)
}
