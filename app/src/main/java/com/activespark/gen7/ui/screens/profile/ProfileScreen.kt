package com.activespark.gen7.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.activespark.gen7.data.models.AvatarConfig
import com.activespark.gen7.data.models.PlayerRank
import com.activespark.gen7.data.models.User
import com.activespark.gen7.navigation.Screen
import com.activespark.gen7.ui.screens.avatar.AvatarPreview
import com.activespark.gen7.ui.theme.*

@Composable
fun ProfileScreen(
    navController: NavController,
    uid: String,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uid) { viewModel.loadUser(uid) }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("PROFILE", style = ActiveSparkTypography.titleMedium.copy(color = NeonCyan)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    if (uiState.isOwnProfile) {
                        // Edit avatar shortcut
                        IconButton(onClick = {
                            navController.navigate(Screen.AvatarCustomization.route)
                        }) {
                            Icon(Icons.Default.Face, "Edit Avatar", tint = NeonCyan)
                        }
                        IconButton(onClick = {
                            viewModel.signOut()
                            navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                        }) {
                            Icon(Icons.Default.Logout, "Sign Out", tint = NeonPink)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        }
    ) { padding ->
        uiState.user?.let { user ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item { ProfileHero(user = user, isOwnProfile = uiState.isOwnProfile, navController = navController) }
                item { Spacer(Modifier.height(24.dp)) }
                item { StatsGrid(user = user) }
                item { Spacer(Modifier.height(24.dp)) }
                item { BadgesSection(badges = user.badges) }
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = NeonCyan)
        }
    }
}

@Composable
private fun ProfileHero(user: User, isOwnProfile: Boolean, navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(colors = listOf(SurfaceVariant, Background)))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Dynamic avatar preview
            AvatarPreview(config = user.avatarConfig, size = 120)

            Spacer(Modifier.height(12.dp))
            Text(user.displayName, style = ActiveSparkTypography.headlineSmall.copy(color = TextPrimary))
            Text("@${user.username}", style = ActiveSparkTypography.bodyMedium.copy(color = TextTertiary))
            Spacer(Modifier.height(8.dp))
            RankBadge(rank = user.rank)
            Spacer(Modifier.height(12.dp))

            // XP Progress bar
            Column(modifier = Modifier.fillMaxWidth(0.8f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Level ${user.level}", style = ActiveSparkTypography.labelSmall.copy(color = NeonCyan))
                    Text("${user.xp} XP", style = ActiveSparkTypography.labelSmall.copy(color = NeonCyan))
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (user.xp % 500) / 500f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = NeonCyan,
                    trackColor = DividerColor
                )
            }

            // Edit avatar button (own profile only)
            if (isOwnProfile) {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { navController.navigate(Screen.AvatarCustomization.route) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Face, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("EDIT AVATAR", style = ActiveSparkTypography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun RankBadge(rank: PlayerRank) {
    val color = when (rank) {
        PlayerRank.BRONZE -> RankBronze
        PlayerRank.SILVER -> RankSilver
        PlayerRank.GOLD -> RankGold
        PlayerRank.PLATINUM -> RankPlatinum
        PlayerRank.DIAMOND -> RankDiamond
        PlayerRank.MASTER -> RankMaster
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = "★ ${rank.displayName.uppercase()}",
            style = ActiveSparkTypography.labelMedium.copy(color = color),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun StatsGrid(user: User) {
    val stats = listOf(
        Triple("⚔️", "${user.totalBattles}", "Battles"),
        Triple("🏆", "${user.totalWins}", "Wins"),
        Triple("🔥", "${user.currentStreak}", "Streak"),
        Triple("⚡", "${user.totalActiveMinutes}m", "Active")
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        stats.forEach { (icon, value, label) ->
            StatCard(icon = icon, value = value, label = label, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(icon: String, value: String, label: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 24.sp)
            Text(value, style = ActiveSparkTypography.titleLarge.copy(color = NeonCyan))
            Text(label, style = ActiveSparkTypography.labelSmall.copy(color = TextTertiary))
        }
    }
}

@Composable
private fun BadgesSection(badges: List<String>) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text("BADGES 🎖️", style = ActiveSparkTypography.titleSmall.copy(color = TextTertiary))
        Spacer(Modifier.height(12.dp))
        if (badges.isEmpty()) {
            Text("Win battles to earn badges!", style = ActiveSparkTypography.bodyMedium.copy(color = TextDisabled))
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                badges.take(6).forEach { badge ->
                    Text(badge, fontSize = 32.sp)
                }
            }
        }
    }
}
