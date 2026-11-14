package com.peanutbutter1001.faceswipe.domain.faceswipe.usecase

import com.peanutbutter1001.faceswipe.domain.faceswipe.model.AppPreferences
import com.peanutbutter1001.faceswipe.domain.faceswipe.repository.AppPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** 앱 환경설정을 관찰한다. */
class ObserveAppPreferencesUseCase @Inject constructor(
    private val repository: AppPreferencesRepository,
) {
    operator fun invoke(): Flow<AppPreferences> = repository.preferences
}

/** 앱 환경설정을 변경한다. 예: `update { it.copy(onboardingDone = true) }` */
class UpdateAppPreferencesUseCase @Inject constructor(
    private val repository: AppPreferencesRepository,
) {
    suspend operator fun invoke(transform: (AppPreferences) -> AppPreferences) {
        repository.update(transform)
    }
}
