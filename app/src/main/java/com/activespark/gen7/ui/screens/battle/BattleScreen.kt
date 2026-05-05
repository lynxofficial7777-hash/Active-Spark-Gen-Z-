package com.activespark.gen7.ui.screens.battle

import android.Manifest
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.activespark.gen7.data.models.ExerciseType
import com.activespark.gen7.navigation.Screen
import com.activespark.gen7.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BattleScreen(
    navController: NavController,
    matchId: String,
    viewModel: BattleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(matchId) { viewModel.initBattle(matchId) }

    // Navigate when battle ends
    LaunchedEffect(uiState.isBattleOver) {
        if (uiState.isBattleOver) {
            navController.navigate(Screen.Results.createRoute(matchId)) {
                popUpTo(Screen.Battle.route) { inclusive = true }
            }
        }
    }

    // Request camera permission on first launch
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BackgroundVariant, Background)
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ─── Top: Timer ───────────────────────────────────────────────
            BattleTimerBar(
                secondsRemaining = uiState.secondsRemaining,
                totalSeconds = uiState.totalSeconds
            )

            // ─── Scores Row ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerScorePanel(
                    name = "YOU",
                    reps = uiState.myReps,
                    score = uiState.myScore,
                    color = NeonCyan,
                    isLeading = uiState.myReps >= uiState.opponentReps
                )

                Text(
                    "VS",
                    style = ActiveSparkTypography.headlineMedium.copy(
                        color = TextTertiary,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                PlayerScorePanel(
                    name = uiState.opponentName,
                    reps = uiState.opponentReps,
                    score = uiState.opponentScore,
                    color = NeonPink,
                    isLeading = uiState.opponentReps > uiState.myReps
                )
            }

            // ─── Camera / Permission Area ─────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                when {
                    cameraPermissionState.status.isGranted -> {
                        // Real camera preview with MediaPipe pose detection
                        CameraPreviewView(
                            exerciseType = uiState.exerciseType,
                            onRepDetected = { formScore ->
                                viewModel.onRepDetected(formScore)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    cameraPermissionState.status.shouldShowRationale -> {
                        // Show rationale
                        CameraPermissionRationale(
                            onRequest = { cameraPermissionState.launchPermissionRequest() }
                        )
                    }
                    else -> {
                        // Permission denied permanently
                        CameraPermissionDenied()
                    }
                }

                // Rep counter overlay (always on top of camera)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .background(Background.copy(0.8f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${uiState.myReps}",
                            style = ActiveSparkTypography.displayLarge.copy(
                                color = NeonCyan,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        Text(
                            "REPS",
                            style = ActiveSparkTypography.labelLarge.copy(color = TextSecondary)
                        )
                    }
                }
            }

            // ─── Form Accuracy ────────────────────────────────────────────
            FormAccuracyBar(accuracy = uiState.formAccuracy)

            Spacer(Modifier.height(16.dp))
        }

        // ─── Camera Setup Overlay (side-camera exercises only) ───────────
        if (uiState.showCameraSetup) {
            CameraSetupOverlay(
                exerciseType = uiState.exerciseType,
                onReady = { viewModel.onCameraSetupDismissed() }
            )
        }

        // ─── Countdown Overlay ────────────────────────────────────────────
        if (uiState.isCountdown && !uiState.showCameraSetup) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background.copy(0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${uiState.countdownValue}",
                    style = ActiveSparkTypography.displayLarge.copy(
                        fontSize = 128.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }
        }
    }
}

// ─── Camera Setup Overlay ─────────────────────────────────────────────────────

@Composable
internal fun CameraSetupOverlay(exerciseType: ExerciseType, onReady: () -> Unit) {
    val isPushUp = exerciseType == ExerciseType.PUSH_UP

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background.copy(alpha = 0.96f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            // Exercise name badge
            Box(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(listOf(NeonCyan.copy(0.2f), NeonPink.copy(0.2f))),
                        RoundedCornerShape(50)
                    )
                    .border(1.dp, NeonCyan, RoundedCornerShape(50))
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = exerciseType.displayName.uppercase(),
                    style = ActiveSparkTypography.labelLarge.copy(
                        color = NeonCyan,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }

            Text(
                text = "📱 Phone Placement",
                style = ActiveSparkTypography.headlineMedium.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            // Diagram box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(SurfaceVariant, RoundedCornerShape(20.dp))
                    .border(1.dp, NeonCyan.copy(0.4f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // ASCII-style side view diagram
                    Text(
                        text = if (isPushUp) "📱  ←  🏃 (side view)" else "📱  ←  🧍 (side view)",
                        style = ActiveSparkTypography.bodyLarge.copy(color = NeonCyan),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (isPushUp)
                            "Place phone on the floor\n~1 metre to your side"
                        else
                            "Place phone at waist height\n~1 metre to your side",
                        style = ActiveSparkTypography.bodyMedium.copy(color = TextSecondary),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Why text
            Text(
                text = if (isPushUp)
                    "Push-ups need a side view so the camera can measure your elbow angle accurately."
                else
                    "Plank needs a side view so the camera can check your body is straight.",
                style = ActiveSparkTypography.bodySmall.copy(color = TextTertiary),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            // Ready button
            Button(
                onClick = onReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text(
                    "I'M READY  ✓",
                    style = ActiveSparkTypography.titleMedium.copy(
                        color = Background,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }
        }
    }
}

// ─── Permission UI ────────────────────────────────────────────────────────────

@Composable
private fun CameraPermissionRationale(onRequest: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        Text("📷", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Camera Required",
            style = ActiveSparkTypography.titleLarge.copy(color = TextPrimary),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Active Spark needs camera access to track your exercise form using MediaPipe pose detection.",
            style = ActiveSparkTypography.bodyMedium.copy(color = TextSecondary),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
        ) {
            Text(
                "GRANT CAMERA ACCESS",
                style = ActiveSparkTypography.labelLarge.copy(color = Background)
            )
        }
    }
}

@Composable
private fun CameraPermissionDenied() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        Text("🚫", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Camera Access Denied",
            style = ActiveSparkTypography.titleLarge.copy(color = NeonPink),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Please enable camera permission in your device Settings to use pose tracking.",
            style = ActiveSparkTypography.bodyMedium.copy(color = TextSecondary),
            textAlign = TextAlign.Center
        )
    }
}

// ─── Timer Bar ────────────────────────────────────────────────────────────────

@Composable
private fun BattleTimerBar(secondsRemaining: Int, totalSeconds: Int) {
    val progress = secondsRemaining.toFloat() / totalSeconds.toFloat()
    val timerColor = when {
        progress > 0.5f -> NeonGreen
        progress > 0.25f -> NeonYellow
        else -> NeonPink
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$secondsRemaining",
                style = ActiveSparkTypography.displayMedium.copy(
                    color = timerColor,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = timerColor,
            trackColor = DividerColor
        )
    }
}

// ─── Player Score Panel ───────────────────────────────────────────────────────

@Composable
private fun PlayerScorePanel(
    name: String,
    reps: Int,
    score: Float,
    color: androidx.compose.ui.graphics.Color,
    isLeading: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(140.dp)
    ) {
        Text(
            name,
            style = ActiveSparkTypography.labelLarge.copy(
                color = if (isLeading) color else TextTertiary
            )
        )
        Text(
            "$reps",
            style = ActiveSparkTypography.displaySmall.copy(
                color = if (isLeading) color else TextSecondary,
                fontWeight = FontWeight.ExtraBold
            )
        )
        Text("reps", style = ActiveSparkTypography.bodySmall.copy(color = TextTertiary))
        if (isLeading) {
            Text("👑 LEADING", style = ActiveSparkTypography.labelSmall.copy(color = color))
        }
    }
}

// ─── Form Accuracy Bar ────────────────────────────────────────────────────────

@Composable
private fun FormAccuracyBar(accuracy: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Form Accuracy",
                style = ActiveSparkTypography.labelSmall.copy(color = TextTertiary)
            )
            Text(
                "${(accuracy * 100).toInt()}%",
                style = ActiveSparkTypography.labelSmall.copy(color = NeonGreen)
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { accuracy },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = NeonGreen,
            trackColor = HealthBarBackground
        )
    }
}
