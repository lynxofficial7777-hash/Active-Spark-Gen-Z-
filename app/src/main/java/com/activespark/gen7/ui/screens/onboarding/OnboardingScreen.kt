package com.activespark.gen7.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.activespark.gen7.navigation.Screen
import com.activespark.gen7.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val accentColor: androidx.compose.ui.graphics.Color
)

private val onboardingPages = listOf(
    OnboardingPage("⚡", "BATTLE YOUR FRIENDS", "Challenge friends to real-time fitness battles. Move your body, beat the opponent!", NeonCyan),
    OnboardingPage("🤖", "AI TRACKS YOUR MOVES", "MediaPipe AI watches your form and counts your reps automatically. No cheating allowed!", NeonGreen),
    OnboardingPage("🏆", "CLIMB THE RANKS", "Win battles, earn XP, and rise from Bronze to Master rank on the global leaderboard!", NeonPink),
    OnboardingPage("👪", "SAFE FOR KIDS", "Parents get a full dashboard to track activity, set limits, and cheer you on!", NeonPurple)
)

@Composable
fun OnboardingScreen(navController: NavController) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = onboardingPages[page]
            OnboardingPageContent(item = item)
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(onboardingPages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) NeonCyan else TextDisabled)
                            .size(if (isSelected) 24.dp else 8.dp, 8.dp)
                            .animateContentSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // CTA Button
            val isLastPage = pagerState.currentPage == onboardingPages.lastIndex
            Button(
                onClick = {
                    if (isLastPage) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text(
                    text = if (isLastPage) "LET'S BATTLE! ⚡" else "NEXT →",
                    color = Background,
                    style = ActiveSparkTypography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(item: OnboardingPage) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(item.accentColor.copy(alpha = 0.08f), Background),
                    radius = 800f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Text(text = item.emoji, fontSize = 100.sp)
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = item.title,
                style = ActiveSparkTypography.headlineMedium.copy(color = item.accentColor),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = item.subtitle,
                style = ActiveSparkTypography.bodyLarge.copy(color = TextSecondary),
                textAlign = TextAlign.Center
            )
        }
    }
}
