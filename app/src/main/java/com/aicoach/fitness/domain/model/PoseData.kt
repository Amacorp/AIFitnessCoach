package com.aicoach.fitness.domain.model

data class PoseData(
    val landmarks: List<BodyLandmark>,
    val timestamp: Long = System.currentTimeMillis()
)

data class BodyLandmark(
    val name: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val visibility: Float,
    val presence: Float
)

// MediaPipe Pose Landmarker indices
object LandmarkIndices {
    const val NOSE = 0
    const val LEFT_EYE_INNER = 1
    const val LEFT_EYE = 2
    const val LEFT_EYE_OUTER = 3
    const val RIGHT_EYE_INNER = 4
    const val RIGHT_EYE = 5
    const val RIGHT_EYE_OUTER = 6
    const val LEFT_EAR = 7
    const val RIGHT_EAR = 8
    const val MOUTH_LEFT = 9
    const val MOUTH_RIGHT = 10
    const val LEFT_SHOULDER = 11
    const val RIGHT_SHOULDER = 12
    const val LEFT_ELBOW = 13
    const val RIGHT_ELBOW = 14
    const val LEFT_WRIST = 15
    const val RIGHT_WRIST = 16
    const val LEFT_PINKY = 17
    const val RIGHT_PINKY = 18
    const val LEFT_INDEX = 19
    const val RIGHT_INDEX = 20
    const val LEFT_THUMB = 21
    const val RIGHT_THUMB = 22
    const val LEFT_HIP = 23
    const val RIGHT_HIP = 24
    const val LEFT_KNEE = 25
    const val RIGHT_KNEE = 26
    const val LEFT_ANKLE = 27
    const val RIGHT_ANKLE = 28
    const val LEFT_HEEL = 29
    const val RIGHT_HEEL = 30
    const val LEFT_FOOT_INDEX = 31
    const val RIGHT_FOOT_INDEX = 32

    val LANDMARK_NAMES = mapOf(
        NOSE to "nose",
        LEFT_EYE_INNER to "left_eye_inner",
        LEFT_EYE to "left_eye",
        LEFT_EYE_OUTER to "left_eye_outer",
        RIGHT_EYE_INNER to "right_eye_inner",
        RIGHT_EYE to "right_eye",
        RIGHT_EYE_OUTER to "right_eye_outer",
        LEFT_EAR to "left_ear",
        RIGHT_EAR to "right_ear",
        MOUTH_LEFT to "mouth_left",
        MOUTH_RIGHT to "mouth_right",
        LEFT_SHOULDER to "left_shoulder",
        RIGHT_SHOULDER to "right_shoulder",
        LEFT_ELBOW to "left_elbow",
        RIGHT_ELBOW to "right_elbow",
        LEFT_WRIST to "left_wrist",
        RIGHT_WRIST to "right_wrist",
        LEFT_PINKY to "left_pinky",
        RIGHT_PINKY to "right_pinky",
        LEFT_INDEX to "left_index",
        RIGHT_INDEX to "right_index",
        LEFT_THUMB to "left_thumb",
        RIGHT_THUMB to "right_thumb",
        LEFT_HIP to "left_hip",
        RIGHT_HIP to "right_hip",
        LEFT_KNEE to "left_knee",
        RIGHT_KNEE to "right_knee",
        LEFT_ANKLE to "left_ankle",
        RIGHT_ANKLE to "right_ankle",
        LEFT_HEEL to "left_heel",
        RIGHT_HEEL to "right_heel",
        LEFT_FOOT_INDEX to "left_foot_index",
        RIGHT_FOOT_INDEX to "right_foot_index"
    )
}