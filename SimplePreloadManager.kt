// SimplePreloadManager.kt
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Simple media item for preloading
 */
data class MediaContent(
    val id: String,
    val url: String,
    val isVideo: Boolean = true
)

/**
 * Simple preload status
 */
data class PreloadStatus(
    val id: String,
    val isPreloaded: Boolean = false,
    val isLoading: Boolean = false
)

/**
 * Simple but effective preload manager for Media3 1.7.1
 */
class SimplePreloadManager(private val context: Context) {
    
    private var preloadManager: DefaultPreloadManager? = null
    private var player: ExoPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Track preload status
    private val _preloadStatus = MutableStateFlow<Map<String, PreloadStatus>>(emptyMap())
    val preloadStatus: StateFlow<Map<String, PreloadStatus>> = _preloadStatus.asStateFlow()
    
    // Current playing item
    private var currentPlayingId: String? = null
    
    /**
     * Initialize the preload manager
     */
    fun initialize() {
        if (preloadManager == null) {
            // Create preload manager with simple configuration
            val preloadManagerBuilder = DefaultPreloadManager.Builder(context)
                .setTargetPreloadStatusControl(
                    TargetPreloadStatusControl.PreloadStatusControl { targetPreloadStatus ->
                        targetPreloadStatus.buildUpon()
                            .setPreloadDurationUs(5_000_000L) // 5 seconds
                            .build()
                    }
                )
            
            preloadManager = preloadManagerBuilder.build()
            player = preloadManagerBuilder.buildExoPlayer()
            
            // Add simple player listener
            player?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    currentPlayingId?.let { id ->
                        when (playbackState) {
                            Player.STATE_READY -> updateStatus(id, isPreloaded = true, isLoading = false)
                            Player.STATE_BUFFERING -> updateStatus(id, isPreloaded = false, isLoading = true)
                            Player.STATE_ENDED -> currentPlayingId = null
                        }
                    }
                }
            })
        }
    }
    
    /**
     * Add items for preloading
     */
    fun preloadItems(items: List<MediaContent>) {
        scope.launch {
            items.forEach { item ->
                try {
                    updateStatus(item.id, isLoading = true)
                    
                    val mediaItem = MediaItem.fromUri(item.url)
                    val mediaSource = preloadManager?.getMediaSourceFactory()?.createMediaSource(mediaItem)
                    
                    mediaSource?.let { source ->
                        preloadManager?.add(source, 50) // Default priority
                        updateStatus(item.id, isPreloaded = true, isLoading = false)
                    }
                } catch (e: Exception) {
                    updateStatus(item.id, isPreloaded = false, isLoading = false)
                }
            }
        }
    }
    
    /**
     * Play a specific item
     */
    fun playItem(item: MediaContent) {
        currentPlayingId = item.id
        val mediaItem = MediaItem.fromUri(item.url)
        player?.apply {
            setMediaItem(mediaItem)
            prepare()
            play()
        }
    }
    
    /**
     * Get the player instance
     */
    fun getPlayer(): ExoPlayer? = player
    
    /**
     * Check if item is preloaded
     */
    fun isPreloaded(id: String): Boolean {
        return _preloadStatus.value[id]?.isPreloaded == true
    }
    
    /**
     * Clear all preloaded items
     */
    fun clearAll() {
        _preloadStatus.value = emptyMap()
    }
    
    /**
     * Release resources
     */
    fun release() {
        player?.release()
        preloadManager?.release()
        clearAll()
    }
    
    private fun updateStatus(id: String, isPreloaded: Boolean = false, isLoading: Boolean = false) {
        val currentStatuses = _preloadStatus.value.toMutableMap()
        currentStatuses[id] = PreloadStatus(id, isPreloaded, isLoading)
        _preloadStatus.value = currentStatuses
    }
}