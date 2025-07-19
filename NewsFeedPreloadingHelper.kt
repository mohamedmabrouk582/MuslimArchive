// NewsFeedPreloadingHelper.kt
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
 * Types of content in news feed
 */
enum class FeedContentType {
    VIDEO,
    AUDIO,
    IMAGE_NEWS,
    TEXT_ONLY
}

/**
 * Auto-play behavior configuration
 */
data class AutoPlayConfig(
    val enableAutoPlay: Boolean = true,
    val autoPlayOnWifi: Boolean = true,
    val autoPlayOnMobile: Boolean = false,
    val minVisibilityPercentage: Float = 0.5f, // 50% visibility to trigger auto-play
    val autoMuteVideos: Boolean = true,
    val pauseWhenNotVisible: Boolean = true,
    val continuePlayingWhenScrolling: Boolean = false,
    val maxConcurrentPlayers: Int = 1, // Like Facebook/LinkedIn - only one video plays at a time
    val prioritizeVideoOverAudio: Boolean = true
)

/**
 * Configuration for news feed preloading
 */
data class NewsFeedPreloadConfig(
    val videoPreloadDurationUs: Long = 8_000_000L, // 8 seconds for video
    val audioPreloadDurationUs: Long = 15_000_000L, // 15 seconds for audio
    val maxPreloadItems: Int = 8,
    val preloadRange: Int = 5, // Wider range for feed content
    val videoBufferMs: Int = 15000,
    val audioBufferMs: Int = 5000,
    val autoPlayConfig: AutoPlayConfig = AutoPlayConfig()
)

/**
 * Interface for news feed items that can contain media
 */
interface NewsFeedItem : PreloadableItem {
    val contentType: FeedContentType
    val thumbnailUrl: String?
    val title: String
    val description: String?
    val authorName: String?
    val publishTime: Long
    val duration: Long? // For video/audio content
    val hasSound: Boolean get() = contentType == FeedContentType.VIDEO || contentType == FeedContentType.AUDIO
    
    // Auto-play eligibility
    val canAutoPlay: Boolean get() = contentType == FeedContentType.VIDEO || contentType == FeedContentType.AUDIO
}

/**
 * Status of item visibility and playback
 */
data class FeedItemPlaybackStatus(
    val id: String,
    val isVisible: Boolean = false,
    val visibilityPercentage: Float = 0f,
    val isPlaying: Boolean = false,
    val isMuted: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val preloadStatus: PreloadItemStatus = PreloadItemStatus(id)
)

/**
 * Enhanced preloading helper for news feeds with auto-play capabilities
 */
class NewsFeedPreloadingHelper private constructor(
    private val context: Context,
    private val config: NewsFeedPreloadConfig,
    private val listener: PreloadListener?
) {
    
    private var preloadManager: DefaultPreloadManager? = null
    private var primaryPlayer: ExoPlayer? = null
    private var secondaryPlayer: ExoPlayer? = null // For handling multiple visible items
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Track items and their status
    private val preloadedItems = ConcurrentHashMap<String, MediaSource>()
    private val _itemStatuses = MutableStateFlow<Map<String, PreloadItemStatus>>(emptyMap())
    val itemStatuses: StateFlow<Map<String, PreloadItemStatus>> = _itemStatuses.asStateFlow()
    
    // Track playback status for auto-play management
    private val _playbackStatuses = MutableStateFlow<Map<String, FeedItemPlaybackStatus>>(emptyMap())
    val playbackStatuses: StateFlow<Map<String, FeedItemPlaybackStatus>> = _playbackStatuses.asStateFlow()
    
    // Currently playing items (can be multiple for audio/video mix)
    private var currentPlayingVideoId: String? = null
    private var currentPlayingAudioId: String? = null
    
    private lateinit var mediaSourceFactory: DefaultMediaSourceFactory
    
    /**
     * Builder for NewsFeedPreloadingHelper
     */
    class Builder(private val context: Context) {
        private var config = NewsFeedPreloadConfig()
        private var listener: PreloadListener? = null
        
        fun setConfig(config: NewsFeedPreloadConfig) = apply { this.config = config }
        
        fun setAutoPlayConfig(autoPlayConfig: AutoPlayConfig) = apply {
            this.config = config.copy(autoPlayConfig = autoPlayConfig)
        }
        
        fun setVideoPreloadDuration(durationUs: Long) = apply {
            this.config = config.copy(videoPreloadDurationUs = durationUs)
        }
        
        fun setAudioPreloadDuration(durationUs: Long) = apply {
            this.config = config.copy(audioPreloadDurationUs = durationUs)
        }
        
        fun setMaxConcurrentPlayers(maxPlayers: Int) = apply {
            this.config = config.copy(
                autoPlayConfig = config.autoPlayConfig.copy(maxConcurrentPlayers = maxPlayers)
            )
        }
        
        fun setListener(listener: PreloadListener) = apply { this.listener = listener }
        
        fun build(): NewsFeedPreloadingHelper {
            return NewsFeedPreloadingHelper(context, config, listener)
        }
    }
    
    /**
     * Initialize the helper
     */
    fun initialize(): NewsFeedPreloadingHelper {
        if (preloadManager == null) {
            setupPreloadManager()
            setupPlayers()
        }
        return this
    }
    
    private fun setupPreloadManager() {
        val bandwidthMeter = DefaultBandwidthMeter.Builder(context).build()
        val trackSelector = DefaultTrackSelector(context)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                config.videoBufferMs,
                config.videoBufferMs * 3,
                config.videoBufferMs / 3,
                config.videoBufferMs / 2
            )
            .build()
        
        val dataSourceFactory = DefaultDataSource.Factory(context)
        mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        
        val preloadManagerBuilder = DefaultPreloadManager.Builder(context)
            .setBandwidthMeter(bandwidthMeter)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setTargetPreloadStatusControl(
                TargetPreloadStatusControl.PreloadStatusControl { targetPreloadStatus ->
                    targetPreloadStatus.buildUpon()
                        .setPreloadDurationUs(config.videoPreloadDurationUs)
                        .build()
                }
            )
        
        preloadManager = preloadManagerBuilder.build()
    }
    
    private fun setupPlayers() {
        // Primary player for main content
        primaryPlayer = ExoPlayer.Builder(context).build().apply {
            addListener(createPlayerListener("primary"))
        }
        
        // Secondary player for handling concurrent audio/video scenarios
        if (config.autoPlayConfig.maxConcurrentPlayers > 1) {
            secondaryPlayer = ExoPlayer.Builder(context).build().apply {
                addListener(createPlayerListener("secondary"))
            }
        }
    }
    
    private fun createPlayerListener(playerType: String) = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val playerId = if (playerType == "primary") currentPlayingVideoId else currentPlayingAudioId
            playerId?.let { itemId ->
                when (playbackState) {
                    Player.STATE_READY -> {
                        updatePlaybackStatus(itemId) { it.copy(isPlaying = true) }
                        updatePreloadStatus(itemId, PreloadItemStatus(itemId, isPreloaded = true, progress = 1.0f))
                    }
                    Player.STATE_BUFFERING -> {
                        updatePreloadStatus(itemId, PreloadItemStatus(itemId, isLoading = true, progress = 0.5f))
                    }
                    Player.STATE_ENDED -> {
                        updatePlaybackStatus(itemId) { it.copy(isPlaying = false, currentPosition = 0L) }
                        handlePlaybackEnded(itemId)
                    }
                }
            }
        }
        
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val playerId = if (playerType == "primary") currentPlayingVideoId else currentPlayingAudioId
            playerId?.let { itemId ->
                updatePlaybackStatus(itemId) { it.copy(isPlaying = isPlaying) }
            }
        }
        
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            val playerId = if (playerType == "primary") currentPlayingVideoId else currentPlayingAudioId
            playerId?.let { itemId ->
                updatePreloadStatus(itemId, PreloadItemStatus(itemId, error = error.message))
                listener?.onPreloadError(itemId, error.message ?: "Playback error")
            }
        }
    }
    
    /**
     * Add news feed items for preloading
     */
    fun addFeedItems(items: List<NewsFeedItem>): NewsFeedPreloadingHelper {
        coroutineScope.launch {
            // Sort by priority and content type
            val sortedItems = items.sortedWith(compareByDescending<NewsFeedItem> { it.priority }
                .thenBy { if (it.canAutoPlay) 0 else 1 })
            
            val itemsToPreload = sortedItems
                .filter { it.canAutoPlay }
                .take(config.maxPreloadItems)
            
            itemsToPreload.forEach { item ->
                addSingleItem(item)
            }
        }
        return this
    }
    
    private fun addSingleItem(item: NewsFeedItem) {
        try {
            if (!item.canAutoPlay) return
            
            updatePreloadStatus(item.id, PreloadItemStatus(item.id, isLoading = true))
            listener?.onPreloadStarted(item.id)
            
            val mediaItem = MediaItem.fromUri(item.url)
            val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)
            
            preloadManager?.add(mediaSource, item.priority)
            preloadedItems[item.id] = mediaSource
            
            updatePreloadStatus(item.id, PreloadItemStatus(
                id = item.id,
                isPreloaded = true,
                progress = 1.0f,
                priority = item.priority
            ))
            
            listener?.onPreloadCompleted(item.id)
            
        } catch (e: Exception) {
            updatePreloadStatus(item.id, PreloadItemStatus(item.id, error = e.message))
            listener?.onPreloadError(item.id, e.message ?: "Unknown error")
        }
    }
    
    /**
     * Update visibility and handle auto-play logic
     */
    fun updateItemVisibility(
        itemId: String,
        visibilityPercentage: Float,
        item: NewsFeedItem
    ): NewsFeedPreloadingHelper {
        val isVisible = visibilityPercentage >= config.autoPlayConfig.minVisibilityPercentage
        
        updatePlaybackStatus(itemId) { currentStatus ->
            currentStatus.copy(
                isVisible = isVisible,
                visibilityPercentage = visibilityPercentage
            )
        }
        
        if (isVisible && config.autoPlayConfig.enableAutoPlay && item.canAutoPlay) {
            handleAutoPlay(item)
        } else if (!isVisible && config.autoPlayConfig.pauseWhenNotVisible) {
            handleAutoPause(itemId, item)
        }
        
        return this
    }
    
    private fun handleAutoPlay(item: NewsFeedItem) {
        coroutineScope.launch {
            // Check if we should auto-play based on network conditions
            if (!shouldAutoPlay()) return@launch
            
            when (item.contentType) {
                FeedContentType.VIDEO -> handleVideoAutoPlay(item)
                FeedContentType.AUDIO -> handleAudioAutoPlay(item)
                else -> return@launch
            }
        }
    }
    
    private fun handleVideoAutoPlay(item: NewsFeedItem) {
        // Facebook/LinkedIn behavior: Only one video plays at a time
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
        
        // Use secondary player for audio if available
        val player = secondaryPlayer ?: primaryPlayer
        playItem(item, player, muted = false)
        currentPlayingAudioId = item.id
    }
    
    private fun handleAutoPause(itemId: String, item: NewsFeedItem) {
        when (item.contentType) {
            FeedContentType.VIDEO -> {
                if (currentPlayingVideoId == itemId) {
                    primaryPlayer?.pause()
                    currentPlayingVideoId = null
                }
            }
            FeedContentType.AUDIO -> {
                if (currentPlayingAudioId == itemId) {
                    (secondaryPlayer ?: primaryPlayer)?.pause()
                    currentPlayingAudioId = null
                }
            }
            else -> { /* No action needed */ }
        }
    }
    
    private fun playItem(item: NewsFeedItem, player: ExoPlayer?, muted: Boolean) {
        player?.let { exoPlayer ->
            val mediaItem = MediaItem.fromUri(item.url)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.volume = if (muted) 0f else 1f
            exoPlayer.play()
            
            updatePlaybackStatus(item.id) { status ->
                status.copy(isPlaying = true, isMuted = muted)
            }
        }
    }
    
    private fun pauseCurrentVideo() {
        currentPlayingVideoId?.let { videoId ->
            primaryPlayer?.pause()
            updatePlaybackStatus(videoId) { it.copy(isPlaying = false) }
            currentPlayingVideoId = null
        }
    }
    
    private fun pauseCurrentAudio() {
        currentPlayingAudioId?.let { audioId ->
            (secondaryPlayer ?: primaryPlayer)?.pause()
            updatePlaybackStatus(audioId) { it.copy(isPlaying = false) }
            currentPlayingAudioId = null
        }
    }
    
    private fun shouldAutoPlay(): Boolean {
        // Check network conditions and user preferences
        return config.autoPlayConfig.enableAutoPlay &&
                (isOnWifi() && config.autoPlayConfig.autoPlayOnWifi ||
                 !isOnWifi() && config.autoPlayConfig.autoPlayOnMobile)
    }
    
    private fun isOnWifi(): Boolean {
        // Implementation to check if device is on WiFi
        // You would implement this based on your network detection logic
        return true // Placeholder
    }
    
    private fun handlePlaybackEnded(itemId: String) {
        // Reset playback status
        updatePlaybackStatus(itemId) { it.copy(isPlaying = false, currentPosition = 0L) }
        
        // Clear current playing references
        if (currentPlayingVideoId == itemId) {
            currentPlayingVideoId = null
        }
        if (currentPlayingAudioId == itemId) {
            currentPlayingAudioId = null
        }
    }
    
    /**
     * Manual play/pause controls
     */
    fun playItem(item: NewsFeedItem): NewsFeedPreloadingHelper {
        when (item.contentType) {
            FeedContentType.VIDEO -> {
                pauseCurrentVideo()
                playItem(item, primaryPlayer, muted = false)
                currentPlayingVideoId = item.id
            }
            FeedContentType.AUDIO -> {
                pauseCurrentAudio()
                playItem(item, secondaryPlayer ?: primaryPlayer, muted = false)
                currentPlayingAudioId = item.id
            }
            else -> { /* No action for non-media content */ }
        }
        return this
    }
    
    fun pauseItem(itemId: String): NewsFeedPreloadingHelper {
        when (itemId) {
            currentPlayingVideoId -> {
                primaryPlayer?.pause()
                updatePlaybackStatus(itemId) { it.copy(isPlaying = false) }
            }
            currentPlayingAudioId -> {
                (secondaryPlayer ?: primaryPlayer)?.pause()
                updatePlaybackStatus(itemId) { it.copy(isPlaying = false) }
            }
        }
        return this
    }
    
    fun toggleMute(itemId: String): NewsFeedPreloadingHelper {
        val currentStatus = _playbackStatuses.value[itemId]
        if (currentStatus?.isPlaying == true) {
            val player = when (itemId) {
                currentPlayingVideoId -> primaryPlayer
                currentPlayingAudioId -> secondaryPlayer ?: primaryPlayer
                else -> null
            }
            
            player?.let { exoPlayer ->
                val newMutedState = !currentStatus.isMuted
                exoPlayer.volume = if (newMutedState) 0f else 1f
                updatePlaybackStatus(itemId) { it.copy(isMuted = newMutedState) }
            }
        }
        return this
    }
    
    /**
     * Update preloading based on feed position and visible items
     */
    fun updateForFeedPosition(
        currentVisibleItems: List<Pair<Int, NewsFeedItem>>, // (index, item) pairs
        allItems: List<NewsFeedItem>
    ): NewsFeedPreloadingHelper {
        coroutineScope.launch {
            // Calculate preload range around visible items
            val visibleIndices = currentVisibleItems.map { it.first }
            val minVisible = visibleIndices.minOrNull() ?: 0
            val maxVisible = visibleIndices.maxOrNull() ?: 0
            
            val startIndex = maxOf(0, minVisible - config.preloadRange)
            val endIndex = minOf(allItems.size - 1, maxVisible + config.preloadRange)
            
            // Items to preload
            val itemsToPreload = (startIndex..endIndex)
                .mapNotNull { index -> allItems.getOrNull(index) }
                .filter { it.canAutoPlay }
                .take(config.maxPreloadItems)
            
            // Remove items that are too far away
            val itemsToRemove = preloadedItems.keys.filter { itemId ->
                val itemIndex = allItems.indexOfFirst { it.id == itemId }
                itemIndex < startIndex || itemIndex > endIndex
            }
            
            itemsToRemove.forEach { removeItem(it) }
            
            // Add new items
            addFeedItems(itemsToPreload)
        }
        return this
    }
    
    fun removeItem(itemId: String): NewsFeedPreloadingHelper {
        preloadedItems.remove(itemId)
        updatePreloadStatus(itemId, null)
        updatePlaybackStatus(itemId, null)
        listener?.onPreloadRemoved(itemId)
        return this
    }
    
    /**
     * Get various states and statistics
     */
    fun getPlaybackStatus(itemId: String): FeedItemPlaybackStatus? {
        return _playbackStatuses.value[itemId]
    }
    
    fun getCurrentlyPlayingItems(): List<String> {
        return listOfNotNull(currentPlayingVideoId, currentPlayingAudioId)
    }
    
    fun getPreloadStats(): PreloadStats {
        val statuses = _itemStatuses.value.values
        return PreloadStats(
            totalItems = statuses.size,
            preloadedItems = statuses.count { it.isPreloaded },
            loadingItems = statuses.count { it.isLoading },
            errorItems = statuses.count { it.error != null },
            averagePriority = statuses.map { it.priority }.average().takeIf { it.isFinite() } ?: 0.0
        )
    }
    
    fun getPrimaryPlayer(): ExoPlayer? = primaryPlayer
    fun getSecondaryPlayer(): ExoPlayer? = secondaryPlayer
    
    /**
     * Cleanup
     */
    fun release() {
        primaryPlayer?.release()
        secondaryPlayer?.release()
        preloadManager?.release()
        preloadedItems.clear()
        _itemStatuses.value = emptyMap()
        _playbackStatuses.value = emptyMap()
        currentPlayingVideoId = null
        currentPlayingAudioId = null
    }
    
    private fun updatePreloadStatus(itemId: String, status: PreloadItemStatus?) {
        val currentStatuses = _itemStatuses.value.toMutableMap()
        if (status != null) {
            currentStatuses[itemId] = status
        } else {
            currentStatuses.remove(itemId)
        }
        _itemStatuses.value = currentStatuses
    }
    
    private fun updatePlaybackStatus(itemId: String, update: ((FeedItemPlaybackStatus) -> FeedItemPlaybackStatus)?) {
        val currentStatuses = _playbackStatuses.value.toMutableMap()
        if (update != null) {
            val currentStatus = currentStatuses[itemId] ?: FeedItemPlaybackStatus(itemId)
            currentStatuses[itemId] = update(currentStatus)
        } else {
            currentStatuses.remove(itemId)
        }
        _playbackStatuses.value = currentStatuses
    }
}

/**
 * Extension functions for easier usage
 */
fun Context.createNewsFeedPreloadingHelper(
    config: NewsFeedPreloadConfig = NewsFeedPreloadConfig(),
    listener: PreloadListener? = null
): NewsFeedPreloadingHelper {
    return NewsFeedPreloadingHelper.Builder(this)
        .setConfig(config)
        .apply { listener?.let { setListener(it) } }
        .build()
        .initialize()
}