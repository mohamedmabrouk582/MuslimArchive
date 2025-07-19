# Simple Media3 Preloading for 1.7.1

A simple but effective preloading solution for Media3 1.7.1 that handles video preloading and auto-play with minimal complexity.

## What You Get

- ✅ **Simple Preloading**: Just add videos and they preload automatically
- ✅ **Auto-Play**: Videos auto-play when 60% visible (like social media)
- ✅ **Easy Integration**: Just a few lines of code to get started
- ✅ **Lightweight**: No complex configurations or interfaces

## Quick Start

### 1. Add Dependencies

```kotlin
// build.gradle.kts
implementation("androidx.media3:media3-exoplayer:1.7.1")
implementation("androidx.media3:media3-ui:1.7.1")
implementation("androidx.media3:media3-common:1.7.1")
```

### 2. Basic Preloading (No Auto-Play)

```kotlin
// Create manager
val preloadManager = SimplePreloadManager(context)
preloadManager.initialize()

// Add videos to preload
val videos = listOf(
    MediaContent("1", "video_url_1"),
    MediaContent("2", "video_url_2")
)
preloadManager.preloadItems(videos)

// Play a video
preloadManager.playItem(videos[0])

// Get player for UI
val player = preloadManager.getPlayer()

// Clean up
preloadManager.release()
```

### 3. Auto-Play (Facebook/LinkedIn Style)

```kotlin
// Create auto-play manager
val autoPlayManager = SimpleAutoPlayManager(context)
autoPlayManager.initialize()

// In your scroll listener:
autoPlayManager.updateVisibility(video, visibilityPercentage)

// Toggle mute
autoPlayManager.toggleMute()
```

### 4. Complete LazyColumn Example

```kotlin
@Composable
fun VideoFeed() {
    SimpleFeedScreen() // That's it!
}

// Or with auto-play:
@Composable
fun AutoPlayVideoFeed() {
    SimpleAutoPlayFeedScreen() // Done!
}
```

## Files Overview

| File | Purpose |
|------|---------|
| `SimplePreloadManager.kt` | Core preloading logic |
| `SimpleFeedScreen.kt` | Basic video list with preloading |
| `SimpleAutoPlayManager.kt` | Auto-play functionality |
| `SimpleUsageExample.kt` | Usage examples |

## Core Classes

### SimplePreloadManager
```kotlin
class SimplePreloadManager(context: Context) {
    fun initialize()
    fun preloadItems(items: List<MediaContent>)
    fun playItem(item: MediaContent)
    fun getPlayer(): ExoPlayer?
    fun release()
}
```

### SimpleAutoPlayManager
```kotlin
class SimpleAutoPlayManager(context: Context) : SimplePreloadManager(context) {
    fun updateVisibility(item: MediaContent, visibilityPercentage: Float)
    fun toggleMute()
    fun getCurrentAutoPlayingId(): String?
}
```

### MediaContent
```kotlin
data class MediaContent(
    val id: String,
    val url: String,
    val isVideo: Boolean = true
)
```

## Auto-Play Behavior

- ✅ Videos auto-play when **60% visible**
- ✅ Only **one video** plays at a time
- ✅ Videos start **muted**
- ✅ Auto-pause when scrolled away
- ✅ Manual mute/unmute toggle

## Preloading Features

- ✅ Preloads **5 seconds** of video content
- ✅ Tracks preload status with StateFlow
- ✅ Automatic preloading around visible items
- ✅ Memory efficient cleanup

## Integration Examples

### In Activity
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SimpleAutoPlayFeedScreen()
        }
    }
}
```

### Custom Implementation
```kotlin
@Composable
fun MyVideoFeed() {
    val viewModel: SimpleAutoPlayViewModel = viewModel()
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }
    
    // Your LazyColumn here...
}
```

### Manual Control
```kotlin
// In your ViewModel
private val preloadManager = SimplePreloadManager(context)

fun onViewCreated() {
    preloadManager.initialize()
    preloadManager.preloadItems(myVideos)
}

fun onDestroy() {
    preloadManager.release()
}
```

## Customization

### Change Auto-Play Threshold
```kotlin
// In SimpleAutoPlayManager.kt, line ~35:
val shouldAutoPlay = visibilityPercentage >= 0.6f // Change to 0.8f for 80%
```

### Change Preload Duration
```kotlin
// In SimplePreloadManager.kt, line ~52:
.setPreloadDurationUs(5_000_000L) // Change to 10_000_000L for 10 seconds
```

### Add Network Awareness
```kotlin
fun updateVisibility(item: MediaContent, visibilityPercentage: Float) {
    val shouldAutoPlay = visibilityPercentage >= 0.6f && 
                        autoPlayEnabled && 
                        isOnWifi() // Add your network check
    // ...
}
```

## Common Use Cases

### 1. Simple Video List
```kotlin
setContent {
    SimpleFeedScreen()
}
```

### 2. Auto-Playing Feed
```kotlin
setContent {
    SimpleAutoPlayFeedScreen()
}
```

### 3. Custom Integration
```kotlin
val preloadManager = SimplePreloadManager(context)
preloadManager.initialize()

// Your videos
val videos = getMyVideos()
preloadManager.preloadItems(videos)

// In RecyclerView/LazyColumn
onItemVisible { index ->
    if (index in visibleRange) {
        preloadManager.preloadItems(videosAroundIndex)
    }
}
```

## Troubleshooting

### Videos Not Preloading
- Check internet connection
- Verify video URLs are accessible
- Call `initialize()` before `preloadItems()`

### Auto-Play Not Working
- Ensure visibility percentage calculation is correct
- Check that videos are preloaded first
- Verify auto-play is enabled

### Memory Issues
- Call `release()` in onDestroy/onCleared
- Reduce preload duration
- Limit number of preloaded items

## Advantages

- **Simple**: Just a few classes and methods
- **Effective**: Uses Media3 1.7.1 DefaultPreloadManager properly
- **Lightweight**: Minimal overhead
- **Ready to Use**: Complete examples included
- **Customizable**: Easy to modify for your needs

This solution focuses on simplicity while providing the core functionality you need for video preloading and auto-play in Media3 1.7.1.