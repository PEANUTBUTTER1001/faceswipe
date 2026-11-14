package com.peanutbutter1001.faceswipe.data.faceswipe.repository

import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.peanutbutter1001.faceswipe.data.faceswipe.datasource.CameraAnalyzer
import com.peanutbutter1001.faceswipe.data.faceswipe.model.toDomain
import com.peanutbutter1001.faceswipe.data.faceswipe.service.AppStateManager
import com.peanutbutter1001.faceswipe.data.faceswipe.service.LifecycleAwareFaceTracker
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.FaceData
import com.peanutbutter1001.faceswipe.domain.faceswipe.repository.FaceTrackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CameraX + ML Kit 기반 얼굴 추적 리포지토리 구현체.
 *
 * - bind()를 통해 LifecycleOwner(FaceSwipeForegroundService)에 카메라를 바인딩
 * - startTracking() / stopTracking()으로 대상 앱 포그라운드 여부에 따라 파이프라인 제어
 * - 해상도: 480x640 (배터리 최적화)
 * - 백프레셔: STRATEGY_KEEP_ONLY_LATEST (이전 프레임 버림)
 *
 * 자원 수명:
 *  - ML Kit 감지기와 분석기는 bind()~release() 주기당 한 번만 만들고, 대상 앱을 오갈 때는 재사용한다.
 *    (이전에는 대상 앱 진입마다 새 감지기를 만들고 닫지 않았다)
 *  - release()에서 카메라 해제 후 감지기를 close()한다.
 *
 * 카메라 사용 불가 감지:
 *  - 바인딩 예외, 또는 바인딩 후 CameraState 오류(다른 앱이 사용 중 등)를
 *    AppStateManager.cameraUnavailable로 알려 홈·알림에 표시한다.
 *
 * 주의: 이 클래스는 @Singleton 이므로 프로세스 생존 시 인스턴스가 유지된다.
 * recents 스와이프 등으로 서비스만 재시작되고 프로세스가 살아있는 경우에도
 * 재바인딩이 정상 동작해야 하므로, cameraExecutor가 shutdown 되었으면 재생성한다.
 */
@Singleton
class FaceTrackingRepositoryImpl @Inject constructor(
    private val appStateManager: AppStateManager,
) : FaceTrackingRepository,
    LifecycleAwareFaceTracker {

    private val _faceDataFlow = MutableSharedFlow<FaceData>(extraBufferCapacity = 1)
    override val faceDataFlow: Flow<FaceData> = _faceDataFlow.asSharedFlow()

    // val -> var: release()에서 shutdown 후 재바인딩 시 재생성 가능하도록.
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var context: Context? = null
    private var isTrackingRequested = false

    /** bind()~release() 주기 동안 재사용하는 ML Kit 감지기와 분석기 */
    private var faceDetector: FaceDetector? = null
    private var imageAnalysis: ImageAnalysis? = null

    /** 현재 바인딩된 카메라의 상태 관찰 (재바인딩·해제 시 제거) */
    private var observedCamera: Camera? = null
    private val cameraStateObserver = Observer<CameraState> { state ->
        // 오류가 있으면 사용 불가, 정상적으로 열리면 해제
        when {
            state.error != null -> appStateManager.setCameraUnavailable(true)
            state.type == CameraState.Type.OPEN -> appStateManager.setCameraUnavailable(false)
        }
    }

    override fun bind(lifecycleOwner: LifecycleOwner, context: Context) {
        this.lifecycleOwner = lifecycleOwner
        this.context = context

        ProcessCameraProvider.getInstance(context).also { future ->
            future.addListener({
                cameraProvider = future.get()
                if (isTrackingRequested) bindCamera()
            }, ContextCompat.getMainExecutor(context))
        }
    }

    override fun startTracking() {
        isTrackingRequested = true
        if (cameraProvider != null) bindCamera()
    }

    override fun stopTracking() {
        isTrackingRequested = false
        stopObservingCamera()
        cameraProvider?.unbindAll()
        // 대상 앱 밖에서는 카메라를 쓰지 않으므로 사용 불가 표시도 해제
        appStateManager.setCameraUnavailable(false)
    }

    private fun bindCamera() {
        val provider = cameraProvider ?: return
        val owner = lifecycleOwner ?: return

        // 핵심: shutdown 된 executor는 재사용 불가(작업 거부) -> 재생성.
        // 프로세스가 살아있는 채로 release() 이후 재바인딩되는 경우 대응.
        if (cameraExecutor.isShutdown || cameraExecutor.isTerminated) {
            cameraExecutor = Executors.newSingleThreadExecutor()
            imageAnalysis = null // 이전 executor에 묶인 분석기는 버리고 새로 만든다
        }

        val analysis = imageAnalysis ?: createImageAnalysis().also { imageAnalysis = it }

        try {
            provider.unbindAll()
            val camera = provider.bindToLifecycle(
                owner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                analysis
            )
            observeCamera(camera, owner)
        } catch (e: Exception) {
            // 전면 카메라 없음, 잘못된 상태 등 바인딩 자체가 실패한 경우
            Log.w(TAG, "camera bind failed", e)
            appStateManager.setCameraUnavailable(true)
        }
    }

    /** 분석기와 ML Kit 감지기를 한 번만 만든다 */
    private fun createImageAnalysis(): ImageAnalysis {
        val detector = faceDetector ?: FaceDetection.getClient(FACE_DETECTOR_OPTIONS).also { faceDetector = it }

        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(480, 640),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                )
            )
            .build()

        return ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(
                    cameraExecutor,
                    CameraAnalyzer(detector) { faceData ->
                        _faceDataFlow.tryEmit(faceData.toDomain())
                    }
                )
            }
    }

    private fun observeCamera(camera: Camera, owner: LifecycleOwner) {
        stopObservingCamera()
        observedCamera = camera
        camera.cameraInfo.cameraState.observe(owner, cameraStateObserver)
    }

    private fun stopObservingCamera() {
        observedCamera?.cameraInfo?.cameraState?.removeObserver(cameraStateObserver)
        observedCamera = null
    }

    override fun release() {
        // 카메라 해제 → 감지기 close → executor shutdown 순서.
        // 처리 중이던 감지 Task가 실패해도 CameraAnalyzer가 완료 콜백에서 프레임을 닫는다.
        stopObservingCamera()
        cameraProvider?.unbindAll()
        imageAnalysis?.clearAnalyzer()
        imageAnalysis = null
        faceDetector?.close()
        faceDetector = null
        cameraExecutor.shutdown()
        appStateManager.setCameraUnavailable(false)
    }

    private companion object {
        const val TAG = "FaceTrackingRepository"

        /**
         * 최적화 전략:
         *  - PERFORMANCE_MODE_FAST: 빠른 처리 우선
         *  - CLASSIFICATION_MODE_NONE: 윙크 제거로 눈 열림 확률이 필요 없어 분류 비활성화 (처리 비용 절감)
         *  - CONTOUR_MODE_ALL: 입술 컨투어로 입 벌림 감지
         *  - 랜드마크 비활성화: Euler 각도 + 입술 컨투어에만 집중
         */
        val FACE_DETECTOR_OPTIONS: FaceDetectorOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()
    }
}
