package com.activespark.gen7.data.models

/**
 * Parental control settings for a child account.
 * Stored in Realtime Database under /parental_settings/{childUid}
 *
 * Read by the app before allowing certain actions (online battles, chat, etc.)
 * Written only by the parent from the ParentDashboardScreen.
 */
data class ParentalSettings(
    val childUid: String = "",
    val dailyLimitHours: Int = 2,
    val allowOnlineBattles: Boolean = true,
    val allowChat: Boolean = false,
    val allowNotifications: Boolean = true,
    val lastUpdated: Long = 0L
)
