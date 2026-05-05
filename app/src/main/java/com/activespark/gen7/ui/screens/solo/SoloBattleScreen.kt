package com.activespark.gen7.ui.screens.solo

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.activespark.gen7.navigation.Screen
import com.activespark.gen7.ui.screens.battle.CameraPreviewView
import com.activespark.gen7.ui.screens.battle.CameraSetupOverlay
import com.activespark.gen7.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SoloBattleScreen(
    navController: NavController,
    challengeId: String,
    viewModel: SoloBattleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(challengeId) { viewModel.initSoloBattle(challengeId) }
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF051A0F), Background)))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top bar: mode label + timer ────────────────────────────────
            SoloTimerBar(
                secondsRemaining = uiState.secondsRemaining,
                totalSeconds     = uiState.totalSeconds,
                challengeName    = uiState.challengeName
            )

            // ── Target reps progress bar ───────────────────────────────────
            if (uiState.targetReps > 0) {
                val repProgress = (uiState.myReps.toFloat() / uiState.targetReps).coerceIn(0f, 1f)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Progress",
                            style = ActiveSparkTypography.labelSmall.copy(color = TextTertiary)
                        )
                        Text(
                            "${uiState.myReps} / ${uiState.targetReps} reps",
                            style = ActiveSparkTypography.labelSmall.copy(color = NeonGreen)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { repProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = NeonGreen,
                        trackColor = DividerColor
                    )
                }
            }

            // ── Camera area ────────────────────────────────────────────────
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
                        CameraPreviewView(
                            exerciseType = uiState.exerciseType,
                            onRepDetected = { formScore -> viewModel.onRepDetected(formScore) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    cameraPermissionState.status.shouldShowRationale -> {
                        SoloCameraRationale { cameraPermissionState.launchPermissionRequest() }
                    }
                    else -> SoloCameraDenied()
                }

                // ── Big pulsing rep counter overlay ────────────────────────
                AnimatedRepCounter(
                    reps = uiState.myReps,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(20.dp)
                )
            }

            // ── Form accuracy bar ──────────────────────────────────────────
            SoloFormBar(accuracy = uiState.formAccuracy)

            Spacer(Modifier.height(16.dp))
        }

        // ── Camera placement overlay ───────────────────────────────────────
        if (uiState.showCameraSetup) {
            CameraSetupOverlay(
                exerciseType = uiState.exerciseType,
                onReady      = { viewModel.onCameraSetupDismissed() }
            )
        }

        // ── 3-2-1 Countdown overlay ────────────────────────────────────────
        if (uiState.isCountdown && !uiState.showCameraSetup) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background.copy(0.88f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏃 SOLO PRACTICE", style = ActiveSparkTypography.titleMedium.copy(color = NeonGreen))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "${uiState.countdownValue}",
                        style = ActiveSparkTypography.displayLarge.copy(
                            fontSize = 128.sp,
                            color = NeonGreen,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Text("GET READY!", style = ActiveSparkTypography.titleSmall.copy(color = TextSecondary))
                }
            }
        }

        // ── Results overlay (slides up when battle ends) ───────────────────
        AnimatedVisibility(
            visible = uiState.isBattleOver,
            enter   = slideInVertically(
                initialOffsetY = { it },
                animationSpec  = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            ) + fadeIn()
        ) {
            SoloResultsOverlay(
                uiState      = uiState,
                onHome       = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onTryAgain   = {
                    navController.navigate(Screen.SoloBattle.createRoute(uiState.challengeId)) {
                        popUpTo(Screen.SoloBattle.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

// ─── Results overlay ──────────────────────────────────────────────────────────

@Composable
private fun SoloResultsOverlay(
    uiState: SoloBattleUiState,
    onHome: () -> Unit,
    onTryAgain: () -> Unit
) {
    val starCount = when {
        uiState.myReps >= uiState.targetReps && uiState.formAccuracy >= 0.8f -> 3
        uiState.myReps >= (uiState.targetReps * 0.7f)                         -> 2
        uiState.myReps > 0                                                     -> 1
        else                                                                   -> 0
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Background.copy(0.97f), Background)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            // Star rating
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { i ->
                    Text(
                        if (i < starCount) "⭐" else "☆",
                        fontSize = 40.sp,
                        color = if (i < starCount) NeonYellow else TextDisabled
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = when (starCount) {
                    3    -> "PERFECT! 🔥"
                    2    -> "GREAT WORK! 💪"
                    1    -> "NICE TRY! 👏"
                    else -> "LET'S GO! 🚀"
                },
                style = ActiveSparkTypography.displaySmall.copy(
                    color = NeonGreen,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Text(
                "Solo Practice Complete",
                style = ActiveSparkTypography.bodyMedium.copy(color = TextSecondary)
            )

            Spacer(Modifier.height(28.dp))

            // Stats card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SoloStatColumn("REPS",   "${uiState.myReps}",                       NeonGreen)
                    SoloDivider()
                    SoloStatColumn(
                        "FORM",
                        "${(uiState.formAccuracy * 100).toInt()}%",
                        when {
                            uiState.formAccuracy >= 0.8f -> NeonGreen
                            uiState.formAccuracy >= 0.5f -> NeonYellow
                            else                          -> NeonPink
                        }
                    )
                    SoloDivider()
                    SoloStatColumn("TARGET", "${uiState.targetReps}", NeonCyan)
                }
            }

            Spacer(Modifier.height(16.dp))

            // XP pill
            Surface(
                color = NeonGreen.copy(0.12f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = NeonGreen,
                            strokeWidth = 2.dp
                        )
                    }
                    Text(
                        text = if (uiState.isSaving) "Saving +${uiState.xpEarned} XP…"
                               else                  "+${uiState.xpEarned} XP EARNED ⚡",
                        style = ActiveSparkTypography.titleMedium.copy(color = NeonGreen)
                    )
                }
            }

            uiState.saveError?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    "⚠️ $it",
                    style = ActiveSparkTypography.bodySmall.copy(color = NeonPink),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(32.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick  = onHome,
                    modifier = Modifier.weight(1f).height(54.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Icon(Icons.Default.Home, null)
                    Spacer(Modifier.width(6.dp))
                    Text("HOME")
                }
                Button(
                    onClick  = onTryAgain,
                    modifier = Modifier.weight(1f).height(54.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    shape    = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Replay, null, tint = Background)
                    Spacer(Modifier.width(6.dp))
                    Text("TRY AGAIN", color = Background, style = ActiveSparkTypography.labelLarge)
                }
            }
        }
    }
}

// ─── Solo timer bar ───────────────────────────────────────────────────────────

@Composable
private fun SoloTimerBar(secondsRemaining: Int, totalSeconds: Int, challengeName: String) {
    val progress = secondsRemaining.toFloat() / totalSeconds.toFloat()
    val timerColor = when {
        progress > 0.5f  -> NeonGreen
        progress > 0.25f -> NeonYellow
        else             -> NeonPink
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceVariant)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = NeonGreen.copy(0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "🏃 SOLO",
                    style = ActiveSparkTypography.labelSmall.copy(
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Text(
                text = "$secondsRemaining",
                style = ActiveSparkTypography.displayMedium.copy(
                    color = timerColor,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Text(
                challengeName,
                style = ActiveSparkTypography.labelSmall.copy(color = TextTertiary),
                maxLines = 1
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier  = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color      = timerColor,
            trackColor = DividerColor
        )
    }
}

// ─── Animated rep counter ─────────────────────────────────────────────────────

@Composable
private fun AnimatedRepCounter(reps: Int, modifier: Modifier) {
    val scale by animateFloatAsState(
        targetValue    = if (reps > 0) 1.2f else 1f,
        animationSpec  = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        finishedListener = { },
        label          = "rep_pop"
    )

    // Reset scale back after pop
    val displayScale by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = tween(300),
        label         = "rep_settle"
    )

    Box(
        modifier = modifier
            .background(Background.copy(0.85f), RoundedCornerShape(24.dp))
            .border(1.5.dp, NeonGreen.copy(0.5f), RoundedCornerShape(24.dp))
            .padding(horizontal = 32.dp, vertical = 16.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = "$reps",
                style = ActiveSparkTypography.displayLarge.copy(
                    color      = NeonGreen,
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

// ─── Form bar ─────────────────────────────────────────────────────────────────

@Composable
private fun SoloFormBar(accuracy: Float) {
    val formColor = when {
        accuracy >= 0.8f -> NeonGreen
        accuracy >= 0.5f -> NeonYellow
        else             -> NeonPink
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Form", style = ActiveSparkTypography.labelSmall.copy(color = TextTertiary))
            Text(
                "${(accuracy * 100).toInt()}%",
                style = ActiveSparkTypography.labelSmall.copy(color = formColor)
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress  = { accuracy },
            modifier  = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color      = formColor,
            trackColor = HealthBarBackground
        )
    }
}

// ─── Permission screens ───────────────────────────────────────────────────────

@Composable
private fun SoloCameraRationale(onRequest: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        Text("📷", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Camera Needed",
            style = ActiveSparkTypography.titleLarge.copy(color = TextPrimary),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Active Spark uses your camera to count reps and check your form — no video is stored.",
            style = ActiveSparkTypography.bodyMedium.copy(color = TextSecondary),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onRequest,
            colors  = ButtonDefaults.buttonColors(containerColor = NeonGreen)
        ) {
            Text("GRANT ACCESS", style = ActiveSparkTypography.labelLarge.copy(color = Background))
        }
    }
}

@Composable
private fun SoloCameraDenied() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        Text("🚫", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Camera Denied",
            style = ActiveSparkTypography.titleLarge.copy(color = NeonPink),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Enable camera in Settings → Apps → Active Spark to use rep tracking.",
            style = ActiveSparkTypography.bodyMedium.copy(color = TextSecondary),
            textAlign = TextAlign.Center
        )
    }
}

// ─── Small helpers ────────────────────────────────────────────────────────────

@Composable
private fun SoloStatColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = ActiveSparkTypography.labelSmall.copy(color = TextTertiary))
        Text(
            value,
            style = ActiveSparkTypography.headlineMedium.copy(
                color      = color,
                fontWeight = FontWeight.ExtraBold
            )
        )
    }
}

@Composable
private fun SoloDivider() {
    Box(
        modifier = Modifier
            .height(48.dp)
            .width(1.dp)
            .background(DividerColor)
    )
}
