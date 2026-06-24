package com.peanutbutter1001.faceswipe.data.repository

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.peanutbutter1001.faceswipe.data.datasource.CameraAnalyzer
import com.peanutbutter1001.faceswipe.data.model.FaceDataEntity
import com.peanutbutter1001.faceswipe.domain.repository.FaceTrackingRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Size

/**
 * CameraX + ML Kit 기반 얼굴 추적 리포지토리 구현체.
 *
 * - bind()를 통해 LifecycleOwner(faceswipeForegroundService)에 카메라를 바인딩
 * - startTracking() / stopTracking()으로 유튜브 포그라운드 여부에 따라 파이프라인 제어
 * - 해상도: 480x640 (배터리 최적화)
 * - 백프레셔: STRATEGY_KEEP_ONLY_LATEST (이전 프레임 버림)
 */
@Singleton
class FaceTrackingRepositoryImpl @Inject constructor() : FaceTrackingRepository {

    private val _faceDataFlow = MutableSharedFlow<FaceDataEntity>(extraBufferCapacity = 1)
    override val faceDataFlow: SharedFlow<FaceDataEntity> = _faceDataFlow.asSharedFlow()

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var context: Context? = null
    private var isTrackingRequested = false

    /**
     * faceswipeForegroundService에서 호출.
     * LifecycleService를 LifecycleOwner로 등록하여 CameraX 바인딩 준비.
     */
    fun bind(lifecycleOwner: LifecycleOwner, context: Context) {
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
                    CameraAnalyzer { faceData -> _faceDataFlow.tryEmit(faceData) }
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

    fun release() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }
}
