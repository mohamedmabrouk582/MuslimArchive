# News Feed with Auto-Play and Mixed Content Support

A comprehensive news feed implementation for Android using Media3 1.7.1 that supports mixed content types (video, audio, image news, text) with Facebook/LinkedIn-style auto-play behavior.

## Overview

This implementation provides a complete news feed solution with:
- **Mixed Content Support**: Videos, audio, image news, and text articles
- **Auto-Play Behavior**: Smart auto-play similar to Facebook and LinkedIn
- **Intelligent Preloading**: Optimized preloading based on visibility and content type
- **Social Interactions**: Like, share, and comment functionality
- **Performance Optimized**: Efficient memory and bandwidth management

## Features

### 🎯 **Auto-Play Behavior**
- ✅ Only one video/audio plays at a time (like Facebook/LinkedIn)
- ✅ Auto-play when 60% of content is visible
- ✅ Videos start muted by default
- ✅ Pause when content is no longer visible
- ✅ Smart network-aware auto-play (WiFi vs mobile data)
- ✅ Video takes priority over audio when both are visible

### 📱 **Content Types**
- ✅ **Video Content**: Auto-playing videos with controls
- ✅ **Audio Content**: Podcasts, music with playback controls
- ✅ **Image News**: News articles with image carousels
- ✅ **Text News**: Text-based articles with previews

### 🚀 **Performance Features**
- ✅ Intelligent preloading based on scroll position
- ✅ Visibility percentage tracking
- ✅ Memory-efficient buffering
- ✅ Bandwidth-aware configurations
- ✅ Real-time status tracking

## Project Structure

```
├── NewsFeedPreloadingHelper.kt         # Core preloading logic
├── NewsFeedWithAutoPlay.kt            # Main feed implementation
├── NewsFeedCardComponents.kt          # UI components for different content types
├── PreloadingHelper.kt                # Generic preloading helper (base)
└── NewsFeed_README.md                 # This file
```

## Quick Start

### 1. Add Dependencies

```kotlin
// app/build.gradle.kts
dependencies {
    // Media3 for version 1.7.1
    implementation("androidx.media3:media3-exoplayer:1.7.1")
    implementation("androidx.media3:media3-ui:1.7.1")
    implementation("androidx.media3:media3-common:1.7.1")
    implementation("androidx.media3:media3-datasource:1.7.1")
    
    // Compose
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    
    // Image loading
    implementation("io.coil-kt:coil-compose:2.4.0")
}
```

### 2. Basic Usage

```kotlin
// In your MainActivity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                NewsFeedScreen()
            }
        }
    }
}
```

### 3. Custom Configuration

```kotlin
// Configure auto-play behavior
val autoPlayConfig = AutoPlayConfig(
    enableAutoPlay = true,
    autoPlayOnWifi = true,
    autoPlayOnMobile = false, // Conservative for mobile data
    minVisibilityPercentage = 0.6f, // 60% visibility like Facebook
    autoMuteVideos = true, // Start muted
    pauseWhenNotVisible = true,
    maxConcurrentPlayers = 1, // Only one video at a time
    prioritizeVideoOverAudio = true
)

val config = NewsFeedPreloadConfig(
    videoPreloadDurationUs = 10_000_000L, // 10 seconds
    audioPreloadDurationUs = 20_000_000L, // 20 seconds
    maxPreloadItems = 6,
    preloadRange = 3,
    autoPlayConfig = autoPlayConfig
)
```

## Content Types Implementation

### Video Content

```kotlin
data class VideoFeedItem(
    override val id: String,
    override val url: String,
    override val title: String,
    override val description: String?,
    override val authorName: String?,
    override val publishTime: Long,
    override val duration: Long?,
    override val thumbnailUrl: String?,
    val likes: Int = 0,
    val comments: Int = 0,
    val shares: Int = 0,
    override val priority: Int = 80
) : NewsFeedItem {
    override val contentType = FeedContentType.VIDEO
}
```

### Audio Content

```kotlin
data class AudioFeedItem(
    override val id: String,
    override val url: String,
    override val title: String,
    override val description: String?,
    override val authorName: String?,
    override val publishTime: Long,
    override val duration: Long?,
    override val thumbnailUrl: String?,
    val podcastName: String? = null,
    val likes: Int = 0,
    val comments: Int = 0,
    override val priority: Int = 70
) : NewsFeedItem {
    override val contentType = FeedContentType.AUDIO
}
```

### Image News

```kotlin
data class ImageNewsFeedItem(
    override val id: String,
    override val title: String,
    override val description: String?,
    override val authorName: String?,
    override val publishTime: Long,
    override val thumbnailUrl: String?,
    val imageUrls: List<String> = emptyList(),
    val newsCategory: String? = null,
    val readTime: String? = null,
    val likes: Int = 0,
    val comments: Int = 0,
    val shares: Int = 0,
    override val priority: Int = 60
) : NewsFeedItem {
    override val contentType = FeedContentType.IMAGE_NEWS
}
```

### Text News

```kotlin
data class TextNewsFeedItem(
    override val id: String,
    override val title: String,
    override val description: String?,
    override val authorName: String?,
    override val publishTime: Long,
    val content: String,
    val newsCategory: String? = null,
    val readTime: String? = null,
    val likes: Int = 0,
    val comments: Int = 0,
    val shares: Int = 0,
    override val priority: Int = 50
) : NewsFeedItem {
    override val contentType = FeedContentType.TEXT_ONLY
}
```

## Auto-Play Configuration

### Facebook-Style Auto-Play

```kotlin
val facebookStyleConfig = AutoPlayConfig(
    enableAutoPlay = true,
    autoPlayOnWifi = true,
    autoPlayOnMobile = false,
    minVisibilityPercentage = 0.6f, // 60% visible
    autoMuteVideos = true, // Always start muted
    pauseWhenNotVisible = true,
    continuePlayingWhenScrolling = false,
    maxConcurrentPlayers = 1, // Only one video
    prioritizeVideoOverAudio = true
)
```

### LinkedIn-Style Auto-Play

```kotlin
val linkedInStyleConfig = AutoPlayConfig(
    enableAutoPlay = true,
    autoPlayOnWifi = true,
    autoPlayOnMobile = true, // LinkedIn is more aggressive
    minVisibilityPercentage = 0.5f, // 50% visible
    autoMuteVideos = true,
    pauseWhenNotVisible = true,
    continuePlayingWhenScrolling = true, // Continue during slow scroll
    maxConcurrentPlayers = 1,
    prioritizeVideoOverAudio = true
)
```

### Conservative Auto-Play

```kotlin
val conservativeConfig = AutoPlayConfig(
    enableAutoPlay = true,
    autoPlayOnWifi = true,
    autoPlayOnMobile = false,
    minVisibilityPercentage = 0.8f, // 80% visible - more conservative
    autoMuteVideos = true,
    pauseWhenNotVisible = true,
    continuePlayingWhenScrolling = false,
    maxConcurrentPlayers = 1,
    prioritizeVideoOverAudio = true
)
```

## Advanced Features

### Visibility Tracking

The implementation automatically tracks visibility percentage for each item:

```kotlin
// Automatic visibility calculation
LaunchedEffect(listState) {
    snapshotFlow {
        listState.layoutInfo.visibleItemsInfo.map { itemInfo ->
            val visibilityPercentage = calculateVisibilityPercentage(itemInfo)
            item.id to visibilityPercentage
        }
    }.collect { visibilityMap ->
        visibilityMap.forEach { (itemId, visibility) ->
            viewModel.updateItemVisibility(itemId, visibility)
        }
    }
}
```

### Smart Preloading

```kotlin
// Preload based on visible range and content type
fun updateForFeedPosition(
    currentVisibleItems: List<Pair<Int, NewsFeedItem>>,
    allItems: List<NewsFeedItem>
) {
    // Calculate preload range around visible items
    val visibleIndices = currentVisibleItems.map { it.first }
    val minVisible = visibleIndices.minOrNull() ?: 0
    val maxVisible = visibleIndices.maxOrNull() ?: 0
    
    val startIndex = maxOf(0, minVisible - config.preloadRange)
    val endIndex = minOf(allItems.size - 1, maxVisible + config.preloadRange)
    
    // Preload items in range with priority
    val itemsToPreload = (startIndex..endIndex)
        .mapNotNull { index -> allItems.getOrNull(index) }
        .filter { it.canAutoPlay }
        .take(config.maxPreloadItems)
}
```

### Multiple Player Management

```kotlin
// Handle video and audio separately
private fun handleVideoAutoPlay(item: NewsFeedItem) {
    // Only one video plays at a time (Facebook/LinkedIn behavior)
    if (currentPlayingVideoId != null && currentPlayingVideoId != item.id) {
        pauseCurrentVideo()
    }
    
    // If audio is playing and we prioritize video, pause audio
    if (config.autoPlayConfig.prioritizeVideoOverAudio && currentPlayingAudioId != null) {
        pauseCurrentAudio()
    }
    
    playItem(item, primaryPlayer, muted = config.autoPlayConfig.autoMuteVideos)
    currentPlayingVideoId = item.id
}

private fun handleAudioAutoPlay(item: NewsFeedItem) {
    // Only play audio if no video is currently playing (if video has priority)
    if (config.autoPlayConfig.prioritizeVideoOverAudio && currentPlayingVideoId != null) {
        return
    }
    
    val player = secondaryPlayer ?: primaryPlayer
    playItem(item, player, muted = false)
    currentPlayingAudioId = item.id
}
```

## UI Components

### Video Card with Auto-Play

The video card automatically handles:
- Thumbnail display when not playing
- Video player when auto-playing
- Mute/unmute controls
- Visibility percentage tracking
- Play/pause controls

### Audio Card with Playback

The audio card provides:
- Audio thumbnail/artwork
- Playback progress
- Play/pause controls
- Podcast information
- Duration display

### Image News Card

Features include:
- Main image display
- Image carousel for multiple images
- News category chips
- Read time estimation
- Social interaction buttons

### Text News Card

Provides:
- Article preview
- "Read more" functionality
- Category classification
- Social engagement metrics

## Performance Optimization

### Memory Management

```kotlin
// Automatic cleanup of distant items
val itemsToRemove = preloadedItems.keys.filter { itemId ->
    val itemIndex = allItems.indexOfFirst { it.id == itemId }
    itemIndex < startIndex || itemIndex > endIndex
}

itemsToRemove.forEach { removeItem(it) }
```

### Network Optimization

```kotlin
// Different configurations based on network
private fun shouldAutoPlay(): Boolean {
    return config.autoPlayConfig.enableAutoPlay &&
            (isOnWifi() && config.autoPlayConfig.autoPlayOnWifi ||
             !isOnWifi() && config.autoPlayConfig.autoPlayOnMobile)
}
```

### Battery Optimization

- Videos start muted to reduce CPU usage
- Intelligent buffering based on content type
- Pause when not visible to save battery
- Smart preload duration based on network conditions

## Best Practices

### 1. Content Prioritization

```kotlin
// Set different priorities for different content types
data class VideoFeedItem(
    override val priority: Int = 80 // High priority for videos
) : NewsFeedItem

data class AudioFeedItem(
    override val priority: Int = 70 // Medium priority for audio
) : NewsFeedItem

data class ImageNewsFeedItem(
    override val priority: Int = 60 // Lower priority for image news
) : NewsFeedItem
```

### 2. Network Awareness

```kotlin
// Adjust preload duration based on network
val config = if (isOnWifi()) {
    NewsFeedPreloadConfig(
        videoPreloadDurationUs = 15_000_000L, // 15 seconds on WiFi
        audioPreloadDurationUs = 30_000_000L  // 30 seconds on WiFi
    )
} else {
    NewsFeedPreloadConfig(
        videoPreloadDurationUs = 5_000_000L,  // 5 seconds on mobile
        audioPreloadDurationUs = 10_000_000L  // 10 seconds on mobile
    )
}
```

### 3. User Experience

```kotlin
// Show loading states
if (preloadStatus?.isLoading == true) {
    LinearProgressIndicator(
        progress = { preloadStatus.progress },
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondary
    )
}

// Visual feedback for playing state
colors = CardDefaults.cardColors(
    containerColor = if (playbackStatus?.isPlaying == true) 
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    else 
        MaterialTheme.colorScheme.surface
)
```

## Testing

### Unit Testing

```kotlin
@Test
fun `auto-play behavior follows Facebook rules`() {
    val helper = createNewsFeedHelper(facebookStyleConfig)
    val videoItem = createVideoItem(id = "video1")
    
    // Video should not auto-play when less than 60% visible
    helper.updateItemVisibility("video1", 0.5f, videoItem)
    assertFalse(helper.getCurrentlyPlayingItems().contains("video1"))
    
    // Video should auto-play when 60% or more visible
    helper.updateItemVisibility("video1", 0.6f, videoItem)
    assertTrue(helper.getCurrentlyPlayingItems().contains("video1"))
}

@Test
fun `only one video plays at a time`() {
    val helper = createNewsFeedHelper()
    val video1 = createVideoItem(id = "video1")
    val video2 = createVideoItem(id = "video2")
    
    helper.updateItemVisibility("video1", 0.8f, video1)
    helper.updateItemVisibility("video2", 0.8f, video2)
    
    val playingItems = helper.getCurrentlyPlayingItems()
    assertEquals(1, playingItems.size)
}
```

### Integration Testing

```kotlin
@Test
fun `feed handles mixed content correctly`() = runTest {
    val viewModel = NewsFeedViewModel()
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    
    viewModel.initializeFeedHelper(context)
    
    // Test auto-play with mixed content
    viewModel.updateItemVisibility("video1", 0.7f)
    viewModel.updateItemVisibility("audio1", 0.8f)
    
    // Video should take priority over audio
    val playbackStatuses = viewModel.playbackStatuses?.value
    assertTrue(playbackStatuses?.get("video1")?.isPlaying == true)
    assertFalse(playbackStatuses?.get("audio1")?.isPlaying == true)
}
```

## Troubleshooting

### Common Issues

1. **Videos not auto-playing**
   - Check network conditions (WiFi vs mobile settings)
   - Verify visibility percentage meets threshold
   - Ensure content is properly preloaded

2. **Multiple videos playing simultaneously**
   - Check `maxConcurrentPlayers` configuration
   - Verify player management logic
   - Check for memory leaks in player instances

3. **High memory usage**
   - Reduce `maxPreloadItems`
   - Lower `preloadDurationUs` values
   - Implement proper cleanup in `onCleared()`

4. **Poor scroll performance**
   - Optimize visibility calculations
   - Reduce preload range
   - Use proper keys in LazyColumn

### Debug Tools

```kotlin
// Add debug logging
val listener = object : PreloadListener {
    override fun onPreloadStarted(itemId: String) {
        Log.d("NewsFeed", "Preload started: $itemId")
    }
    
    override fun onPreloadCompleted(itemId: String) {
        Log.d("NewsFeed", "Preload completed: $itemId")
    }
    
    override fun onPreloadError(itemId: String, error: String) {
        Log.e("NewsFeed", "Preload error: $itemId - $error")
    }
}

// Monitor visibility
playbackStatus?.let { status ->
    if (status.visibilityPercentage > 0f) {
        LinearProgressIndicator(
            progress = { status.visibilityPercentage },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
    }
}
```

## Future Enhancements

- [ ] **Live Streaming Support**: Add support for live video streams
- [ ] **Picture-in-Picture**: Implement PiP mode for videos
- [ ] **Offline Support**: Cache content for offline viewing
- [ ] **Analytics Integration**: Add detailed analytics tracking
- [ ] **Adaptive Streaming**: Implement quality adaptation based on network
- [ ] **Voice Control**: Add voice commands for playback control

## License

This implementation is provided as educational material for Media3 integration. Follow your project's license requirements when using this code.

---

This news feed implementation provides a production-ready solution for apps requiring Facebook/LinkedIn-style auto-play behavior with mixed content support. The modular design allows for easy customization and extension based on specific requirements.