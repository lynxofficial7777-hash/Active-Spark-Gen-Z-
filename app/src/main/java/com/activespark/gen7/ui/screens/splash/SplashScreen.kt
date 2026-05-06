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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.activespark.gen7.navigation.Screen
import com.activespark.gen7.ui.theme.*
import kotlinx.coroutines.delay

private fun DrawScope.drawLightningBolt(
    color: Color, glowColor: Color, glowAlpha: Float, scale: Float
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val s  = scale
    val path = Path().apply {
        moveTo(cx + 10f * s, cy - 50f * s)
        lineTo(cx - 15f * s, cy - 5f  * s)
        lineTo(cx + 5f  * s, cy - 5f  * s)
        lineTo(cx - 10f * s, cy + 50f * s)
        lineTo(cx + 15f * s, cy + 5f  * s)
        lineTo(cx - 5f  * s, cy + 5f  * s)
        close()
    }
    for (i in 4 downTo 1) {
        drawPath(path = path, color = glowColor.copy(alpha = glowAlpha * 0.15f / i),
            style = Stroke(width = (i * 10).toFloat()))
    }
    drawPath(path = path, color = glowColor.copy(alpha = glowAlpha * 0.35f))
    drawPath(path = path, color = color)
}

@Composable
private fun TypewriterText(
    fullText: String,
    style: androidx.compose.ui.text.TextStyle,
    delayPerChar: Long = 60L,
    startDelay: Long = 900L
) {
    var displayed by remember { mutableStateOf("") }
    LaunchedEffect(fullText) {
        delay(startDelay)
        fullText.forEachIndexed { index, _ ->
            displayed = fullText.substring(0, index + 1)
            delay(delayPerChar)
        }
    }
    Text(
        text = displayed,
        style = style,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

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

    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    val boltScale by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bolt_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    var nameVisible by remember { mutableStateOf(false) }
    val nameAlpha by animateFloatAsState(
        targetValue = if (nameVisible) 1f else 0f,
        animationSpec = tween(1000), label = "name_alpha"
    )
    val nameScale by animateFloatAsState(
        targetValue = if (nameVisible) 1f else 0.85f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing), label = "name_scale"
    )

    LaunchedEffect(Unit) { delay(400); nameVisible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF050508), Color(0xFF0A0A14), Color(0xFF050508))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Background radial glow
        Box(
            modifier = Modifier
                .size(500.dp)
                .align(Alignment.Center)
                .background(
                    Brush.radialGradient(
                        listOf(
                            NeonCyan.copy(alpha = 0.08f * glowAlpha),
                            NeonPink.copy(alpha = 0.03f * glowAlpha),
                            Color.Transparent
                        )
                    )
                )
        )

        // Scanlines
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    var y = 0f
                    while (y < size.height) {
                        drawLine(Color.White.copy(alpha = 0.018f), Offset(0f, y), Offset(size.width, y), 1f)
                        y += 5f
                    }
                }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ── Lightning bolt ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .drawBehind {
                        drawLightningBolt(NeonCyan, NeonCyan, glowAlpha, boltScale)
                    }
            )

            Spacer(Modifier.height(44.dp))

            // ── ACTIVE SPARK ───────────────────────────────────────────────
            Text(
                text = "ACTIVE SPARK",
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = nameAlpha; scaleX = nameScale; scaleY = nameScale },
                style = ActiveSparkTypography.displaySmall.copy(
                    color = NeonCyan,
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.Black
                )
            )

            Spacer(Modifier.height(8.dp))

            // ── GEN 7 ──────────────────────────────────────────────────────
            Text(
                text = "GEN 7",
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = nameAlpha },
                style = ActiveSparkTypography.headlineLarge.copy(
                    color = NeonPink,
                    letterSpacing = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(Modifier.height(36.dp))

            // ── Typewriter tagline ─────────────────────────────────────────
            TypewriterText(
                fullText = "FITNESS. BATTLE. GLORY.",
                style = ActiveSparkTypography.labelLarge.copy(
                    color = TextSecondary,
                    letterSpacing = 3.sp
                ),
                delayPerChar = 55L,
                startDelay = 700L
            )
        }
    }
}
