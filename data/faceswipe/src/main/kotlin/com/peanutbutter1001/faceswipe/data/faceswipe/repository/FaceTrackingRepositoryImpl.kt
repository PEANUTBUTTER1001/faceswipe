package com.peanutbutter1001.faceswipe.data.faceswipe.repository

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.peanutbutter1001.faceswipe.data.faceswipe.datasource.CameraAnalyzer
import com.peanutbutter1001.faceswipe.data.faceswipe.model.toDomain
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
 * 주의: 이 클래스는 @Singleton 이므로 프로세스 생존 시 인스턴스가 유지된다.
 * recents 스와이프 등으로 서비스만 재시작되고 프로세스가 살아있는 경우에도
 * 재바인딩이 정상 동작해야 하므로, cameraExecutor가 shutdown 되었으면 재생성한다.
 */
@Singleton
class FaceTrackingRepositoryImpl @Inject constructor() :
    FaceTrackingRepository,
    LifecycleAwareFaceTracker {

    private val _faceDataFlow = MutableSharedFlow<FaceData>(extraBufferCapacity = 1)
    override val faceDataFlow: Flow<FaceData> = _faceDataFlow.asSharedFlow()

    // val -> var: release()에서 shutdown 후 재바인딩 시 재생성 가능하도록.
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var context: Context? = null
    private var isTrackingRequested = false

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
        cameraProvider?.unbindAll()
    }

    private fun bindCamera() {
        val provider = cameraProvider ?: return
        val owner = lifecycleOwner ?: return

        // 핵심: shutdown 된 executor는 재사용 불가(작업 거부) -> 재생성.
        // 프로세스가 살아있는 채로 release() 이후 재바인딩되는 경우 대응.
        if (cameraExecutor.isShutdown || cameraExecutor.isTerminated) {
            cameraExecutor = Executors.newSingleThreadExecutor()
        }

        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(480, 640),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                )
            )
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(
                    cameraExecutor,
                    CameraAnalyzer { faceData ->
                        _faceDataFlow.tryEmit(faceData.toDomain())
                    }
                )
            }

        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                owner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                imageAnalysis
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun release() {
        // 카메라 리소스만 해제. executor는 shutdown 하되, 다음 bindCamera에서 재생성된다.
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }
}
