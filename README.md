# Media3 DefaultPreloadManager with LazyColumn Example

This project demonstrates how to use `DefaultPreloadManager` with Media3 1.7.1 in a Jetpack Compose LazyColumn for optimized video preloading and playback.

## Overview

The `DefaultPreloadManager` is a powerful component in Media3 that reduces video startup latency by preloading media content before it's needed. This is particularly useful for video feed applications like TikTok, Instagram Reels, or YouTube Shorts.

## Features

- ✅ **Intelligent Preloading**: Automatically preloads videos based on user scroll position
- ✅ **Priority-Based Loading**: Higher priority for visible and nearby videos
- ✅ **Memory Optimization**: Configurable preload ranges and durations
- ✅ **Bandwidth Management**: Preloading doesn't interfere with active playback
- ✅ **Real-time Status Tracking**: Visual indicators for preload progress
- ✅ **Seamless Playback**: Instant video start for preloaded content

## Project Structure

```
├── VideoListWithPreloading.kt         # Basic implementation
├── AdvancedVideoListWithPreloading.kt # Advanced implementation with status tracking
├── MainActivity.kt                    # Example usage
├── build.gradle.kts                   # Dependencies
└── README.md                          # This file
```

## Dependencies

Add these dependencies to your `app/build.gradle.kts`:

```kotlin
dependencies {
    // Media3 dependencies for version 1.7.1
    implementation("androidx.media3:media3-exoplayer:1.7.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.7.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.7.1")
    implementation("androidx.media3:media3-ui:1.7.1")
    implementation("androidx.media3:media3-common:1.7.1")
    implementation("androidx.media3:media3-datasource:1.7.1")
    
    // Compose dependencies
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
}
```

## Basic Usage

### 1. Basic Implementation

The basic implementation (`VideoListWithPreloading.kt`) provides:

```kotlin
// Initialize the preload manager
val preloadManagerBuilder = DefaultPreloadManager.Builder(context)
    .setTargetPreloadStatusControl(
        TargetPreloadStatusControl.PreloadStatusControl { targetPreloadStatus ->
            targetPreloadStatus.buildUpon()
                .setPreloadDurationUs(5_000_000) // 5 seconds
                .build()
        }
    )

val preloadManager = preloadManagerBuilder.build()
val player = preloadManagerBuilder.buildExoPlayer()
```

### 2. Add Videos to Preload Manager

```kotlin
videos.forEachIndexed { index, video ->
    val mediaItem = MediaItem.fromUri(video.url)
    val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)
    val priority = 100 - index // Higher priority for earlier videos
    
    preloadManager.add(mediaSource, priority)
}
```

### 3. Use in Compose

```kotlin
@Composable
fun VideoListScreen() {
    val viewModel: VideoListViewModel = viewModel()
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.initializePreloadManager(context)
    }
    
    LazyColumn {
        itemsIndexed(viewModel.videos) { index, video ->
            VideoItemCard(
                video = video,
                isPlaying = viewModel.currentPlayingIndex == index,
                onPlayClick = { viewModel.playVideo(index) },
                player = viewModel.player
            )
        }
    }
}
```

## Advanced Features

### 1. Priority-Based Preloading

The advanced implementation includes smart priority management:

```kotlin
val priority = when (distance) {
    1 -> 900 // Next/previous video - highest priority
    2 -> 800 // Two videos away - high priority
    else -> 700 // Further away - lower priority
}
```

### 2. Visible Range Optimization

```kotlin
LaunchedEffect(listState) {
    snapshotFlow {
        listState.layoutInfo.visibleItemsInfo.let { visibleItems ->
            val firstVisible = visibleItems.firstOrNull()?.index ?: 0
            val lastVisible = visibleItems.lastOrNull()?.index ?: 0
            firstVisible to lastVisible
        }
    }.collect { (first, last) ->
        viewModel.updateVisibleRange(first, last)
    }
}
```

### 3. Preload Status Tracking

```kotlin
data class PreloadStatus(
    val isPreloaded: Boolean = false,
    val preloadProgress: Float = 0f,
    val error: String? = null
)

// UI indicator
LinearProgressIndicator(
    progress = { preloadStatus.preloadProgress },
    color = if (preloadStatus.isPreloaded) 
        MaterialTheme.colorScheme.primary 
    else 
        MaterialTheme.colorScheme.secondary
)
```

## Configuration Options

### 1. Preload Duration

```kotlin
// High priority: 10 seconds
private val highPriorityPreloadDuration = 10_000_000L

// Low priority: 3 seconds  
private val lowPriorityPreloadDuration = 3_000_000L
```

### 2. Buffer Configuration

```kotlin
val loadControl = DefaultLoadControl.Builder()
    .setBufferDurationsMs(
        /* minBufferMs= */ 15000,
        /* maxBufferMs= */ 50000,
        /* bufferForPlaybackMs= */ 2500,
        /* bufferForPlaybackAfterRebufferMs= */ 5000
    )
    .build()
```

### 3. Preload Range

```kotlin
private val preloadRange = 3 // Number of videos to preload ahead and behind
```

## Best Practices

### 1. Memory Management

- Set appropriate preload durations based on video lengths
- Limit the number of simultaneously preloaded videos
- Monitor memory usage in your app

### 2. Network Optimization

- Use different preload durations based on network conditions
- Implement adaptive bitrate streaming
- Consider user's data plan (WiFi vs mobile)

### 3. User Experience

- Show preload status to users
- Provide smooth transitions between videos
- Handle errors gracefully

### 4. Performance Tips

```kotlin
// Use shared components for efficiency
val preloadManagerBuilder = DefaultPreloadManager.Builder(context)
    .setBandwidthMeter(sharedBandwidthMeter)
    .setTrackSelector(sharedTrackSelector)
    .setLoadControl(sharedLoadControl)
```

## Common Issues and Solutions

### 1. Context Access in ViewModel

❌ **Don't do this:**
```kotlin
// Using LocalContext.current in ViewModel
val dataSourceFactory = DefaultDataSource.Factory(LocalContext.current)
```

✅ **Do this:**
```kotlin
// Pass context as parameter
fun initializePreloadManager(context: Context) {
    val dataSourceFactory = DefaultDataSource.Factory(context)
}
```

### 2. Memory Leaks

❌ **Don't forget:**
```kotlin
override fun onCleared() {
    super.onCleared()
    // Always release resources
    currentPlayer?.release()
    preloadManager?.release()
}
```

### 3. Buffer Duration Coordination

✅ **Important:**
```kotlin
// Ensure preload duration >= buffer duration
val bufferForPlaybackMs = 2500
val preloadDurationUs = 5_000_000L // Must be >= bufferForPlaybackMs
```

## Migration from Earlier Versions

If you're upgrading from Media3 versions before 1.5.0:

### Old Way (Pre-1.5.0)
```kotlin
// Required manual component sharing
val renderersFactory = DefaultRenderersFactory(context)
val trackSelector = DefaultTrackSelector(context)
val loadControl = DefaultLoadControl()
val bandwidthMeter = DefaultBandwidthMeter.Builder(context).build()

val preloadManager = DefaultPreloadManager(
    renderersFactory,
    trackSelector,
    loadControl,
    bandwidthMeter,
    /* preloadLooper= */ Looper.getMainLooper()
)

val player = ExoPlayer.Builder(context)
    .setRenderersFactory(renderersFactory)
    .setTrackSelector(trackSelector)
    .setLoadControl(loadControl)
    .setBandwidthMeter(bandwidthMeter)
    .build()
```

### New Way (1.5.0+)
```kotlin
// Simplified with builder
val preloadManagerBuilder = DefaultPreloadManager.Builder(context)
val preloadManager = preloadManagerBuilder.build()
val player = preloadManagerBuilder.buildExoPlayer()
```

## Testing

To test the preloading functionality:

1. **Network Speed**: Test on different network speeds (3G, 4G, WiFi)
2. **Memory Usage**: Monitor memory consumption during scrolling
3. **Battery Impact**: Check battery usage with preloading enabled/disabled
4. **Error Handling**: Test with broken video URLs
5. **Performance**: Measure startup latency with and without preloading

## Example Usage

To use the examples in your app:

1. Copy the desired implementation file to your project
2. Add the required dependencies
3. Update `MainActivity.kt` to use your chosen screen:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // Choose one:
                VideoListScreen()              // Basic implementation
                // or
                AdvancedVideoListScreen()      // Advanced implementation
            }
        }
    }
}
```

## Additional Resources

- [Media3 Documentation](https://developer.android.com/media/media3)
- [ExoPlayer Blog](https://medium.com/google-exoplayer)
- [Media3 GitHub Repository](https://github.com/androidx/media)
- [Preloading Media Guide](https://proandroiddev.com/preloading-media-a-future-forward-approach-with-exoplayer-877ca6b0873d)

## License

This project is provided as educational material. Please follow your project's license requirements when using this code.