package com.activespark.gen7.ui.screens.avatar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.activespark.gen7.data.models.AvatarConfig
import com.activespark.gen7.ui.theme.*

@Composable
fun AvatarCustomizationScreen(
    navController: NavController,
    viewModel: AvatarCustomizationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "YOUR AVATAR",
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
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = NeonCyan) }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Live preview ─────────────────────────────────────────────────
            AvatarPreview(config = uiState.config)

            Spacer(Modifier.height(28.dp))

            // ── Character ────────────────────────────────────────────────────
            SelectorSection(
                title = "CHARACTER",
                options = AVATAR_CHARACTERS.entries.map { (key, emoji) ->
                    SelectionOption(key = key, display = emoji, label = null)
                },
                selected = uiState.config.bodyType,
                onSelect = viewModel::selectCharacter
            )

            Spacer(Modifier.height(20.dp))

            // ── Color theme ──────────────────────────────────────────────────
            ColorSelectorSection(
                selected = uiState.config.hairColor,
                onSelect = viewModel::selectColorTheme
            )

            Spacer(Modifier.height(20.dp))

            // ── Outfit ───────────────────────────────────────────────────────
            SelectorSection(
                title = "OUTFIT",
                options = AVATAR_OUTFITS.entries.map { (key, label) ->
                    SelectionOption(key = key, display = label, label = null)
                },
                selected = uiState.config.outfit,
                onSelect = viewModel::selectOutfit
            )

            Spacer(Modifier.height(20.dp))

            // ── Accessory ────────────────────────────────────────────────────
            SelectorSection(
                title = "ACCESSORY",
                options = AVATAR_ACCESSORIES.entries.map { (key, emoji) ->
                    SelectionOption(key = key, display = emoji, label = key.replaceFirstChar { it.uppercase() })
                },
                selected = uiState.config.accessory,
                onSelect = viewModel::selectAccessory
            )

            Spacer(Modifier.height(32.dp))

            // ── Error banner ─────────────────────────────────────────────────
            AnimatedVisibility(visible = uiState.saveError != null) {
                uiState.saveError?.let {
                    Surface(
                        color = NeonPink.copy(0.12f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "⚠️ $it",
                            style = ActiveSparkTypography.bodySmall.copy(color = NeonPink),
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // ── Save button ──────────────────────────────────────────────────
            Button(
                onClick = viewModel::saveAvatar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isSaving && !uiState.isSaved,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isSaved) NeonGreen else NeonCyan,
                    disabledContainerColor = if (uiState.isSaved) NeonGreen.copy(0.5f) else NeonCyan.copy(0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Background,
                        strokeWidth = 2.dp
                    )
                } else if (uiState.isSaved) {
                    Icon(Icons.Default.Check, null, tint = Background)
                    Spacer(Modifier.width(8.dp))
                    Text("SAVED!", color = Background, style = ActiveSparkTypography.labelLarge)
                } else {
                    Text(
                        "SAVE AVATAR",
                        color = Background,
                        style = ActiveSparkTypography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Avatar Preview ───────────────────────────────────────────────────────────

@Composable
fun AvatarPreview(config: AvatarConfig, size: Int = 140) {
    val themeColor = avatarThemeColor(config.hairColor)
    val pulse = rememberInfiniteTransition(label = "avatar_pulse")
    val glowScale by pulse.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "glow"
    )

    Box(contentAlignment = Alignment.TopCenter) {
        // Outer glow ring
        Box(
            modifier = Modifier
                .size((size + 24).dp)
                .scale(glowScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(themeColor.copy(0.25f), Color.Transparent)
                    )
                )
        )
        // Avatar circle
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(themeColor.copy(0.35f), SurfaceVariant)
                    )
                )
                .border(2.dp, themeColor.copy(0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Character emoji
            Text(
                text = AVATAR_CHARACTERS[config.bodyType] ?: "⚡",
                fontSize = (size * 0.45f).sp
            )
        }
        // Accessory badge in top-right corner
        if (config.accessory != "none") {
            val accessEmoji = AVATAR_ACCESSORIES[config.accessory] ?: ""
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SurfaceElevated)
                    .border(1.5.dp, themeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(accessEmoji, fontSize = 18.sp)
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    // Outfit label pill
    val outfitLabel = AVATAR_OUTFITS[config.outfit] ?: config.outfit
    Surface(
        color = themeColor.copy(0.12f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            outfitLabel,
            style = ActiveSparkTypography.labelMedium.copy(color = themeColor),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}

// ─── Color theme selector ─────────────────────────────────────────────────────

@Composable
private fun ColorSelectorSection(selected: String, onSelect: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "COLOR THEME",
            style = ActiveSparkTypography.labelSmall.copy(
                color = TextTertiary,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AVATAR_COLORS.entries.forEach { (key, hex) ->
                val color = colorFromHexString(hex)
                val isSelected = selected == hex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(color.copy(if (isSelected) 1f else 0.35f))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) Color.White else color.copy(0.4f),
                            shape = CircleShape
                        )
                        .clickable { onSelect(key) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Generic selector section ─────────────────────────────────────────────────

data class SelectionOption(val key: String, val display: String, val label: String?)

@Composable
private fun SelectorSection(
    title: String,
    options: List<SelectionOption>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            style = ActiveSparkTypography.labelSmall.copy(
                color = TextTertiary,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(Modifier.height(10.dp))
        // Wrap into rows of 4
        val chunked = options.chunked(4)
        chunked.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { option ->
                    OptionTile(
                        option = option,
                        isSelected = option.key == selected,
                        onSelect = { onSelect(option.key) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill empty cells to maintain row layout
                repeat(4 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
            if (row != chunked.last()) Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OptionTile(
    option: SelectionOption,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tile_scale"
    )
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale)
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) NeonCyan.copy(0.15f) else SurfaceElevated
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan)
        else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = option.display,
                    fontSize = if (option.label == null) 28.sp else 20.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                if (option.label != null && option.label != "None") {
                    Text(
                        text = option.label,
                        style = ActiveSparkTypography.labelSmall.copy(
                            color = if (isSelected) NeonCyan else TextTertiary,
                            fontSize = 9.sp
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

/** Returns the Compose Color for the hairColor hex string stored in AvatarConfig. */
fun avatarThemeColor(hairColorHex: String): Color {
    return when (hairColorHex) {
        "0xFF00F5FF" -> NeonCyan
        "0xFFFF1B8D" -> NeonPink
        "0xFF39FF14" -> NeonGreen
        "0xFFBF00FF" -> NeonPurple
        "0xFFFF6B00" -> NeonOrange
        "0xFFFFE600" -> NeonYellow
        else         -> NeonCyan   // fallback
    }
}

private fun colorFromHexString(hex: String): Color {
    // Input format: "0xFFRRGGBB"  e.g. "0xFF00F5FF"
    return try {
        val cleaned = hex.removePrefix("0x").removePrefix("0X")
        Color(cleaned.toLong(16))
    } catch (e: Exception) {
        NeonCyan
    }
}
