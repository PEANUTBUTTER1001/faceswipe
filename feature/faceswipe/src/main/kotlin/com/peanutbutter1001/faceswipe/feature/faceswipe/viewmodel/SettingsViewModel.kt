package com.peanutbutter1001.faceswipe.feature.faceswipe.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureAction
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureTrigger
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.TargetApp
import com.peanutbutter1001.faceswipe.domain.faceswipe.usecase.GetGestureMappingUseCase
import com.peanutbutter1001.faceswipe.domain.faceswipe.usecase.UpdateGestureMappingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 제스처 설정 화면의 ViewModel.
 *
 * 단방향 데이터 흐름(UDF):
 *   UI 이벤트 → ViewModel 함수 호출 → DataStore 갱신 → Flow emit → UI 갱신
 *
 * 앱 탭 전환 시 selectedApp을 변경하면 flatMapLatest로
 * 해당 앱의 매핑을 자동으로 구독한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getGestureMapping: GetGestureMappingUseCase,
    private val updateGestureMapping: UpdateGestureMappingUseCase,
) : ViewModel() {

    /** 현재 선택된 대상 앱 */
    private val _selectedApp = MutableStateFlow(TargetApp.YOUTUBE)

    /**
     * UI 상태. 앱 전환 시 자동으로 해당 앱의 매핑을 구독한다.
     *
     * flatMapLatest: selectedApp이 바뀌면 이전 Flow 구독을 취소하고
     * 새 앱의 매핑 Flow를 구독한다.
     */
    /** 마지막 전체 해제 직전 매핑 (실행 취소용). 스낵바가 닫히면 비운다. */
    private val lastReset = MutableStateFlow<ResetSnapshot?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        _selectedApp.flatMapLatest { app ->
            getGestureMapping(app).map { mappings -> app to mappings }
        },
        lastReset,
    ) { (app, mappings), reset ->
        SettingsUiState.Success(
            selectedApp = app,
            mappings = mappings,
            pendingUndoApp = reset?.app,
        ) as SettingsUiState
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState.Loading,
        )

    /**
     * 대상 앱 탭을 전환한다.
     */
    fun selectApp(targetApp: TargetApp) {
        _selectedApp.value = targetApp
    }

    /**
     * 특정 제스처의 액션 매핑을 변경한다.
     * 변경 즉시 DataStore에 persist된다.
     */
    fun updateAction(trigger: GestureTrigger, action: GestureAction?) {
        viewModelScope.launch {
            updateGestureMapping(
                targetApp = _selectedApp.value,
                trigger = trigger,
                action = action,
            )
        }
    }

    /**
     * 현재 선택된 앱의 모든 매핑을 초기화한다.
     */
    fun resetCurrentApp() {
        val current = uiState.value as? SettingsUiState.Success ?: return
        // 해제 직전 매핑을 보관해 두고, 스낵바의 "실행 취소"로 한 번에 복원한다.
        lastReset.value = ResetSnapshot(current.selectedApp, current.mappings)
        viewModelScope.launch {
            updateGestureMapping.reset(current.selectedApp)
        }
    }

    /** 전체 해제 실행 취소: 보관한 매핑을 단일 트랜잭션으로 복원 */
    fun undoReset() {
        val snapshot = lastReset.value ?: return
        lastReset.value = null
        viewModelScope.launch {
            updateGestureMapping.restore(snapshot.app, snapshot.mappings)
        }
    }

    /** 실행 취소 스낵바가 시간 초과·닫힘으로 사라짐 → 복원 불가 */
    fun onUndoDismissed() {
        lastReset.value = null
    }

    private data class ResetSnapshot(
        val app: TargetApp,
        val mappings: Map<GestureTrigger, GestureAction?>,
    )
}
