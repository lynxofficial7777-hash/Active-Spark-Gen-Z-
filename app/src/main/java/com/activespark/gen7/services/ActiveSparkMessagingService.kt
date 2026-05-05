package com.activespark.gen7.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.activespark.gen7.MainActivity
import com.activespark.gen7.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Firebase Cloud Messaging service for Active Spark Gen 7.
 * Handles incoming push notifications and FCM token refresh.
 *
 * Notification types supported:
 *   BATTLE_INVITE   → deep-links to Battle screen
 *   BATTLE_RESULT   → deep-links to Results screen
 *   FRIEND_REQUEST  → opens Home
 *   ACHIEVEMENT_UNLOCKED / RANK_UP → opens Profile
 */
class ActiveSparkMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID_BATTLES      = "active_spark_battles"
        const val CHANNEL_ID_SOCIAL       = "active_spark_social"
        const val CHANNEL_ID_ACHIEVEMENTS = "active_spark_achievements"

        const val EXTRA_MATCH_ID    = "matchId"
        const val EXTRA_NOTIF_TYPE  = "type"
    }

    /**
     * Called whenever Firebase issues a new token for this device.
     * We immediately save it to Realtime Database so the backend can reach this device.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase
            .getInstance("https://active-spark-gen7-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("users/$uid/fcmToken")
            .setValue(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title   = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Active Spark"
        val body    = remoteMessage.notification?.body  ?: remoteMessage.data["body"]  ?: ""
        val type    = remoteMessage.data[EXTRA_NOTIF_TYPE] ?: "SYSTEM"
        val matchId = remoteMessage.data[EXTRA_MATCH_ID]   ?: ""

        val channelId = when (type) {
            "BATTLE_INVITE", "BATTLE_RESULT" -> CHANNEL_ID_BATTLES
            "FRIEND_REQUEST"                 -> CHANNEL_ID_SOCIAL
            "ACHIEVEMENT_UNLOCKED", "RANK_UP"-> CHANNEL_ID_ACHIEVEMENTS
            else                             -> CHANNEL_ID_BATTLES
        }

        showNotification(title = title, body = body, channelId = channelId, matchId = matchId, type = type)
    }

    private fun showNotification(
        title: String,
        body: String,
        channelId: String,
        matchId: String,
        type: String
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels(notificationManager)

        // Build intent with deep-link extras so MainActivity can route to the right screen
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_MATCH_ID, matchId)
            putExtra(EXTRA_NOTIF_TYPE, type)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 250, 100, 250))
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannels(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannels(
                listOf(
                    NotificationChannel(CHANNEL_ID_BATTLES, "Battle Notifications",
                        NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Battle invites, results, and challenges"
                    },
                    NotificationChannel(CHANNEL_ID_SOCIAL, "Social Notifications",
                        NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "Friend requests and messages"
                    },
                    NotificationChannel(CHANNEL_ID_ACHIEVEMENTS, "Achievements",
                        NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "Badges, rank-ups, and achievements"
                    }
                )
            )
        }
    }
}
