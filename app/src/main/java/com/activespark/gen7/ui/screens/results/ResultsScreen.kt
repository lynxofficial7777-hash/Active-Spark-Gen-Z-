package com.activespark.gen7.ui.screens.results

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.activespark.gen7.data.models.PlayerRank
import com.activespark.gen7.navigation.Screen
import com.activespark.gen7.ui.theme.*

@Composable
fun ResultsScreen(
    navController: NavController,
    matchId: String,
    viewModel: ResultsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(matchId) { viewModel.loadResults(matchId) }

    // Trophy bounce animation
    val trophyScale by animateFloatAsState(
        targetValue = if (uiState.isWinner) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "trophy_scale"
    )

    // Rank-up glow pulse
    val rankUpPulse = rememberInfiniteTransition(label = "rank_pulse")
    val rankGlow by rankUpPulse.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "rank_glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = when {
                        uiState.newRank != null -> listOf(NeonPurple.copy(0.18f), Background)
                        uiState.isWinner        -> listOf(NeonCyan.copy(0.12f),  Background)
                        uiState.isDraw          -> listOf(NeonYellow.copy(0.08f), Background)
                        else                    -> listOf(NeonPink.copy(0.08f),   Background)
                    }
                )
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {

            Spacer(Modifier.height(16.dp))

            // ── Trophy / result emoji ──────────────────────────────────
            Text(
                text = if (uiState.isWinner) "🏆" else if (uiState.isDraw) "🤝" else "💪",
                fontSize = 96.sp,
                modifier = Modifier.scale(trophyScale)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = when {
                    uiState.isWinner -> "VICTORY!"
                    uiState.isDraw   -> "DRAW!"
                    else             -> "DEFEATED!"
                },
                style = ActiveSparkTypography.displaySmall.copy(
                    color = when {
                        uiState.isWinner -> NeonCyan
                        uiState.isDraw   -> NeonYellow
                        else             -> NeonPink
                    },
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Text(
                text = when {
                    uiState.isWinner -> "You crushed it! Keep battling!"
                    uiState.isDraw   -> "An even match! Try again!"
                    else             -> "Train harder, come back stronger!"
                },
                style = ActiveSparkTypography.bodyMedium.copy(color = TextSecondary),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            // ── Reps comparison card ───────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ResultsStatColumn("YOUR REPS",  "${uiState.myReps}",       NeonCyan)
                    VerticalDivider()
                    ResultsStatColumn("OPP. REPS",  "${uiState.opponentReps}", NeonPink)
                    VerticalDivider()
                    ResultsStatColumn(
                        label = "FORM",
                        value = "${(uiState.formAccuracy * 100).toInt()}%",
                        color = when {
                            uiState.formAccuracy >= 0.8f -> NeonGreen
                            uiState.formAccuracy >= 0.5f -> NeonYellow
                            else -> NeonPink
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── XP earned pill ─────────────────────────────────────────
            Surface(
                color = NeonGreenSubtle,
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

            // ── Save error (offline / network failure) ─────────────────
            AnimatedVisibility(visible = uiState.saveError != null) {
                uiState.saveError?.let { error ->
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = NeonPink.copy(0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "⚠️ $error",
                            style = ActiveSparkTypography.bodySmall.copy(color = NeonPink),
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ── Rank-up celebration ────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.newRank != null,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioLowBouncy)) + fadeIn()
            ) {
                uiState.newRank?.let { newRank ->
                    Spacer(Modifier.height(16.dp))
                    RankUpCard(
                        from     = uiState.previousRank ?: PlayerRank.BRONZE,
                        to       = newRank,
                        glowAlpha = rankGlow
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            // ── Action buttons ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier.weight(1f).height(54.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Icon(Icons.Default.Home, null)
                    Spacer(Modifier.width(6.dp))
                    Text("HOME")
                }

                Button(
                    onClick = {
                        val cId = uiState.challengeId
                        if (cId.isNotEmpty()) {
                            navController.navigate(Screen.Matchmaking.createRoute(cId)) {
                                popUpTo(Screen.Results.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Replay, null, tint = Background)
                    Spacer(Modifier.width(6.dp))
                    Text("REMATCH", color = Background, style = ActiveSparkTypography.labelLarge)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Rank-up celebration card ─────────────────────────────────────────────────

@Composable
private fun RankUpCard(from: PlayerRank, to: PlayerRank, glowAlpha: Float) {
    val toColor = rankColor(to)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(listOf(toColor.copy(0.15f), NeonPurple.copy(0.15f))),
                RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    listOf(toColor.copy(glowAlpha), NeonPurple.copy(glowAlpha))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🚀 RANK UP!", style = ActiveSparkTypography.titleLarge.copy(color = toColor))
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    from.displayName.uppercase(),
                    style = ActiveSparkTypography.titleSmall.copy(color = rankColor(from))
                )
                Text(
                    "  →  ",
                    style = ActiveSparkTypography.titleSmall.copy(color = TextTertiary)
                )
                Text(
                    to.displayName.uppercase(),
                    style = ActiveSparkTypography.titleMedium.copy(
                        color = toColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "You're on fire! Keep battling to climb higher! 🔥",
                style = ActiveSparkTypography.bodySmall.copy(color = TextSecondary),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun rankColor(rank: PlayerRank): Color = when (rank) {
    PlayerRank.BRONZE   -> RankBronze
    PlayerRank.SILVER   -> RankSilver
    PlayerRank.GOLD     -> RankGold
    PlayerRank.PLATINUM -> RankPlatinum
    PlayerRank.DIAMOND  -> RankDiamond
    PlayerRank.MASTER   -> RankMaster
}

// ─── Helper composables ───────────────────────────────────────────────────────

@Composable
private fun ResultsStatColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = ActiveSparkTypography.labelSmall.copy(color = TextTertiary))
        Text(
            value,
            style = ActiveSparkTypography.headlineMedium.copy(
                color = color,
                fontWeight = FontWeight.ExtraBold
            )
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .height(48.dp)
            .width(1.dp)
            .background(DividerColor)
    )
}
