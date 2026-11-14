package com.peanutbutter1001.faceswipe.data.faceswipe.datasource

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.peanutbutter1001.faceswipe.data.faceswipe.model.FaceDataEntity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetector

/**
 * CameraX ImageAnalysis.Analyzer 구현체.
 *
 * ML Kit 감지기는 직접 만들지 않고 주입받는다. 생성·재사용·close는
 * FaceTrackingRepositoryImpl이 bind()~release() 주기로 관리한다 (감지기 누수 방지).
 * 감지기 옵션과 STRATEGY_KEEP_ONLY_LATEST 백프레셔도 FaceTrackingRepositoryImpl에서 설정한다.
 */
class CameraAnalyzer(
    private val detector: FaceDetector,
    private val onFaceDetected: (FaceDataEntity) -> Unit
) : ImageAnalysis.Analyzer {

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
                            eulerZ = face.headEulerAngleZ,
                            mouthOpenRatio = calculateMouthOpenRatio(
                                face.getContour(FaceContour.UPPER_LIP_BOTTOM)?.points,
                                face.getContour(FaceContour.LOWER_LIP_TOP)?.points,
                                face.boundingBox.height().toFloat()
                            )
                        )
                    )
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    /**
     * 윗입술 하단 컨투어와 아랫입술 상단 컨투어의 중앙점 거리를
     * 얼굴 바운딩박스 높이로 정규화하여 입 벌림 비율을 계산한다.
     *
     * @return 입 벌림 비율 (0에 가까우면 다문 상태, 0.08+ 이면 벌린 상태), null이면 감지 불가
     */
    private fun calculateMouthOpenRatio(
        upperLipBottom: List<android.graphics.PointF>?,
        lowerLipTop: List<android.graphics.PointF>?,
        faceHeight: Float
    ): Float? {
        if (upperLipBottom.isNullOrEmpty() || lowerLipTop.isNullOrEmpty() || faceHeight <= 0f) {
            return null
        }
        val upperCenter = upperLipBottom[upperLipBottom.size / 2]
        val lowerCenter = lowerLipTop[lowerLipTop.size / 2]
        val lipGap = lowerCenter.y - upperCenter.y
        return (lipGap / faceHeight).coerceAtLeast(0f)
    }
}
