// SimpleUsageExample.kt
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Simple usage examples for Media3 1.7.1 preloading
 */

// Example 1: Basic preloading without auto-play
@Composable
fun BasicPreloadExample() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Basic Preloading Example",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            SimpleFeedScreen()
        }
    }
}

// Example 2: Auto-play with visibility detection
@Composable
fun AutoPlayExample() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Auto-Play Example",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Videos auto-play when 60% visible",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            SimpleAutoPlayFeedScreen()
        }
    }
}

// Main activity usage example
@Composable
fun SimplePreloadingApp() {
    var selectedExample by remember { mutableIntStateOf(0) }
    
    MaterialTheme {
        Column {
            // Tab row to switch between examples
            TabRow(selectedTabIndex = selectedExample) {
                Tab(
                    selected = selectedExample == 0,
                    onClick = { selectedExample = 0 },
                    text = { Text("Basic") }
                )
                Tab(
                    selected = selectedExample == 1,
                    onClick = { selectedExample = 1 },
                    text = { Text("Auto-Play") }
                )
            }
            
            // Content
            when (selectedExample) {
                0 -> BasicPreloadExample()
                1 -> AutoPlayExample()
            }
        }
    }
}

/**
 * Minimal usage for just preloading functionality:
 * 
 * val preloadManager = SimplePreloadManager(context)
 * preloadManager.initialize()
 * 
 * val items = listOf(
 *     MediaContent("1", "video_url_1"),
 *     MediaContent("2", "video_url_2")
 * )
 * 
 * preloadManager.preloadItems(items)
 * 
 * // To play an item:
 * preloadManager.playItem(items[0])
 * 
 * // To get the player:
 * val player = preloadManager.getPlayer()
 * 
 * // Don't forget to release:
 * preloadManager.release()
 */

/**
 * Minimal usage for auto-play functionality:
 * 
 * val autoPlayManager = SimpleAutoPlayManager(context)
 * autoPlayManager.initialize()
 * 
 * // In your scroll listener or LazyColumn:
 * autoPlayManager.updateVisibility(item, visibilityPercentage)
 * 
 * // To toggle mute:
 * autoPlayManager.toggleMute()
 * 
 * // To check what's auto-playing:
 * val currentId = autoPlayManager.getCurrentAutoPlayingId()
 */