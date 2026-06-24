package com.peanutbutter1001.faceswipe.data.faceswipe.datasource

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.peanutbutter1001.faceswipe.data.faceswipe.model.FaceDataEntity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * CameraX ImageAnalysis.Analyzer 구현체.
 *
 * 최적화 전략:
 *  - PERFORMANCE_MODE_FAST: 빠른 처리 우선
 *  - 랜드마크/표정/윤곽선 비활성화: Euler 각도 추출에만 집중
 *  - STRATEGY_KEEP_ONLY_LATEST: 백프레셔 방지 (FaceTrackingRepositoryImpl에서 설정)
 */
class CameraAnalyzer(
    private val onFaceDetected: (FaceDataEntity) -> Unit
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .build()
    )

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                faces.firstOrNull()?.let { face ->
                    onFaceDetected(
                        FaceDataEntity(
                            eulerX = face.headEulerAngleX,
                            eulerY = face.headEulerAngleY,
                            eulerZ = face.headEulerAngleZ
                        )
                    )
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
