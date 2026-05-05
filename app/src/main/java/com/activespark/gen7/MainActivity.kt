package com.activespark.gen7

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.activespark.gen7.navigation.AppNavGraph
import com.activespark.gen7.services.ActiveSparkMessagingService
import com.activespark.gen7.ui.theme.ActiveSparkTheme
import com.activespark.gen7.ui.theme.Background
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity — single entry point for the app.
 *
 * Deep-link routing: when a notification is tapped, the FCM service puts
 * EXTRA_MATCH_ID and EXTRA_NOTIF_TYPE into the intent. We pass those to
 * AppNavGraph, which navigates to the appropriate screen after Splash.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ActiveSparkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Background
                ) {
                    AppNavGraph(
                        deepLinkMatchId = intent.getStringExtra(ActiveSparkMessagingService.EXTRA_MATCH_ID),
                        deepLinkType    = intent.getStringExtra(ActiveSparkMessagingService.EXTRA_NOTIF_TYPE)
                    )
                }
            }
        }
    }

    /** Called when a new notification tap arrives while the app is already open. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)   // Updates the intent so Compose recomposition picks it up
    }
}
