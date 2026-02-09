package com.aicoach.fitness.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.aicoach.fitness.domain.model.BodyLandmark
import com.aicoach.fitness.domain.model.LandmarkIndices
import com.aicoach.fitness.domain.model.PoseData
import com.google.gson.Gson
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.pow

@Singleton
class PoseAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "PoseAnalyzer"
        private const val MODEL_PATH = "pose_landmarker_lite.task"
        private const val MIN_POSE_DETECTION_CONFIDENCE = 0.5f
        private const val MIN_POSE_PRESENCE_CONFIDENCE = 0.5f
        private const val MIN_TRACKING_CONFIDENCE = 0.5f
        private const val ANALYSIS_INTERVAL_MS = 2000L // 2 seconds
    }

    sealed class AnalysisState {
        data object Idle : AnalysisState()
        data object Detecting : AnalysisState()
        data class Detected(val poseData: PoseData) : AnalysisState()
        data class Error(val message: String) : AnalysisState()
    }

    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()

    private var poseLandmarker: PoseLandmarker? = null
    private var executor: ExecutorService? = null
    private var lastAnalysisTime = 0L

    private var onPoseDetected: ((PoseData) -> Unit)? = null

    init {
        initializePoseLandmarker()
    }

    private fun initializePoseLandmarker() {
        try {
            val baseOptions = BaseOptions.builder()
                .setDelegate(Delegate.CPU)
                .setModelAssetPath(MODEL_PATH)
                .build()

            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setMinPoseDetectionConfidence(MIN_POSE_DETECTION_CONFIDENCE)
                .setMinPosePresenceConfidence(MIN_POSE_PRESENCE_CONFIDENCE)
                .setMinTrackingConfidence(MIN_TRACKING_CONFIDENCE)
                .setResultListener { result: PoseLandmarkerResult, inputImage ->
                    handlePoseResult(result)
                }
                .setErrorListener { error: RuntimeException ->
                    _analysisState.value = AnalysisState.Error(error.message ?: "Unknown error")
                }
                .build()

            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            executor = Executors.newSingleThreadExecutor()
        } catch (e: Exception) {
            _analysisState.value = AnalysisState.Error("Failed to initialize: ${e.message}")
        }
    }

    fun setOnPoseDetectedListener(listener: (PoseData) -> Unit) {
        onPoseDetected = listener
    }

    fun analyzeImage(imageProxy: ImageProxy) {
        val currentTime = SystemClock.elapsedRealtime()

        // Throttle analysis to every 2 seconds
        if (currentTime - lastAnalysisTime < ANALYSIS_INTERVAL_MS) {
            imageProxy.close()
            return
        }

        val bitmap = imageProxy.toBitmap()
        val mpImage = BitmapImageBuilder(bitmap).build()

        val frameTime = SystemClock.elapsedRealtime()

        poseLandmarker?.detectAsync(mpImage, frameTime)
        imageProxy.close()
    }

    private fun handlePoseResult(result: PoseLandmarkerResult) {
        if (result.landmarks().isEmpty()) {
            _analysisState.value = AnalysisState.Idle
            return
        }

        val landmarks = result.landmarks()[0]
        val bodyLandmarks = mutableListOf<BodyLandmark>()

        landmarks.forEachIndexed { index, landmark ->
            val name = LandmarkIndices.LANDMARK_NAMES[index] ?: "landmark_$index"
            bodyLandmarks.add(
                BodyLandmark(
                    name = name,
                    x = landmark.x(),
                    y = landmark.y(),
                    z = landmark.z(),
                    visibility = landmark.visibility().orElse(0f),
                    presence = landmark.presence().orElse(0f)
                )
            )
        }

        val poseData = PoseData(
            landmarks = bodyLandmarks,
            timestamp = SystemClock.elapsedRealtime()
        )

        lastAnalysisTime = SystemClock.elapsedRealtime()
        _analysisState.value = AnalysisState.Detected(poseData)
        onPoseDetected?.invoke(poseData)
    }

    fun convertPoseToJson(poseData: PoseData): String {
        val landmarkMap = poseData.landmarks.associate {
            it.name to listOf(
                roundToDecimals(it.x, 4),
                roundToDecimals(it.y, 4),
                roundToDecimals(it.z, 4)
            )
        }
        return gson.toJson(landmarkMap)
    }

    private fun roundToDecimals(value: Float, decimals: Int): Float {
        val factor = 10.0.pow(decimals.toDouble()).toFloat()
        return kotlin.math.round(value * factor) / factor
    }

    // Calculate angle between three points (for form analysis)
    fun calculateAngle(
        firstPoint: BodyLandmark,
        midPoint: BodyLandmark,
        lastPoint: BodyLandmark
    ): Float {
        val radians = atan2(
            lastPoint.y - midPoint.y,
            lastPoint.x - midPoint.x
        ) - atan2(
            firstPoint.y - midPoint.y,
            firstPoint.x - midPoint.x
        )
        var degrees = Math.toDegrees(radians.toDouble()).toFloat()
        if (abs(degrees) > 180) {
            degrees = if (degrees > 0) 360 - degrees else 360 + degrees
        }
        return abs(degrees)
    }

    // Calculate distance between two points
    fun calculateDistance(p1: BodyLandmark, p2: BodyLandmark): Float {
        return sqrt(
            (p2.x - p1.x) * (p2.x - p1.x) +
                    (p2.y - p1.y) * (p2.y - p1.y)
        )
    }

    // Get specific landmark by name
    fun getLandmark(poseData: PoseData, name: String): BodyLandmark? {
        return poseData.landmarks.find { it.name == name }
    }

    // Get landmark by index
    fun getLandmarkByIndex(poseData: PoseData, index: Int): BodyLandmark? {
        val name = LandmarkIndices.LANDMARK_NAMES[index] ?: return null
        return getLandmark(poseData, name)
    }

    // Check if pose is valid (has minimum required landmarks)
    fun isValidPose(poseData: PoseData): Boolean {
        val requiredLandmarks = listOf(
            "left_shoulder", "right_shoulder",
            "left_hip", "right_hip",
            "left_knee", "right_knee"
        )
        return requiredLandmarks.all { name ->
            poseData.landmarks.any { it.name == name && it.visibility > 0.5f }
        }
    }

    // Get body center point
    fun getBodyCenter(poseData: PoseData): BodyLandmark? {
        val leftHip = getLandmark(poseData, "left_hip") ?: return null
        val rightHip = getLandmark(poseData, "right_hip") ?: return null
        val leftShoulder = getLandmark(poseData, "left_shoulder") ?: return null
        val rightShoulder = getLandmark(poseData, "right_shoulder") ?: return null

        return BodyLandmark(
            name = "body_center",
            x = (leftHip.x + rightHip.x + leftShoulder.x + rightShoulder.x) / 4,
            y = (leftHip.y + rightHip.y + leftShoulder.y + rightShoulder.y) / 4,
            z = (leftHip.z + rightHip.z + leftShoulder.z + rightShoulder.z) / 4,
            visibility = 1.0f,
            presence = 1.0f
        )
    }

    fun close() {
        poseLandmarker?.close()
        executor?.shutdown()
        executor = null
    }

    // Extension function to convert ImageProxy to Bitmap
    private fun ImageProxy.toBitmap(): Bitmap {
        val buffer = planes[0].buffer
        val rotationDegrees = imageInfo.rotationDegrees

        // Create bitmap from YUV or RGB depending on format
        val bitmap = if (format == android.graphics.ImageFormat.YUV_420_888) {
            yuvToRgb(this)
        } else {
            // For RGB formats
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * width

            val bitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            bitmap
        }

        // Handle rotation
        return if (rotationDegrees != 0) {
            val matrix = Matrix().apply {
                postRotate(rotationDegrees.toFloat())
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    private fun yuvToRgb(image: ImageProxy): Bitmap {
        // Simplified YUV to RGB conversion
        // In production, use RenderScript or GPU for better performance
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = android.graphics.YuvImage(
            nv21,
            android.graphics.ImageFormat.NV21,
            image.width,
            image.height,
            null
        )

        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            android.graphics.Rect(0, 0, image.width, image.height),
            100,
            out
        )
        val imageBytes = out.toByteArray()
        return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}