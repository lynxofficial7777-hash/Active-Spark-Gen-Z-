package com.activespark.gen7.ui.screens.battle

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

/**
 * Wraps MediaPipe PoseLandmarker for real-time pose detection via CameraX.
 *
 * Running mode: LIVE_STREAM — results are delivered asynchronously via [onResults].
 * The model asset "pose_landmarker_lite.task" must be placed in assets/.
 */
class PoseLandmarkerHelper(
    private val context: Context,
    val onResults: (PoseLandmarkerResult, MPImage) -> Unit,
    val onError: (String, Int) -> Unit
) {

    private var poseLandmarker: PoseLandmarker? = null

    companion object {
        const val MODEL_POSE_LANDMARKER_LITE = "pose_landmarker_lite.task"
        const val DEFAULT_NUM_POSES = 1
        const val DEFAULT_POSE_DETECTION_CONFIDENCE = 0.5f
        const val DEFAULT_POSE_TRACKING_CONFIDENCE = 0.5f
        const val DEFAULT_POSE_PRESENCE_CONFIDENCE = 0.5f
        const val OTHER_ERROR = 0
        const val GPU_ERROR = 1
    }

    init {
        setupPoseLandmarker()
    }

    private fun setupPoseLandmarker() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_POSE_LANDMARKER_LITE)
            .setDelegate(Delegate.CPU)
            .build()

        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setMinPoseDetectionConfidence(DEFAULT_POSE_DETECTION_CONFIDENCE)
            .setMinTrackingConfidence(DEFAULT_POSE_TRACKING_CONFIDENCE)
            .setMinPosePresenceConfidence(DEFAULT_POSE_PRESENCE_CONFIDENCE)
            .setNumPoses(DEFAULT_NUM_POSES)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, image -> onResults(result, image) }
            .setErrorListener { error -> onError(error.message ?: "Unknown error", OTHER_ERROR) }
            .build()

        try {
            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
        } catch (e: IllegalStateException) {
            onError("PoseLandmarker failed to initialize: ${e.message}", OTHER_ERROR)
        } catch (e: RuntimeException) {
            onError("PoseLandmarker GPU error: ${e.message}", GPU_ERROR)
        }
    }

    /**
     * Detects pose landmarks from a CameraX [ImageProxy] frame.
     * Converts the frame to a Bitmap, applies rotation, then feeds it to the landmarker.
     */
    fun detectLiveStream(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        val frameTime = SystemClock.uptimeMillis()

        val bitmapBuffer = Bitmap.createBitmap(
            imageProxy.width,
            imageProxy.height,
            Bitmap.Config.ARGB_8888
        )
        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }

        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            if (isFrontCamera) {
                postScale(-1f, 1f, imageProxy.width.toFloat(), imageProxy.height.toFloat())
            }
        }

        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer, 0, 0,
            bitmapBuffer.width, bitmapBuffer.height,
            matrix, true
        )

        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
        poseLandmarker?.detectAsync(mpImage, frameTime)
    }

    fun close() {
        poseLandmarker?.close()
        poseLandmarker = null
    }

    fun isOpen(): Boolean = poseLandmarker != null
}
