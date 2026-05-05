package com.activespark.gen7.ui.screens.battle

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.nio.ByteBuffer

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
     *
     * FIX 1: Do NOT close imageProxy here — the caller (CameraPreviewView analyzer) owns it.
     * FIX 2: Handle rowStride padding so devices with padded rows don't produce corrupted frames.
     * FIX 3: Flip BEFORE rotate so the mirror pivot uses the correct (pre-rotation) dimensions.
     * FIX 4: Recycle bitmapBuffer after use to avoid memory pressure.
     */
    fun detectLiveStream(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        val frameTime = SystemClock.uptimeMillis()
        val width  = imageProxy.width
        val height = imageProxy.height

        val plane       = imageProxy.planes[0]
        val buffer      = plane.buffer
        val rowStride   = plane.rowStride
        val pixelStride = plane.pixelStride   // always 4 for RGBA_8888

        // Build a tight pixel buffer, stripping any per-row padding
        val tightBuffer: ByteBuffer = if (rowStride == width * pixelStride) {
            // No padding — use the plane buffer directly (duplicate so position is 0)
            buffer.duplicate()
        } else {
            // Padded rows — copy each row without the padding bytes
            val tight = ByteArray(width * height * pixelStride)
            val src = buffer.duplicate()
            for (row in 0 until height) {
                src.position(row * rowStride)
                val rowBytes = minOf(width * pixelStride, src.remaining())
                src.get(tight, row * width * pixelStride, rowBytes)
            }
            ByteBuffer.wrap(tight)
        }

        val bitmapBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmapBuffer.copyPixelsFromBuffer(tightBuffer)

        // FIX 3: Apply horizontal mirror FIRST (using original width/height as pivot),
        //         THEN rotate — this keeps the pivot correct for all rotation angles.
        val rotation = imageProxy.imageInfo.rotationDegrees.toFloat()
        val matrix = Matrix().apply {
            if (isFrontCamera) {
                postScale(-1f, 1f, width / 2f, height / 2f)
            }
            postRotate(rotation)
        }

        val rotatedBitmap = Bitmap.createBitmap(bitmapBuffer, 0, 0, width, height, matrix, true)
        bitmapBuffer.recycle()   // FIX 4: release immediately after transform

        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
        poseLandmarker?.detectAsync(mpImage, frameTime)
        // rotatedBitmap ownership passes to MediaPipe — do NOT recycle here
    }

    fun close() {
        poseLandmarker?.close()
        poseLandmarker = null
    }

    fun isOpen(): Boolean = poseLandmarker != null
}
