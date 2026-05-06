package com.activespark.gen7.ui.screens.battle

import com.activespark.gen7.data.models.ExerciseType
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Counts exercise reps from MediaPipe pose landmarks.
 *
 * Angle-based detection is used for most exercises — far more reliable than
 * raw Y-position because it adapts to different body sizes and camera distances.
 *
 * Each exercise maps to one of three processor types:
 *  - Standard:        getMeasurement() → updateStateMachine() (push-up, squat, etc.)
 *  - AlternateLeg:    processAlternateLeg() (high knee, mountain climber)
 *  - PlankHold:       processPlank() — returns hold seconds instead of reps
 *
 * MediaPipe landmark indices used:
 *   0=nose  11=L-shoulder  12=R-shoulder  13=L-elbow  14=R-elbow
 *   15=L-wrist  16=R-wrist  23=L-hip  24=R-hip  25=L-knee  26=R-knee
 *   27=L-ankle  28=R-ankle
 */
class RepCounter(private val exerciseType: ExerciseType) {

    enum class RepState { IDLE, DOWN, UP }

    private var state = RepState.IDLE
    private var repCount = 0
    private var lastFormScore = 1.0f
    private var lastRepTime = 0L          // cooldown: prevent rapid false reps

    // Alternate-leg tracking (HIGH_KNEE, MOUNTAIN_CLIMBER)
    private var lastKneeSide = -1   // 0 = left, 1 = right

    // Plank hold tracking
    private var plankStartTime = 0L
    private var isInPlankPosition = false

    companion object {
        // Minimum milliseconds between two consecutive reps
        private const val REP_COOLDOWN_MS = 800L
        // Minimum visibility score for key landmarks — below this, skip frame
        private const val MIN_VISIBILITY = 0.5f
    }

    data class RepResult(
        val repCount: Int,
        val formScore: Float,
        val repDetected: Boolean,
        val holdSeconds: Int = 0    // PLANK only
    )

    fun process(landmarks: List<NormalizedLandmark>): RepResult {
        if (landmarks.size < 29) return RepResult(repCount, lastFormScore, false)

        // Skip frame if key torso landmarks aren't visible enough
        // (prevents counting reps when camera sees a wall / wrong angle)
        val hipVis      = (landmarks[23].visibility().orElse(0f) + landmarks[24].visibility().orElse(0f)) / 2f
        val shoulderVis = (landmarks[11].visibility().orElse(0f) + landmarks[12].visibility().orElse(0f)) / 2f
        if (hipVis < MIN_VISIBILITY || shoulderVis < MIN_VISIBILITY) {
            return RepResult(repCount, lastFormScore, false)
        }

        val formScore = calculateFormScore(landmarks).also { lastFormScore = it }
        return when (exerciseType) {
            ExerciseType.PLANK           -> processPlank(landmarks, formScore)
            ExerciseType.HIGH_KNEE       -> processAlternateLeg(landmarks, formScore,
                                              liftThreshold = 0.06f, extendThreshold = 0.02f)
            ExerciseType.MOUNTAIN_CLIMBER -> processAlternateLeg(landmarks, formScore,
                                              liftThreshold = 0.10f, extendThreshold = 0.03f)
            else                         -> processStandard(landmarks, formScore)
        }
    }

    // ── Standard rep detection ────────────────────────────────────────────────

    private fun processStandard(landmarks: List<NormalizedLandmark>, formScore: Float): RepResult {
        val measurement = getMeasurement(landmarks)
        val rawRepDetected = updateStateMachine(measurement)
        val now = System.currentTimeMillis()
        val repDetected = rawRepDetected && (now - lastRepTime >= REP_COOLDOWN_MS)
        if (repDetected) { repCount++; lastRepTime = now }
        return RepResult(repCount, formScore, repDetected)
    }

    private fun getMeasurement(landmarks: List<NormalizedLandmark>): Float = when (exerciseType) {
        ExerciseType.PUSH_UP -> {
            // Elbow angle — arms parallel to camera (side-on) works best
            val l = calcAngle(landmarks[11], landmarks[13], landmarks[15])
            val r = calcAngle(landmarks[12], landmarks[14], landmarks[16])
            (l + r) / 2f
        }
        ExerciseType.SQUAT -> {
            // Average knee angle (hip-knee-ankle)
            val l = calcAngle(landmarks[23], landmarks[25], landmarks[27])
            val r = calcAngle(landmarks[24], landmarks[26], landmarks[28])
            (l + r) / 2f
        }
        ExerciseType.JUMPING_JACK, ExerciseType.STAR_JUMP -> {
            // Use knee angle (like squat) — jump down and up counts as a rep
            val l = calcAngle(landmarks[23], landmarks[25], landmarks[27])
            val r = calcAngle(landmarks[24], landmarks[26], landmarks[28])
            (l + r) / 2f
        }
        ExerciseType.SIT_UP -> {
            // Angle at HIP (shoulder→hip→knee): small = sitting up, large = lying flat
            calcAngle(landmarks[11], landmarks[23], landmarks[25])
        }
        ExerciseType.BURPEE -> {
            // Knee angle — squat down then stand up. Works front-facing.
            val l = calcAngle(landmarks[23], landmarks[25], landmarks[27])
            val r = calcAngle(landmarks[24], landmarks[26], landmarks[28])
            (l + r) / 2f
        }
        ExerciseType.LUNGE -> {
            // Front knee angle (pick the more bent knee)
            val l = calcAngle(landmarks[23], landmarks[25], landmarks[27])
            val r = calcAngle(landmarks[24], landmarks[26], landmarks[28])
            minOf(l, r)
        }
        ExerciseType.DANCE_MOVE -> {
            // Hip sway: horizontal spread — counts a cycle of side-to-side movement
            abs(landmarks[23].x() - landmarks[24].x())
        }
        else -> (landmarks[23].y() + landmarks[24].y()) / 2f
    }

    private fun updateStateMachine(m: Float): Boolean {
        return when (exerciseType) {

            // ── Angle exercises: small angle = DOWN, large angle = UP ──────────
            // upAngle lowered 155→150 so a partial extension still counts
            ExerciseType.PUSH_UP -> angleStateMachine(m, downAngle = 100f, upAngle = 150f)
            ExerciseType.SQUAT   -> angleStateMachine(m, downAngle = 110f, upAngle = 150f)
            ExerciseType.LUNGE   -> angleStateMachine(m, downAngle = 110f, upAngle = 150f)

            // ── Sit-up: angle at hip decreases when torso rises ───────────────
            ExerciseType.SIT_UP  -> reverseAngleStateMachine(m, sitUpAngle = 80f, lyingAngle = 140f)

            // ── Jumping jack / Star jump: bend knees and jump = 1 rep ───────
            ExerciseType.JUMPING_JACK,
            ExerciseType.STAR_JUMP -> angleStateMachine(m, downAngle = 120f, upAngle = 150f)

            // ── Burpee: now uses knee angle same as squat ─────────────────────
            ExerciseType.BURPEE  -> angleStateMachine(m, downAngle = 110f, upAngle = 150f)

            // ── Dance: small hip-spread change counts a beat ──────────────────
            ExerciseType.DANCE_MOVE -> spreadStateMachine(m, outThresh = 0.15f, inThresh = 0.05f)

            else -> false
        }
    }

    // State machine helpers — each returns true on rep completion

    private fun angleStateMachine(angle: Float, downAngle: Float, upAngle: Float): Boolean {
        return when (state) {
            RepState.IDLE -> { if (angle < downAngle) state = RepState.DOWN; false }
            RepState.DOWN -> if (angle > upAngle) { state = RepState.UP; true } else false
            RepState.UP   -> { state = RepState.IDLE; false }
        }
    }

    private fun reverseAngleStateMachine(angle: Float, sitUpAngle: Float, lyingAngle: Float): Boolean {
        return when (state) {
            RepState.IDLE -> { if (angle < sitUpAngle) state = RepState.DOWN; false }
            RepState.DOWN -> if (angle > lyingAngle) { state = RepState.UP; true } else false
            RepState.UP   -> { state = RepState.IDLE; false }
        }
    }

    private fun spreadStateMachine(value: Float, outThresh: Float, inThresh: Float): Boolean {
        return when (state) {
            RepState.IDLE -> { if (value > outThresh) state = RepState.DOWN; false }
            RepState.DOWN -> if (value < inThresh) { state = RepState.UP; true } else false
            RepState.UP   -> { state = RepState.IDLE; false }
        }
    }

    private fun yPositionStateMachine(y: Float, downY: Float, upY: Float): Boolean {
        return when (state) {
            RepState.IDLE -> { if (y > downY) state = RepState.DOWN; false }
            RepState.DOWN -> if (y < upY) { state = RepState.UP; true } else false
            RepState.UP   -> { state = RepState.IDLE; false }
        }
    }

    // ── Alternate-leg detection (HIGH_KNEE, MOUNTAIN_CLIMBER) ────────────────

    private fun processAlternateLeg(
        landmarks: List<NormalizedLandmark>,
        formScore: Float,
        liftThreshold: Float,
        extendThreshold: Float
    ): RepResult {
        val leftKneeAboveHip  = landmarks[23].y() - landmarks[25].y() > liftThreshold
        val rightKneeAboveHip = landmarks[24].y() - landmarks[26].y() > liftThreshold

        var repDetected = false
        when (state) {
            RepState.IDLE -> {
                when {
                    leftKneeAboveHip  && lastKneeSide != 0 -> { state = RepState.DOWN; lastKneeSide = 0 }
                    rightKneeAboveHip && lastKneeSide != 1 -> { state = RepState.DOWN; lastKneeSide = 1 }
                }
            }
            RepState.DOWN -> {
                val lowered = if (lastKneeSide == 0)
                    landmarks[23].y() - landmarks[25].y() < extendThreshold
                else
                    landmarks[24].y() - landmarks[26].y() < extendThreshold
                val now = System.currentTimeMillis()
                if (lowered && now - lastRepTime >= REP_COOLDOWN_MS) {
                    state = RepState.IDLE; repCount++; lastRepTime = now; repDetected = true
                } else if (lowered) { state = RepState.IDLE }
            }
            RepState.UP -> state = RepState.IDLE
        }
        return RepResult(repCount, formScore, repDetected)
    }

    // ── Plank hold ────────────────────────────────────────────────────────────

    private fun processPlank(landmarks: List<NormalizedLandmark>, formScore: Float): RepResult {
        // Body angle (left shoulder → hip → ankle) should be straight (> 155°) for a valid plank
        val bodyAngle = calcAngle(landmarks[11], landmarks[23], landmarks[27])
        val inPlank = bodyAngle > 155f && formScore > 0.45f

        when {
            inPlank && !isInPlankPosition -> {
                plankStartTime = System.currentTimeMillis()
                isInPlankPosition = true
            }
            !inPlank -> isInPlankPosition = false
        }

        val holdSeconds = if (isInPlankPosition)
            ((System.currentTimeMillis() - plankStartTime) / 1000).toInt()
        else 0

        // For plank, repCount carries the longest hold reached this session
        if (holdSeconds > repCount) repCount = holdSeconds

        return RepResult(repCount, formScore, holdSeconds > 0 && holdSeconds % 5 == 0, holdSeconds)
    }

    // ── Form scoring ──────────────────────────────────────────────────────────

    private fun calculateFormScore(landmarks: List<NormalizedLandmark>): Float {
        return try {
            when (exerciseType) {
                ExerciseType.PUSH_UP -> {
                    // Body should be a straight line: shoulder-hip-ankle alignment
                    val bodyAngle = calcAngle(landmarks[11], landmarks[23], landmarks[27])
                    ((bodyAngle - 150f) / 30f).coerceIn(0.3f, 1.0f)
                }
                ExerciseType.SQUAT -> {
                    // Knees track over ankles (knee X ≈ ankle X on each side)
                    val alignL = 1f - (abs(landmarks[25].x() - landmarks[27].x()) * 5f).coerceIn(0f, 0.7f)
                    val alignR = 1f - (abs(landmarks[26].x() - landmarks[28].x()) * 5f).coerceIn(0f, 0.7f)
                    ((alignL + alignR) / 2f).coerceIn(0.3f, 1.0f)
                }
                ExerciseType.JUMPING_JACK, ExerciseType.STAR_JUMP -> {
                    // Arms should be symmetric: equal distance from shoulders
                    val leftDrop  = abs(landmarks[15].y() - landmarks[11].y())
                    val rightDrop = abs(landmarks[16].y() - landmarks[12].y())
                    (1f - abs(leftDrop - rightDrop).coerceIn(0f, 0.3f) / 0.3f).coerceIn(0.3f, 1.0f)
                }
                ExerciseType.SIT_UP -> {
                    // Shoulder Y should be clearly above hip Y when sitting up
                    val rise = (landmarks[24].y() - landmarks[12].y()).coerceIn(0f, 0.3f) / 0.3f
                    rise.coerceIn(0.3f, 1.0f)
                }
                ExerciseType.PLANK -> {
                    // Flat body: shoulder-hip-ankle angle close to 180°
                    val bodyAngle = calcAngle(landmarks[11], landmarks[23], landmarks[27])
                    ((bodyAngle - 150f) / 30f).coerceIn(0.3f, 1.0f)
                }
                ExerciseType.HIGH_KNEE -> {
                    // Upright posture: torso angle (shoulder over hip) near vertical
                    val torsoAngle = calcAngle(landmarks[12], landmarks[24], landmarks[26])
                    ((torsoAngle - 150f) / 30f).coerceIn(0.3f, 1.0f)
                }
                ExerciseType.LUNGE -> {
                    // Front knee should track over front ankle
                    val alignL = 1f - abs(landmarks[25].x() - landmarks[27].x()).coerceIn(0f, 0.2f) / 0.2f
                    val alignR = 1f - abs(landmarks[26].x() - landmarks[28].x()).coerceIn(0f, 0.2f) / 0.2f
                    minOf(alignL, alignR).coerceIn(0.3f, 1.0f)
                }
                ExerciseType.MOUNTAIN_CLIMBER -> {
                    // Plank position maintained throughout
                    val bodyAngle = calcAngle(landmarks[11], landmarks[23], landmarks[27])
                    ((bodyAngle - 145f) / 35f).coerceIn(0.3f, 1.0f)
                }
                ExerciseType.BURPEE -> {
                    // Landmark visibility as proxy (burpee is complex)
                    val vis = (landmarks[11].visibility().orElse(0f) +
                               landmarks[23].visibility().orElse(0f)) / 2f
                    vis.coerceIn(0.3f, 1.0f)
                }
                ExerciseType.DANCE_MOVE -> 0.8f   // fixed — dance scoring is subjective
                else -> 0.8f
            }
        } catch (_: Exception) { 0.7f }
    }

    // ── Geometry helpers ──────────────────────────────────────────────────────

    /** Angle at vertex [b] formed by rays b→a and b→c, in degrees (0–180). */
    private fun calcAngle(
        a: NormalizedLandmark,
        b: NormalizedLandmark,
        c: NormalizedLandmark
    ): Float {
        val abX = a.x() - b.x(); val abY = a.y() - b.y()
        val cbX = c.x() - b.x(); val cbY = c.y() - b.y()
        val dot = abX * cbX + abY * cbY
        val mag = sqrt((abX * abX + abY * abY).toDouble()) *
                  sqrt((cbX * cbX + cbY * cbY).toDouble())
        if (mag == 0.0) return 180f
        return Math.toDegrees(acos((dot / mag).coerceIn(-1.0, 1.0))).toFloat()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun reset() {
        state = RepState.IDLE
        repCount = 0
        lastFormScore = 1.0f
        lastKneeSide = -1
        plankStartTime = 0L
        isInPlankPosition = false
    }

    fun getRepCount(): Int = repCount
}
