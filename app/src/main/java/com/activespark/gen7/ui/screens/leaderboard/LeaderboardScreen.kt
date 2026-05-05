package com.activespark.gen7.ui.screens.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.activespark.gen7.data.models.LeaderboardEntry
import com.activespark.gen7.data.models.LeaderboardPeriod
import com.activespark.gen7.data.models.PlayerRank
import com.activespark.gen7.ui.theme.*

@Composable
fun LeaderboardScreen(
    navController: NavController,
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(colors = listOf(SurfaceVariant, Background))
                    )
                    .padding(top = 20.dp, bottom = 8.dp)
            ) {
                Text(
                    "🏆 LEADERBOARD",
                    style = ActiveSparkTypography.headlineSmall.copy(color = NeonCyan),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(12.dp))

                // Period tabs
                ScrollableTabRow(
                    selectedTabIndex = uiState.selectedPeriodIndex,
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    edgePadding = 20.dp,
                    indicator = {},
                    divider = {}
                ) {
                    LeaderboardPeriod.entries.forEachIndexed { index, period ->
                        val isSelected = index == uiState.selectedPeriodIndex
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectPeriod(period) },
                            label = {
                                Text(
                                    period.displayName,
                                    style = ActiveSparkTypography.labelMedium.copy(
                                        color = if (isSelected) Background else TextSecondary
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                containerColor = SurfaceVariant
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonCyan)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                itemsIndexed(uiState.entries) { index, entry ->
                    LeaderboardRow(
                        rank = index + 1,
                        entry = entry,
                        isCurrentUser = entry.playerUid == uiState.currentUserId
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(rank: Int, entry: LeaderboardEntry, isCurrentUser: Boolean) {
    val rankColors = mapOf(1 to RankGold, 2 to RankSilver, 3 to RankBronze)
    val rankColor = rankColors[rank] ?: TextTertiary
    val bg = if (isCurrentUser) NeonCyanSubtle else androidx.compose.ui.graphics.Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank number / medal
        Box(
            modifier = Modifier.width(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "#$rank" },
                style = ActiveSparkTypography.titleMedium.copy(
                    color = rankColor,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(Modifier.width(12.dp))

        // Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(NeonCyanGlow),
            contentAlignment = Alignment.Center
        ) {
            Text("⚡", fontSize = 22.sp)
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.username,
                style = ActiveSparkTypography.titleSmall.copy(
                    color = if (isCurrentUser) NeonCyan else TextPrimary
                )
            )
            val rankNameColor = when (entry.playerRank) {
                PlayerRank.MASTER -> RankMaster
                PlayerRank.DIAMOND -> RankDiamond
                PlayerRank.PLATINUM -> RankPlatinum
                else -> TextTertiary
            }
            Text(
                entry.playerRank.displayName,
                style = ActiveSparkTypography.labelSmall.copy(color = rankNameColor)
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${entry.totalXp} XP",
                style = ActiveSparkTypography.titleSmall.copy(color = NeonCyan, fontWeight = FontWeight.Bold)
            )
            Text(
                "${entry.totalWins}W",
                style = ActiveSparkTypography.labelSmall.copy(color = TextTertiary)
            )
        }
    }

    Divider(color = DividerColor, modifier = Modifier.padding(horizontal = 20.dp))
}
