package com.activespark.gen7.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.activespark.gen7.data.models.Challenge
import com.activespark.gen7.navigation.Screen
import com.activespark.gen7.ui.theme.*
import kotlin.math.sin
import kotlin.random.Random

// ─── Scanline + particle overlay ─────────────────────────────────────────────

private fun DrawScope.drawScanlines(alpha: Float) {
    val lineSpacing = 6f
    var y = 0f
    while (y < size.height) {
        drawLine(
            color = Color.White.copy(alpha = alpha * 0.04f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        y += lineSpacing
    }
}

private data class Particle(val x: Float, val y: Float, val radius: Float, val alpha: Float)

@Composable
private fun rememberParticles(count: Int = 30): List<Particle> {
    return remember {
        List(count) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                radius = Random.nextFloat() * 2f + 0.5f,
                alpha = Random.nextFloat() * 0.5f + 0.1f
            )
        }
    }
}

// ─── Neon glow border helper ──────────────────────────────────────────────────

@Composable
private fun Modifier.neonBorder(
    color: Color,
    glowColor: Color,
    cornerRadius: Dp = 16.dp,
    width: Dp = 1.dp
): Modifier = this
    .border(width, glowColor.copy(alpha = 0.3f), RoundedCornerShape(cornerRadius + 4.dp))
    .border(width, color.copy(alpha = 0.7f), RoundedCornerShape(cornerRadius))

// ─── Main Screen ──────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val particles = rememberParticles(25)

    val infiniteTransition = rememberInfiniteTransition(label = "home_ambient")
    val scanlineAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanline"
    )
    val particleTick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_tick"
    )

    Scaffold(
        containerColor = Background,
        bottomBar = {
            SparkBottomBar(
                navController = navController,
                currentRoute = Screen.Home.route,
                currentUid = uiState.user?.uid
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0F))
        ) {
            // Sci-fi scanline + particle overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        drawScanlines(scanlineAlpha)
                        // Animated particles
                        particles.forEach { p ->
                            val animY = (p.y + particleTick * 0.15f) % 1f
                            drawCircle(
                                color = NeonCyan.copy(alpha = p.alpha * 0.6f),
                                radius = p.radius,
                                center = Offset(p.x * size.width, animY * size.height)
                            )
                        }
                    }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // ─── Header ───────────────────────────────────────────────
                item {
                    HomeHeader(
                        username = uiState.user?.username ?: "Warrior",
                        level = uiState.user?.level ?: 1,
                        xp = uiState.user?.xp ?: 0,
                        onProfileClick = {
                            val uid = uiState.user?.uid ?: return@HomeHeader
                            navController.navigate(Screen.Profile.createRoute(uid))
                        }
                    )
                }

                // ─── Quick Battle Button ───────────────────────────────────
                item {
                    QuickBattleCard(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        onBattleClick = {
                            val challengeId = uiState.challenges.firstOrNull()?.challengeId ?: "default"
                            navController.navigate(Screen.Challenge.createRoute(challengeId))
                        }
                    )
                }

                // ─── Daily Challenge ───────────────────────────────────────
                item {
                    SectionHeader(
                        title = "DAILY CHALLENGE 🔥",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                item {
                    uiState.dailyChallenge?.let { challenge ->
                        DailyChallengeCard(
                            challenge = challenge,
                            modifier = Modifier.padding(horizontal = 20.dp),
                            onClick = { navController.navigate(Screen.Challenge.createRoute(challenge.challengeId)) }
                        )
                    }
                }

                // ─── Choose Challenge ──────────────────────────────────────
                item {
                    SectionHeader(
                        title = "ALL BATTLES ⚡",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        items(uiState.challenges) { challenge ->
                            ChallengeChip(
                                challenge = challenge,
                                onClick = { navController.navigate(Screen.Challenge.createRoute(challenge.challengeId)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Header with animated neon avatar ring ────────────────────────────────────

@Composable
private fun HomeHeader(
    username: String,
    level: Int,
    xp: Int,
    onProfileClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_ring")
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_alpha"
    )
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0F1A), Color(0xFF0A0A0F))
                )
            )
            .padding(20.dp)
    ) {
        // Ambient glow behind header
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.TopStart)
                .offset(x = (-40).dp, y = (-40).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(NeonCyan.copy(alpha = 0.08f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Avatar with animated neon ring
            Box(contentAlignment = Alignment.Center) {
                // Outer glow ring (animated)
                Box(
                    modifier = Modifier
                        .size((56 + 16).dp)
                        .drawBehind {
                            drawCircle(
                                color = NeonCyan.copy(alpha = ringAlpha * 0.4f),
                                radius = size.minDimension / 2f * ringScale
                            )
                        }
                )
                // Neon border ring
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(
                                listOf(NeonCyan, NeonPink, NeonCyan)
                            ),
                            shape = CircleShape
                        )
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF1A1A2E), Color(0xFF0A0A0F))
                            )
                        )
                        .clickable(onClick = onProfileClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚡", fontSize = 28.sp)
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "HEY, ${username.uppercase()}!",
                    style = ActiveSparkTypography.titleLarge.copy(
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "LVL $level  ·  $xp XP",
                    style = ActiveSparkTypography.bodySmall.copy(
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                )
            }

            IconButton(onClick = { }) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = NeonCyan.copy(alpha = 0.7f))
            }
        }
    }
}

// ─── Quick Battle Card — glowing arena ───────────────────────────────────────

@Composable
private fun QuickBattleCard(modifier: Modifier = Modifier, onBattleClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "battle_glow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "battle_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            // Outer glow layer
            .drawBehind {
                drawRoundRect(
                    color = NeonCyan.copy(alpha = 0.15f + glowPulse * 0.1f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                    size = this.size.copy(
                        width = this.size.width + 8.dp.toPx(),
                        height = this.size.height + 8.dp.toPx()
                    ),
                    topLeft = Offset(-4.dp.toPx(), -4.dp.toPx())
                )
            }
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0D1F2D),
                        Color(0xFF0A0A0F),
                        Color(0xFF1A0D1F)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        NeonCyan.copy(alpha = 0.4f + glowPulse * 0.4f),
                        NeonPink.copy(alpha = 0.3f + glowPulse * 0.3f),
                        NeonCyan.copy(alpha = 0.4f + glowPulse * 0.4f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onBattleClick)
            .padding(28.dp)
    ) {
        // Background arena grid lines
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .drawWithContent {
                    drawContent()
                    // Horizontal grid lines
                    for (i in 0..4) {
                        val y = size.height * i / 4f
                        drawLine(
                            color = NeonCyan.copy(alpha = 0.05f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                    }
                    // Vertical grid lines
                    for (i in 0..6) {
                        val x = size.width * i / 6f
                        drawLine(
                            color = NeonCyan.copy(alpha = 0.05f),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f
                        )
                    }
                }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("⚔️", fontSize = 52.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                "QUICK BATTLE",
                style = ActiveSparkTypography.headlineSmall.copy(
                    color = NeonCyan,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "CHALLENGE A RANDOM OPPONENT NOW",
                style = ActiveSparkTypography.labelMedium.copy(
                    color = TextSecondary,
                    letterSpacing = 2.sp
                )
            )
            Spacer(Modifier.height(16.dp))
            // Glowing ENTER ARENA button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(NeonCyan.copy(0.2f), NeonPink.copy(0.15f))
                        )
                    )
                    .border(1.dp, NeonCyan.copy(alpha = 0.6f + glowPulse * 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    "ENTER ARENA",
                    style = ActiveSparkTypography.labelLarge.copy(
                        color = NeonCyan,
                        letterSpacing = 3.sp
                    )
                )
            }
        }
    }
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .background(NeonCyan, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = ActiveSparkTypography.titleMedium.copy(
                color = TextTertiary,
                letterSpacing = 2.sp
            )
        )
    }
}

// ─── Daily Challenge Card ─────────────────────────────────────────────────────

@Composable
private fun DailyChallengeCard(challenge: Challenge, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF0F1A1F), Color(0xFF0A0A0F))
                )
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(NeonGreen.copy(0.5f), NeonCyan.copy(0.3f))
                ),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Icon with neon glow background
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeonGreen.copy(alpha = 0.1f))
                    .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(challenge.exerciseType.icon, fontSize = 28.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    challenge.name,
                    style = ActiveSparkTypography.titleMedium.copy(color = TextPrimary)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${challenge.durationSeconds}s  ·  ${challenge.xpReward} XP  ·  ${challenge.targetReps} REPS",
                    style = ActiveSparkTypography.bodySmall.copy(color = NeonGreen)
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = NeonCyan)
        }
    }
}

// ─── Challenge Chip with neon border glow ─────────────────────────────────────

@Composable
private fun ChallengeChip(challenge: Challenge, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "chip_glow")
    val chipGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000 + (challenge.name.length * 100), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chip_border"
    )

    Box(
        modifier = Modifier
            .width(130.dp)
            // Outer glow
            .drawBehind {
                drawRoundRect(
                    color = NeonCyan.copy(alpha = chipGlow * 0.2f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
                    size = this.size.copy(
                        width = this.size.width + 6.dp.toPx(),
                        height = this.size.height + 6.dp.toPx()
                    ),
                    topLeft = Offset(-3.dp.toPx(), -3.dp.toPx())
                )
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0F1A), Color(0xFF0A0A0F))
                )
            )
            .border(
                1.dp,
                NeonCyan.copy(alpha = chipGlow),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Icon with neon glow box
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(NeonCyan.copy(alpha = 0.08f))
                    .border(1.dp, NeonCyan.copy(alpha = chipGlow * 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(challenge.exerciseType.icon, fontSize = 24.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                challenge.exerciseType.displayName,
                style = ActiveSparkTypography.labelMedium.copy(
                    color = TextPrimary,
                    letterSpacing = 0.5.sp
                ),
                maxLines = 2
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${challenge.xpReward} XP",
                style = ActiveSparkTypography.labelSmall.copy(color = NeonCyan)
            )
        }
    }
}

// ─── Bottom Navigation Bar ────────────────────────────────────────────────────

@Composable
private fun SparkBottomBar(
    navController: NavController,
    currentRoute: String,
    currentUid: String?
) {
    val profileRoute = currentUid?.let { Screen.Profile.createRoute(it) } ?: Screen.Home.route
    NavigationBar(containerColor = BottomBarBackground) {
        val items = listOf(
            Triple(Icons.Default.Home, "Home", Screen.Home.route),
            Triple(Icons.Default.EmojiEvents, "Rank", Screen.Leaderboard.route),
            Triple(Icons.Default.Person, "Profile", profileRoute)
        )
        items.forEach { (icon, label, route) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, style = ActiveSparkTypography.labelSmall) },
                selected = currentRoute == route,
                onClick = { if (currentRoute != route) navController.navigate(route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NeonCyan,
                    selectedTextColor = NeonCyan,
                    unselectedIconColor = TextTertiary,
                    unselectedTextColor = TextTertiary,
                    indicatorColor = NeonCyanSubtle
                )
            )
        }
    }
}
