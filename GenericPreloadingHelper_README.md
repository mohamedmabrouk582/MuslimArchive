# Generic PreloadingHelper for Media3

A reusable, type-safe preloading helper for Media3 that can be used with any media content (videos, audio, podcasts, etc.) in Android applications.

## Overview

The `PreloadingHelper` is a generic solution that abstracts the complexity of Media3's `DefaultPreloadManager` into a simple, reusable component. It provides intelligent preloading, priority management, and status tracking for any media content.

## Key Features

- ✅ **Generic Design**: Works with any media type through the `PreloadableItem` interface
- ✅ **Intelligent Preloading**: Automatic priority-based preloading based on position and visibility
- ✅ **Real-time Status Tracking**: Live updates on preload progress and status
- ✅ **Configurable**: Extensive configuration options for different use cases
- ✅ **Memory Efficient**: Smart memory management with configurable limits
- ✅ **LazyList Optimized**: Built-in support for LazyColumn/LazyRow scenarios
- ✅ **Easy Integration**: Simple builder pattern and extension functions
- ✅ **Statistics**: Built-in analytics for preloading performance

## Basic Usage

### 1. Define Your Media Item

```kotlin
data class Video(
    override val id: String,
    val title: String,
    override val url: String,
    val duration: Long = 0L,
    override val priority: Int = 50
) : PreloadableItem
```

### 2. Create and Configure the Helper

```kotlin
// Simple creation
val preloadingHelper = context.createPreloadingHelper()

// Advanced configuration
val config = PreloadConfig(
    preloadDurationUs = 8_000_000L, // 8 seconds
    maxPreloadItems = 5,
    preloadRange = 2,
    minBufferMs = 10000,
    maxBufferMs = 30000
)

val listener = object : PreloadListener {
    override fun onPreloadCompleted(itemId: String) {
        println("Preload completed: $itemId")
    }
}

val preloadingHelper = context.createPreloadingHelper(config, listener)
```

### 3. Use in Your ViewModel

```kotlin
class VideoListViewModel : ViewModel() {
    private var preloadingHelper: PreloadingHelper? = null
    
    fun initializePreloading(context: Context) {
        preloadingHelper = context.createPreloadingHelper()
        
        // Add items to preload
        preloadingHelper?.addAll(videos.take(5))
    }
    
    fun playVideo(index: Int) {
        val video = videos[index]
        preloadingHelper?.playItem(video)
    }
    
    fun updateVisibleRange(firstVisible: Int, lastVisible: Int) {
        preloadingHelper?.updateForLazyList(
            currentPlayingIndex = currentIndex,
            items = videos,
            firstVisibleIndex = firstVisible,
            lastVisibleIndex = lastVisible
        )
    }
}
```

## Components

### PreloadableItem Interface

```kotlin
interface PreloadableItem {
    val id: String
    val url: String
    val priority: Int get() = 50 // Default priority
}
```

Any data class can implement this interface to work with the helper.

### PreloadConfig

```kotlin
data class PreloadConfig(
    val preloadDurationUs: Long = 5_000_000L, // 5 seconds
    val maxPreloadItems: Int = 10,
    val preloadRange: Int = 3,
    val highPriorityThreshold: Int = 80,
    val minBufferMs: Int = 15000,
    val maxBufferMs: Int = 50000,
    val bufferForPlaybackMs: Int = 2500,
    val bufferForPlaybackAfterRebufferMs: Int = 5000
)
```

### PreloadListener

```kotlin
interface PreloadListener {
    fun onPreloadStarted(itemId: String) {}
    fun onPreloadProgress(itemId: String, progress: Float) {}
    fun onPreloadCompleted(itemId: String) {}
    fun onPreloadError(itemId: String, error: String) {}
    fun onPreloadRemoved(itemId: String) {}
}
```

## Examples

### Video List Example

```kotlin
// Video data class
data class Video(
    override val id: String,
    val title: String,
    override val url: String,
    override val priority: Int = 50
) : PreloadableItem

// Usage in Compose
@Composable
fun VideoListWithHelperScreen() {
    val viewModel: VideoListWithHelperViewModel = viewModel()
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.initializePreloading(context)
    }
    
    LazyColumn {
        itemsIndexed(viewModel.videos) { index, video ->
            VideoCard(
                video = video,
                onPlayClick = { viewModel.playVideo(index) },
                player = viewModel.player
            )
        }
    }
}
```

### Podcast/Audio Example

```kotlin
// Podcast data class
data class PodcastEpisode(
    override val id: String,
    val title: String,
    override val url: String,
    val duration: Long,
    val showName: String,
    override val priority: Int = 50
) : PreloadableItem

// Audio-optimized configuration
val config = PreloadConfig(
    preloadDurationUs = 30_000_000L, // 30 seconds for audio
    maxPreloadItems = 8, // More items for audio
    preloadRange = 4, // Wider range for audio
    minBufferMs = 5000, // Shorter buffer for audio
    maxBufferMs = 15000
)
```

## Configuration Options

### Media Type Optimizations

#### For Video Content
```kotlin
val videoConfig = PreloadConfig(
    preloadDurationUs = 5_000_000L, // 5 seconds
    maxPreloadItems = 5,
    preloadRange = 2,
    minBufferMs = 15000,
    maxBufferMs = 50000
)
```

#### For Audio/Podcast Content
```kotlin
val audioConfig = PreloadConfig(
    preloadDurationUs = 30_000_000L, // 30 seconds
    maxPreloadItems = 8,
    preloadRange = 4,
    minBufferMs = 5000,
    maxBufferMs = 15000
)
```

#### For Short-form Content (TikTok-style)
```kotlin
val shortFormConfig = PreloadConfig(
    preloadDurationUs = 15_000_000L, // 15 seconds
    maxPreloadItems = 10,
    preloadRange = 5,
    minBufferMs = 10000,
    maxBufferMs = 30000
)
```

## Advanced Features

### Priority Management

The helper automatically calculates priorities based on:
- Distance from current playing item
- Visibility in the list
- User-defined base priority

```kotlin
val priority = when (distance) {
    0 -> 1000 // Current item - highest priority
    1 -> 900  // Adjacent items
    2 -> 800  // Two items away
    else -> 700 // Further away
}

// Boost priority if in visible range
val finalPriority = if (visibleRange.contains(index)) {
    priority + 100
} else {
    priority
}
```

### Status Tracking

```kotlin
data class PreloadItemStatus(
    val id: String,
    val isPreloaded: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null,
    val priority: Int = 50
)

// Usage in Compose
val preloadStatuses by preloadingHelper.itemStatuses.collectAsState()
```

### Statistics

```kotlin
data class PreloadStats(
    val totalItems: Int,
    val preloadedItems: Int,
    val loadingItems: Int,
    val errorItems: Int,
    val averagePriority: Double
) {
    val preloadRatio: Float
    val errorRatio: Float
}

// Usage
val stats = preloadingHelper.getStats()
```

## API Reference

### PreloadingHelper Methods

| Method | Description |
|--------|-------------|
| `addItem(item)` | Add a single item to preload |
| `addItems(items)` | Add multiple items to preload |
| `removeItem(itemId)` | Remove an item from preloading |
| `clearAll()` | Clear all preloaded items |
| `updateForPosition()` | Update preloading based on position |
| `updateForLazyList()` | Update for LazyColumn scenarios |
| `playItem(item)` | Play a specific item |
| `getPlayer()` | Get the ExoPlayer instance |
| `getItemStatus(itemId)` | Get status of specific item |
| `isItemPreloaded(itemId)` | Check if item is preloaded |
| `getStats()` | Get preloading statistics |
| `release()` | Release all resources |

### Extension Functions

```kotlin
// Simple creation
fun Context.createPreloadingHelper(): PreloadingHelper

// Advanced creation
fun Context.createPreloadingHelper(
    config: PreloadConfig,
    listener: PreloadListener? = null
): PreloadingHelper

// Easy item addition
fun PreloadingHelper.addAll(items: List<PreloadableItem>): PreloadingHelper

// LazyList optimization
fun PreloadingHelper.updateForLazyList(
    currentPlayingIndex: Int,
    items: List<PreloadableItem>,
    firstVisibleIndex: Int,
    lastVisibleIndex: Int
): PreloadingHelper
```

## Best Practices

### 1. Memory Management

```kotlin
// Set appropriate limits
val config = PreloadConfig(
    maxPreloadItems = 10, // Don't preload too many items
    preloadDurationUs = 5_000_000L // Balance between startup speed and memory
)
```

### 2. Network Optimization

```kotlin
// Different configs for different network conditions
val wifiConfig = PreloadConfig(preloadDurationUs = 10_000_000L)
val mobileConfig = PreloadConfig(preloadDurationUs = 3_000_000L)

val config = if (isOnWifi()) wifiConfig else mobileConfig
```

### 3. Priority Strategy

```kotlin
// Set priorities based on content importance
data class Video(
    override val priority: Int = when (category) {
        "trending" -> 90
        "recommended" -> 80
        "recent" -> 70
        else -> 50
    }
) : PreloadableItem
```

### 4. Error Handling

```kotlin
val listener = object : PreloadListener {
    override fun onPreloadError(itemId: String, error: String) {
        // Log error for analytics
        analytics.logPreloadError(itemId, error)
        
        // Retry with lower priority
        val item = items.find { it.id == itemId }
        item?.let { 
            preloadingHelper.addItem(it.copy(priority = it.priority - 10))
        }
    }
}
```

## Testing

### Unit Testing

```kotlin
@Test
fun `preloading helper manages items correctly`() {
    val helper = context.createPreloadingHelper()
    val items = listOf(/* test items */)
    
    helper.addAll(items)
    
    // Verify items are added
    assertEquals(items.size, helper.getStats().totalItems)
    
    // Verify preloading works
    helper.updateForPosition(0, items)
    // Add assertions
}
```

### Integration Testing

```kotlin
@Test
fun `preloading works with LazyColumn`() = runTest {
    val helper = context.createPreloadingHelper()
    val items = generateTestItems()
    
    helper.addAll(items)
    helper.updateForLazyList(
        currentPlayingIndex = 2,
        items = items,
        firstVisibleIndex = 1,
        lastVisibleIndex = 4
    )
    
    // Verify preloading priorities
    // Check visible items have higher priority
}
```

## Performance Tips

1. **Choose appropriate preload duration** based on content type
2. **Limit concurrent preloads** to avoid overwhelming the device
3. **Use priority-based loading** to preload important content first
4. **Monitor memory usage** and adjust configurations accordingly
5. **Handle network changes** by updating configurations dynamically

## Migration from Direct DefaultPreloadManager

```kotlin
// Before (manual setup)
val preloadManager = DefaultPreloadManager.Builder(context)
    .setBandwidthMeter(bandwidthMeter)
    .setTrackSelector(trackSelector)
    .setLoadControl(loadControl)
    .build()

// After (with helper)
val preloadingHelper = context.createPreloadingHelper(config)
```

## Troubleshooting

### Common Issues

1. **High memory usage**: Reduce `maxPreloadItems` and `preloadDurationUs`
2. **Slow preloading**: Check network conditions and reduce preload range
3. **Missing preload events**: Ensure proper listener implementation
4. **Player not found**: Call `initialize()` before using the helper

### Debug Information

```kotlin
// Get debug information
val stats = preloadingHelper.getStats()
println("Preload ratio: ${stats.preloadRatio}")
println("Error ratio: ${stats.errorRatio}")
println("Average priority: ${stats.averagePriority}")

// Check individual item status
val status = preloadingHelper.getItemStatus("item_id")
println("Item status: $status")
```

## License

This helper is provided as part of the Media3 examples. Follow your project's license requirements when using this code.