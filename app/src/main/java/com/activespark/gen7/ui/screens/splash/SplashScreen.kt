package com.activespark.gen7.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.activespark.gen7.navigation.Screen
import com.activespark.gen7.ui.theme.*
import kotlinx.coroutines.delay

// ─── Lightning bolt path drawing ─────────────────────────────────────────────

private fun DrawScope.drawLightningBolt(
    color: Color,
    glowColor: Color,
    glowAlpha: Float,
    scale: Float
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val s = scale

    // Bolt shape: classic ⚡ polygon
    val path = Path().apply {
        moveTo(cx + 10f * s, cy - 50f * s)
        lineTo(cx - 15f * s, cy - 5f * s)
        lineTo(cx + 5f * s, cy - 5f * s)
        lineTo(cx - 10f * s, cy + 50f * s)
        lineTo(cx + 15f * s, cy + 5f * s)
        lineTo(cx - 5f * s, cy + 5f * s)
        close()
    }

    // Outer glow (blurred look via multiple strokes)
    for (i in 4 downTo 1) {
        drawPath(
            path = path,
            color = glowColor.copy(alpha = glowAlpha * 0.15f / i),
            style = Stroke(width = (i * 8).toFloat())
        )
    }
    // Glow fill
    drawPath(path = path, color = glowColor.copy(alpha = glowAlpha * 0.3f))
    // Solid bolt
    drawPath(path = path, color = color)
}

// ─── Typewriter effect ────────────────────────────────────────────────────────

@Composable
private fun TypewriterText(
    fullText: String,
    style: androidx.compose.ui.text.TextStyle,
    delayPerChar: Long = 60L,
    startDelay: Long = 800L
) {
    var displayed by remember { mutableStateOf("") }
    LaunchedEffect(fullText) {
        delay(startDelay)
        fullText.forEachIndexed { index, _ ->
            displayed = fullText.substring(0, index + 1)
            delay(delayPerChar)
        }
    }
    Text(text = displayed, style = style)
}

// ─── Splash Screen ────────────────────────────────────────────────────────────

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val destination by viewModel.navigationDestination.collectAsStateWithLifecycle()

    LaunchedEffect(destination) {
        destination?.let { route ->
            navController.navigate(route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_anim")

    // Lightning bolt scale pulse
    val boltScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bolt_scale"
    )

    // Neon glow intensity
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // Outer ring rotation
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation"
    )

    // App name letter spacing animation (entrance)
    var nameVisible by remember { mutableStateOf(false) }
    val nameAlpha by animateFloatAsState(
        targetValue = if (nameVisible) 1f else 0f,
        animationSpec = tween(800),
        label = "name_alpha"
    )
    val nameLetterSpacing by animateFloatAsState(
        targetValue = if (nameVisible) 8f else 2f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "name_spacing"
    )

    LaunchedEffect(Unit) {
        delay(300)
        nameVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F)),
        contentAlignment = Alignment.Center
    ) {
        // Radial background glow
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.Center)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.06f * glowAlpha),
                            Color.Transparent
                        )
                    ),
                    androidx.compose.foundation.shape.CircleShape
                )
        )

        // Scanlines overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.025f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += 6f
                    }
                }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ── Animated Lightning Bolt Logo ──────────────────────────────
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .drawBehind {
                        drawLightningBolt(
                            color = NeonCyan,
                            glowColor = NeonCyan,
                            glowAlpha = glowAlpha,
                            scale = boltScale
                        )
                    }
            )

            Spacer(Modifier.height(32.dp))

            // ── App Name in Orbitron with entrance animation ───────────────
            Text(
                text = "ACTIVE SPARK",
                style = ActiveSparkTypography.displaySmall.copy(
                    color = NeonCyan.copy(alpha = nameAlpha),
                    letterSpacing = nameLetterSpacing.sp,
                    fontWeight = FontWeight.Black
                )
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "GEN 7",
                style = ActiveSparkTypography.headlineMedium.copy(
                    color = NeonPink.copy(alpha = nameAlpha),
                    letterSpacing = (nameLetterSpacing + 4f).sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(Modifier.height(24.dp))

            // ── Tagline with typewriter animation ─────────────────────────
            TypewriterText(
                fullText = "FITNESS. BATTLE. GLORY.",
                style = ActiveSparkTypography.labelLarge.copy(
                    color = TextSecondary,
                    letterSpacing = 4.sp
                ),
                delayPerChar = 55L,
                startDelay = 600L
            )
        }
    }
}
