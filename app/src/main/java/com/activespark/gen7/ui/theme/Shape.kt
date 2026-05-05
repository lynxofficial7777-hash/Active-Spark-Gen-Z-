package com.activespark.gen7.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape system for Active Spark Gen 7.
 * Uses aggressive rounded corners for a futuristic, pill-like aesthetic.
 */
val ActiveSparkShapes = Shapes(
    // Extra small: tags, chips, badge pills
    extraSmall = RoundedCornerShape(4.dp),
    // Small: input fields, small cards
    small = RoundedCornerShape(8.dp),
    // Medium: standard cards, dialogs
    medium = RoundedCornerShape(16.dp),
    // Large: bottom sheets, large panels
    large = RoundedCornerShape(24.dp),
    // Extra large: full-screen cards, hero elements
    extraLarge = RoundedCornerShape(32.dp)
)
