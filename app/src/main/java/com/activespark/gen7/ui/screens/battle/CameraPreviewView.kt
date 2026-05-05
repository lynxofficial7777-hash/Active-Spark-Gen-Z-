package com.activespark.gen7.ui.screens.battle

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.activespark.gen7.data.models.ExerciseType
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// ─── Landmark Overlay View ────────────────────────────────────────────────────

/**
 * Custom View that draws MediaPipe pose landmarks on top of the camera preview.
 * Renders 33 landmark dots and connecting skeleton lines in neon cyan.
 */
class LandmarkOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var results: PoseLandmarkerResult? = null
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    private val dotPaint = Paint().apply {
        color = Color.parseColor("#00F5FF")   // NeonCyan
        style = Paint.Style.FILL
        strokeWidth = 8f
        isAntiAlias = true
    }

    private val linePaint = Paint().apply {
        color = Color.parseColor("#4000F5FF") // NeonCyan 25% alpha
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val glowPaint = Paint().apply {
        color = Color.parseColor("#2000F5FF")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // MediaPipe pose skeleton connections (landmark index pairs)
    private val connections = listOf(
        // Face
        0 to 1, 1 to 2, 2 to 3, 3 to 7,
        0 to 4, 4 to 5, 5 to 6, 6 to 8,
        // Torso
        11 to 12, 11 to 23, 12 to 24, 23 to 24,
        // Left arm
        11 to 13, 13 to 15, 15 to 17, 15 to 19, 15 to 21, 17 to 19,
        // Right arm
        12 to 14, 14 to 16, 16 to 18, 16 to 20, 16 to 22, 18 to 20,
        // Left leg
        23 to 25, 25 to 27, 27 to 29, 27 to 31, 29 to 31,
        // Right leg
        24 to 26, 26 to 28, 28 to 30, 28 to 32, 30 to 32
    )

    fun setResults(poseLandmarkerResult: PoseLandmarkerResult, imgWidth: Int, imgHeight: Int) {
        results = poseLandmarkerResult
        imageWidth = imgWidth
        imageHeight = imgHeight
        invalidate()
    }

    fun clear() {
        results = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val result = results ?: return
        if (result.landmarks().isEmpty()) return

        val landmarks = result.landmarks()[0]
        val scaleX = width.toFloat() / imageWidth.toFloat()
        val scaleY = height.toFloat() / imageHeight.toFloat()

        // Draw skeleton connections
        for ((startIdx, endIdx) in connections) {
            if (startIdx < landmarks.size && endIdx < landmarks.size) {
                val start = landmarks[startIdx]
                val end = landmarks[endIdx]
                canvas.drawLine(
                    start.x() * width,
                    start.y() * height,
                    end.x() * width,
                    end.y() * height,
                    linePaint
                )
            }
        }

        // Draw landmark dots with glow
        for (landmark in landmarks) {
            val x = landmark.x() * width
            val y = landmark.y() * height
            // Glow
            canvas.drawCircle(x, y, 12f, glowPaint)
            // Dot
            canvas.drawCircle(x, y, 5f, dotPaint)
        }
    }
}

// ─── Compose Camera Preview ───────────────────────────────────────────────────

/**
 * Composable that binds CameraX to a [PreviewView], feeds frames to
 * [PoseLandmarkerHelper], and overlays detected landmarks via [LandmarkOverlayView].
 *
 * @param exerciseType  Determines which rep-counting logic to use
 * @param onRepDetected Called when a full rep is completed, with the form score
 * @param modifier      Layout modifier
 */
@Composable
fun CameraPreviewView(
    exerciseType: ExerciseType,
    onRepDetected: (formScore: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val isFrontCamera = true

    // Hold references that survive recomposition
    val overlayRef = remember { mutableStateOf<LandmarkOverlayView?>(null) }
    val helperRef = remember { mutableStateOf<PoseLandmarkerHelper?>(null) }
    val repCounterRef = remember { mutableStateOf(RepCounter(exerciseType)) }
    val executorRef = remember { mutableStateOf<ExecutorService?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            helperRef.value?.close()
            executorRef.value?.shutdown()
        }
    }

    Box(modifier = modifier) {
        // Camera preview surface
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                val context = previewView.context
                val executor = executorRef.value ?: Executors.newSingleThreadExecutor()
                    .also { executorRef.value = it }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    // Initialize PoseLandmarkerHelper once
                    if (helperRef.value == null || !helperRef.value!!.isOpen()) {
                        helperRef.value = PoseLandmarkerHelper(
                            context = context,
                            onResults = { result, image ->
                                // Update overlay
                                overlayRef.value?.post {
                                    overlayRef.value?.setResults(
                                        result,
                                        image.width,
                                        image.height
                                    )
                                }
                                // Process reps
                                if (result.landmarks().isNotEmpty()) {
                                    val landmarks = result.landmarks()[0]
                                    val repResult = repCounterRef.value.process(landmarks)
                                    if (repResult.repDetected) {
                                        onRepDetected(repResult.formScore)
                                    }
                                }
                            },
                            onError = { error, _ ->
                                android.util.Log.e("CameraPreview", "PoseLandmarker error: $error")
                            }
                        )
                    }

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(executor) { imageProxy ->
                                helperRef.value?.detectLiveStream(imageProxy, isFrontCamera)
                                imageProxy.close()
                            }
                        }

                    val cameraSelector = if (isFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            previewView.context as LifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalyzer
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("CameraPreview", "Camera bind failed: ${e.message}")
                    }
                }, ContextCompat.getMainExecutor(previewView.context))
            }
        )

        // Landmark overlay on top of camera preview
        AndroidView(
            factory = { ctx ->
                LandmarkOverlayView(ctx).also { overlayRef.value = it }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
