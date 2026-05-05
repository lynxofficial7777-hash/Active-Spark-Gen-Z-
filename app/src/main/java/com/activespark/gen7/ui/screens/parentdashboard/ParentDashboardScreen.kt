package com.activespark.gen7.ui.screens.parentdashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.activespark.gen7.ui.theme.*

@Composable
fun ParentDashboardScreen(
    navController: NavController,
    childUid: String,
    viewModel: ParentDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(childUid) { viewModel.loadChild(childUid) }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("PARENT DASHBOARD 👪", style = ActiveSparkTypography.titleMedium.copy(color = NeonCyan)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
                actions = {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp).padding(end = 16.dp),
                            color = NeonCyan,
                            strokeWidth = 2.dp
                        )
                    }
                }
            )
        }
    ) { padding ->
        // Show save error as a snackbar-style banner
        uiState.saveError?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeonPink.copy(0.15f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "⚠️ $error",
                    style = ActiveSparkTypography.bodySmall.copy(color = NeonPink)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─── Child Overview Card ──────────────────────────────────────
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("👦", fontSize = 40.sp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(uiState.childName, style = ActiveSparkTypography.titleLarge.copy(color = TextPrimary))
                                Text("Level ${uiState.childLevel} · ${uiState.childXp} XP", style = ActiveSparkTypography.bodySmall.copy(color = NeonCyan))
                            }
                        }
                    }
                }
            }

            // ─── Weekly Activity ──────────────────────────────────────────
            item {
                SectionCard(title = "📊 WEEKLY ACTIVITY") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ActivityStat(icon = "⚔️", value = "${uiState.weeklyBattles}", label = "Battles")
                        ActivityStat(icon = "⏱️", value = "${uiState.weeklyMinutes}m", label = "Active")
                        ActivityStat(icon = "🔥", value = "${uiState.weeklyCalories}", label = "Cal")
                        ActivityStat(icon = "🏆", value = "${uiState.weeklyWins}", label = "Wins")
                    }
                }
            }

            // ─── Screen Time Limit ────────────────────────────────────────
            item {
                SectionCard(title = "⏰ DAILY SCREEN TIME LIMIT") {
                    var sliderValue by remember { mutableFloatStateOf(uiState.dailyLimitHours.toFloat()) }
                    Column {
                        Text(
                            "${sliderValue.toInt()} hours per day",
                            style = ActiveSparkTypography.titleMedium.copy(color = NeonCyan)
                        )
                        Spacer(Modifier.height(8.dp))
                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            onValueChangeFinished = { viewModel.setDailyLimit(sliderValue.toInt()) },
                            valueRange = 1f..8f,
                            steps = 6,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonCyan,
                                activeTrackColor = NeonCyan,
                                inactiveTrackColor = DividerColor
                            )
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("1h", style = ActiveSparkTypography.labelSmall.copy(color = TextTertiary))
                            Text("8h", style = ActiveSparkTypography.labelSmall.copy(color = TextTertiary))
                        }
                    }
                }
            }

            // ─── Content Controls ─────────────────────────────────────────
            item {
                SectionCard(title = "🔒 SAFETY CONTROLS") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ParentToggle(
                            label = "Allow Online Battles",
                            checked = uiState.allowOnlineBattles,
                            onToggle = viewModel::toggleOnlineBattles
                        )
                        ParentToggle(
                            label = "Allow Chat",
                            checked = uiState.allowChat,
                            onToggle = viewModel::toggleChat
                        )
                        ParentToggle(
                            label = "Push Notifications",
                            checked = uiState.allowNotifications,
                            onToggle = viewModel::toggleNotifications
                        )
                    }
                }
            }

            // ─── Recent Battles ───────────────────────────────────────────
            item {
                SectionCard(title = "⚔️ RECENT BATTLES") {
                    if (uiState.recentBattles.isEmpty()) {
                        Text(
                            "No battles yet — encourage them to start!",
                            style = ActiveSparkTypography.bodyMedium.copy(color = TextTertiary)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.recentBattles.forEach { battle ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(battle.first, style = ActiveSparkTypography.bodyMedium.copy(color = TextSecondary))
                                    Text(battle.second, style = ActiveSparkTypography.bodyMedium.copy(color = if (battle.second == "WIN") NeonGreen else NeonPink))
                                }
                                Divider(color = DividerColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, style = ActiveSparkTypography.titleSmall.copy(color = TextTertiary))
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun ActivityStat(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 24.sp)
        Text(value, style = ActiveSparkTypography.titleLarge.copy(color = NeonCyan))
        Text(label, style = ActiveSparkTypography.labelSmall.copy(color = TextTertiary))
    }
}

@Composable
private fun ParentToggle(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = ActiveSparkTypography.bodyMedium.copy(color = TextPrimary))
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Background,
                checkedTrackColor = NeonCyan,
                uncheckedTrackColor = DividerColor
            )
        )
    }
}
