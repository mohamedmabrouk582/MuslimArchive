// PreloadingHelper.kt
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Generic interface for items that can be preloaded
 */
interface PreloadableItem {
    val id: String
    val url: String
    val priority: Int get() = 50 // Default priority
}

/**
 * Configuration for preloading behavior
 */
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

/**
 * Status of a preloaded item
 */
data class PreloadItemStatus(
    val id: String,
    val isPreloaded: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null,
    val priority: Int = 50
)

/**
 * Listener for preloading events
 */
interface PreloadListener {
    fun onPreloadStarted(itemId: String) {}
    fun onPreloadProgress(itemId: String, progress: Float) {}
    fun onPreloadCompleted(itemId: String) {}
    fun onPreloadError(itemId: String, error: String) {}
    fun onPreloadRemoved(itemId: String) {}
}

/**
 * Generic preloading helper for Media3
 */
class PreloadingHelper private constructor(
    private val context: Context,
    private val config: PreloadConfig,
    private val listener: PreloadListener?
) {
    
    private var preloadManager: DefaultPreloadManager? = null
    private var currentPlayer: ExoPlayer? = null
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Track preloaded items and their status
    private val preloadedItems = ConcurrentHashMap<String, MediaSource>()
    private val _itemStatuses = MutableStateFlow<Map<String, PreloadItemStatus>>(emptyMap())
    val itemStatuses: StateFlow<Map<String, PreloadItemStatus>> = _itemStatuses.asStateFlow()
    
    // Media source factory for creating sources
    private lateinit var mediaSourceFactory: DefaultMediaSourceFactory
    
    /**
     * Builder class for creating PreloadingHelper instances
     */
    class Builder(private val context: Context) {
        private var config = PreloadConfig()
        private var listener: PreloadListener? = null
        
        fun setConfig(config: PreloadConfig) = apply { this.config = config }
        
        fun setPreloadDuration(durationUs: Long) = apply {
            this.config = config.copy(preloadDurationUs = durationUs)
        }
        
        fun setMaxPreloadItems(maxItems: Int) = apply {
            this.config = config.copy(maxPreloadItems = maxItems)
        }
        
        fun setPreloadRange(range: Int) = apply {
            this.config = config.copy(preloadRange = range)
        }
        
        fun setBufferConfiguration(
            minBufferMs: Int,
            maxBufferMs: Int,
            bufferForPlaybackMs: Int,
            bufferForPlaybackAfterRebufferMs: Int
        ) = apply {
            this.config = config.copy(
                minBufferMs = minBufferMs,
                maxBufferMs = maxBufferMs,
                bufferForPlaybackMs = bufferForPlaybackMs,
                bufferForPlaybackAfterRebufferMs = bufferForPlaybackAfterRebufferMs
            )
        }
        
        fun setListener(listener: PreloadListener) = apply { this.listener = listener }
        
        fun build(): PreloadingHelper {
            return PreloadingHelper(context, config, listener)
        }
    }
    
    /**
     * Initialize the preloading system
     */
    fun initialize(): PreloadingHelper {
        if (preloadManager == null) {
            // Create custom components
            val bandwidthMeter = DefaultBandwidthMeter.Builder(context).build()
            val trackSelector = DefaultTrackSelector(context)
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    config.minBufferMs,
                    config.maxBufferMs,
                    config.bufferForPlaybackMs,
                    config.bufferForPlaybackAfterRebufferMs
                )
                .build()
            
            // Create data source factory
            val dataSourceFactory = DefaultDataSource.Factory(context)
            mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            
            // Create preload manager
            val preloadManagerBuilder = DefaultPreloadManager.Builder(context)
                .setBandwidthMeter(bandwidthMeter)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .setTargetPreloadStatusControl(
                    TargetPreloadStatusControl.PreloadStatusControl { targetPreloadStatus ->
                        targetPreloadStatus.buildUpon()
                            .setPreloadDurationUs(config.preloadDurationUs)
                            .build()
                    }
                )
            
            preloadManager = preloadManagerBuilder.build()
            currentPlayer = preloadManagerBuilder.buildExoPlayer()
            
            // Configure player
            currentPlayer?.apply {
                preloadConfiguration = androidx.media3.exoplayer.PreloadConfiguration(
                    targetPreloadDurationUs = config.preloadDurationUs
                )
            }
        }
        return this
    }
    
    /**
     * Get the ExoPlayer instance for playback
     */
    fun getPlayer(): ExoPlayer? = currentPlayer
    
    /**
     * Add a single item to preload
     */
    fun addItem(item: PreloadableItem): PreloadingHelper {
        coroutineScope.launch {
            try {
                updateItemStatus(item.id, PreloadItemStatus(
                    id = item.id,
                    isLoading = true,
                    priority = item.priority
                ))
                
                listener?.onPreloadStarted(item.id)
                
                val mediaItem = MediaItem.fromUri(item.url)
                val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)
                
                preloadManager?.add(mediaSource, item.priority)
                preloadedItems[item.id] = mediaSource
                
                updateItemStatus(item.id, PreloadItemStatus(
                    id = item.id,
                    isPreloaded = true,
                    isLoading = false,
                    progress = 1.0f,
                    priority = item.priority
                ))
                
                listener?.onPreloadCompleted(item.id)
                
            } catch (e: Exception) {
                updateItemStatus(item.id, PreloadItemStatus(
                    id = item.id,
                    isLoading = false,
                    error = e.message,
                    priority = item.priority
                ))
                listener?.onPreloadError(item.id, e.message ?: "Unknown error")
            }
        }
        return this
    }
    
    /**
     * Add multiple items to preload
     */
    fun addItems(items: List<PreloadableItem>): PreloadingHelper {
        // Sort by priority (higher priority first)
        val sortedItems = items.sortedByDescending { it.priority }
        
        // Limit to max preload items
        val itemsToPreload = sortedItems.take(config.maxPreloadItems)
        
        itemsToPreload.forEach { item ->
            addItem(item)
        }
        return this
    }
    
    /**
     * Remove an item from preloading
     */
    fun removeItem(itemId: String): PreloadingHelper {
        preloadedItems.remove(itemId)?.let { mediaSource ->
            // Note: DefaultPreloadManager doesn't have a direct remove method
            // In a real implementation, you might need to track and manage this differently
            updateItemStatus(itemId, null)
            listener?.onPreloadRemoved(itemId)
        }
        return this
    }
    
    /**
     * Clear all preloaded items
     */
    fun clearAll(): PreloadingHelper {
        preloadedItems.clear()
        _itemStatuses.value = emptyMap()
        return this
    }
    
    /**
     * Update preloading based on current position and visible range
     */
    fun updateForPosition(
        currentIndex: Int,
        items: List<PreloadableItem>,
        visibleRange: IntRange? = null
    ): PreloadingHelper {
        coroutineScope.launch {
            // Calculate preload range
            val startIndex = maxOf(0, currentIndex - config.preloadRange)
            val endIndex = minOf(items.size - 1, currentIndex + config.preloadRange)
            
            // Create list of items to preload with adjusted priorities
            val itemsToPreload = mutableListOf<PreloadableItem>()
            
            for (i in startIndex..endIndex) {
                if (i < items.size) {
                    val item = items[i]
                    val distance = kotlin.math.abs(i - currentIndex)
                    
                    // Calculate priority based on distance and visibility
                    val basePriority = when (distance) {
                        0 -> 1000 // Current item - highest priority
                        1 -> 900  // Adjacent items
                        2 -> 800  // Two items away
                        else -> 700 // Further away
                    }
                    
                    // Boost priority if in visible range
                    val finalPriority = if (visibleRange?.contains(i) == true) {
                        basePriority + 100
                    } else {
                        basePriority
                    }
                    
                    // Create item with adjusted priority
                    val prioritizedItem = object : PreloadableItem {
                        override val id = item.id
                        override val url = item.url
                        override val priority = finalPriority
                    }
                    
                    itemsToPreload.add(prioritizedItem)
                }
            }
            
            // Remove items that are too far away
            val itemsToRemove = preloadedItems.keys.filter { itemId ->
                val itemIndex = items.indexOfFirst { it.id == itemId }
                itemIndex < startIndex || itemIndex > endIndex
            }
            
            itemsToRemove.forEach { removeItem(it) }
            
            // Add new items
            addItems(itemsToPreload)
        }
        return this
    }
    
    /**
     * Get the status of a specific item
     */
    fun getItemStatus(itemId: String): PreloadItemStatus? {
        return _itemStatuses.value[itemId]
    }
    
    /**
     * Check if an item is preloaded
     */
    fun isItemPreloaded(itemId: String): Boolean {
        return getItemStatus(itemId)?.isPreloaded == true
    }
    
    /**
     * Get all preloaded item IDs
     */
    fun getPreloadedItemIds(): Set<String> {
        return _itemStatuses.value.filter { it.value.isPreloaded }.keys
    }
    
    /**
     * Get preload statistics
     */
    fun getStats(): PreloadStats {
        val statuses = _itemStatuses.value.values
        return PreloadStats(
            totalItems = statuses.size,
            preloadedItems = statuses.count { it.isPreloaded },
            loadingItems = statuses.count { it.isLoading },
            errorItems = statuses.count { it.error != null },
            averagePriority = statuses.map { it.priority }.average().takeIf { it.isFinite() } ?: 0.0
        )
    }
    
    /**
     * Play a specific item
     */
    fun playItem(item: PreloadableItem): PreloadingHelper {
        currentPlayer?.let { player ->
            val mediaItem = MediaItem.fromUri(item.url)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
        return this
    }
    
    /**
     * Release resources
     */
    fun release() {
        currentPlayer?.release()
        preloadManager?.release()
        preloadedItems.clear()
        _itemStatuses.value = emptyMap()
    }
    
    private fun updateItemStatus(itemId: String, status: PreloadItemStatus?) {
        val currentStatuses = _itemStatuses.value.toMutableMap()
        if (status != null) {
            currentStatuses[itemId] = status
        } else {
            currentStatuses.remove(itemId)
        }
        _itemStatuses.value = currentStatuses
    }
}

/**
 * Statistics about preloading performance
 */
data class PreloadStats(
    val totalItems: Int,
    val preloadedItems: Int,
    val loadingItems: Int,
    val errorItems: Int,
    val averagePriority: Double
) {
    val preloadRatio: Float = if (totalItems > 0) preloadedItems.toFloat() / totalItems else 0f
    val errorRatio: Float = if (totalItems > 0) errorItems.toFloat() / totalItems else 0f
}

/**
 * Extension functions for easier usage
 */

/**
 * Create a simple preloading helper with default configuration
 */
fun Context.createPreloadingHelper(): PreloadingHelper {
    return PreloadingHelper.Builder(this).build().initialize()
}

/**
 * Create a preloading helper with custom configuration
 */
fun Context.createPreloadingHelper(
    config: PreloadConfig,
    listener: PreloadListener? = null
): PreloadingHelper {
    return PreloadingHelper.Builder(this)
        .setConfig(config)
        .apply { listener?.let { setListener(it) } }
        .build()
        .initialize()
}

/**
 * Extension for List<PreloadableItem> to easily add all items
 */
fun PreloadingHelper.addAll(items: List<PreloadableItem>): PreloadingHelper {
    return addItems(items)
}

/**
 * Extension to update preloading for LazyColumn-like scenarios
 */
fun PreloadingHelper.updateForLazyList(
    currentPlayingIndex: Int,
    items: List<PreloadableItem>,
    firstVisibleIndex: Int,
    lastVisibleIndex: Int
): PreloadingHelper {
    val visibleRange = firstVisibleIndex..lastVisibleIndex
    return updateForPosition(currentPlayingIndex, items, visibleRange)
}