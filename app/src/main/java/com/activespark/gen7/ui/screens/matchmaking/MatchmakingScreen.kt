package com.activespark.gen7.ui.screens.matchmaking

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.activespark.gen7.navigation.Screen
import com.activespark.gen7.ui.theme.*

@Composable
fun MatchmakingScreen(
    navController: NavController,
    challengeId: String,
    viewModel: MatchmakingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(challengeId) { viewModel.startMatchmaking(challengeId) }

    // Navigate when match is found
    LaunchedEffect(uiState.matchId) {
        uiState.matchId?.let { matchId ->
            navController.navigate(Screen.Battle.createRoute(matchId)) {
                popUpTo(Screen.Matchmaking.route) { inclusive = true }
            }
        }
    }

    // Pulsing radar animation
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val radarScale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_scale"
    )
    val radarAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        // Radar rings
        Box(
            modifier = Modifier
                .size(300.dp)
                .scale(radarScale)
                .background(NeonCyan.copy(alpha = radarAlpha), CircleShape)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Player avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Brush.radialGradient(listOf(NeonCyanGlow, Background)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("⚡", fontSize = 52.sp)
            }

            Spacer(Modifier.height(32.dp))
            Text(
                "SEARCHING FOR OPPONENT...",
                style = ActiveSparkTypography.headlineSmall.copy(color = NeonCyan)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Elapsed: ${uiState.elapsedSeconds}s",
                style = ActiveSparkTypography.bodyMedium.copy(color = TextTertiary)
            )
            Spacer(Modifier.height(32.dp))

            // Queue size indicator
            Surface(
                color = NeonGreenSubtle,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    "${uiState.queueSize} players searching",
                    style = ActiveSparkTypography.labelMedium.copy(color = NeonGreen),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(48.dp))

            OutlinedButton(
                onClick = {
                    viewModel.cancelMatchmaking()
                    navController.popBackStack()
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPink),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(NeonPink, NeonPink))
                )
            ) {
                Icon(Icons.Default.Close, null)
                Spacer(Modifier.width(8.dp))
                Text("CANCEL", style = ActiveSparkTypography.labelLarge.copy(color = NeonPink))
            }
        }
    }
}
