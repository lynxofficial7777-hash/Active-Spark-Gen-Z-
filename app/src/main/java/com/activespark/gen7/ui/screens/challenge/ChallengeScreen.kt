package com.activespark.gen7.ui.screens.challenge

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.activespark.gen7.data.models.Challenge
import com.activespark.gen7.data.models.ExerciseType
import com.activespark.gen7.navigation.Screen
import com.activespark.gen7.ui.theme.*
import com.airbnb.lottie.compose.*

@Composable
fun ChallengeScreen(
    navController: NavController,
    challengeId: String,
    viewModel: ChallengeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(challengeId) { viewModel.loadChallenge(challengeId) }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "CHOOSE YOUR BATTLE",
                        style = ActiveSparkTypography.titleMedium.copy(color = NeonCyan)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        }
    ) { padding ->
        uiState.challenge?.let { challenge ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ── Exercise Animation ─────────────────────────────────────
                LottieExerciseAnimation(
                    animationUrl = challenge.animationUrl,
                    exerciseType = challenge.exerciseType
                )

                Spacer(Modifier.height(24.dp))

                // ── Name + description ─────────────────────────────────────
                Text(
                    challenge.name,
                    style = ActiveSparkTypography.headlineMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    challenge.description,
                    style = ActiveSparkTypography.bodyMedium.copy(color = TextSecondary),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))

                // ── Stats row ──────────────────────────────────────────────
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatPill("⏱️", "${challenge.durationSeconds}s", modifier = Modifier.weight(1f))
                    StatPill("🎯", "${challenge.targetReps} reps", modifier = Modifier.weight(1f))
                    StatPill("⚡", "+${challenge.xpReward} XP", modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(24.dp))

                // ── Difficulty badge ───────────────────────────────────────
                val diffColor = when (challenge.difficulty.name) {
                    "BEGINNER"     -> NeonGreen
                    "INTERMEDIATE" -> NeonYellow
                    "ADVANCED"     -> NeonPink
                    else           -> NeonPurple
                }
                Surface(
                    color = diffColor.copy(0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "⚡ ${challenge.difficulty.displayName}",
                        style = ActiveSparkTypography.labelLarge.copy(color = diffColor),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                Spacer(Modifier.height(32.dp))

                // ── Instructions card ──────────────────────────────────────
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "📋 HOW TO DO IT",
                            style = ActiveSparkTypography.labelLarge.copy(color = NeonCyan)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            challenge.instruction,
                            style = ActiveSparkTypography.bodyMedium.copy(color = TextSecondary)
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // ── Battle button ──────────────────────────────────────────
                Button(
                    onClick = {
                        navController.navigate(
                            Screen.Matchmaking.createRoute(challenge.challengeId)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "FIND OPPONENT ⚔️",
                        color = Background,
                        style = ActiveSparkTypography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── Solo Practice button ────────────────────────────────────
                OutlinedButton(
                    onClick = {
                        navController.navigate(
                            Screen.SoloBattle.createRoute(challenge.challengeId)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonGreen),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "🏃 SOLO PRACTICE",
                        style = ActiveSparkTypography.titleSmall.copy(
                            color = NeonGreen,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    "Practice alone · Earn XP · No waiting",
                    style = ActiveSparkTypography.bodySmall.copy(color = TextTertiary),
                    textAlign = TextAlign.Center
                )
            }
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NeonCyan)
        }
    }
}

// ── Lottie Exercise Animation ─────────────────────────────────────────────────

/**
 * Displays the exercise demo animation from a Lottie URL.
 *
 * Loading states:
 *  1. URL provided  → load from network, loop automatically, show play/pause button
 *  2. URL empty     → show exercise emoji with a pulsing neon glow
 *  3. Load error    → fall back to emoji (silent, no crash)
 */
@Composable
private fun LottieExerciseAnimation(
    animationUrl: String,
    exerciseType: ExerciseType
) {
    var isPlaying by remember { mutableStateOf(true) }

    // Decide source: URL or nothing (will fall through to emoji)
    val hasUrl = animationUrl.isNotBlank()

    // Load composition (only fires when hasUrl = true)
    val compositionResult by rememberLottieComposition(
        spec = if (hasUrl) LottieCompositionSpec.Url(animationUrl)
               else        LottieCompositionSpec.Asset("lottie/placeholder.json")
    )

    val progress by animateLottieCompositionAsState(
        composition = compositionResult,
        isPlaying = isPlaying,
        iterations = LottieConstants.IterateForever,
        restartOnPlay = false
    )

    // Pulsing glow animation for the emoji fallback
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by pulseAnim.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .size(220.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(NeonCyan.copy(0.15f), NeonPink.copy(0.08f))
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(NeonCyan.copy(glowAlpha), NeonPink.copy(glowAlpha))
                ),
                shape = RoundedCornerShape(32.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (hasUrl && compositionResult != null) {
            // ── Lottie player ──────────────────────────────────────────────
            LottieAnimation(
                composition = compositionResult,
                progress = { progress },
                modifier = Modifier.fillMaxSize().padding(12.dp)
            )

            // Play / pause button (bottom-right corner)
            IconButton(
                onClick = { isPlaying = !isPlaying },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(36.dp)
                    .background(Background.copy(0.75f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            // "DEMO" label top-left
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(NeonCyan.copy(0.15f), RoundedCornerShape(6.dp))
                    .border(1.dp, NeonCyan.copy(0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    "DEMO",
                    style = ActiveSparkTypography.labelSmall.copy(
                        color = NeonCyan,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }
        } else {
            // ── Emoji fallback ─────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(exerciseType.icon, fontSize = 80.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    exerciseType.displayName.uppercase(),
                    style = ActiveSparkTypography.labelMedium.copy(
                        color = NeonCyan.copy(glowAlpha),
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }
        }
    }
}

// ── Stat Pill ─────────────────────────────────────────────────────────────────

@Composable
private fun StatPill(icon: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 20.sp)
            Text(value, style = ActiveSparkTypography.titleSmall.copy(color = NeonCyan))
        }
    }
}
