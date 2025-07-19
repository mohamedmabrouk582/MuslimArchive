// SimpleAutoPlayManager.kt
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.flow.StateFlow

/**
 * Simple auto-play manager that extends SimplePreloadManager
 */
class SimpleAutoPlayManager(context: Context) : SimplePreloadManager(context) {
    
    private var autoPlayEnabled = true
    private var currentAutoPlayingId: String? = null
    
    /**
     * Update item visibility and handle auto-play
     */
    fun updateVisibility(item: MediaContent, visibilityPercentage: Float) {
        val shouldAutoPlay = visibilityPercentage >= 0.6f && autoPlayEnabled
        
        if (shouldAutoPlay && currentAutoPlayingId != item.id) {
            // Stop current auto-playing item
            currentAutoPlayingId?.let { stopAutoPlay() }
            
            // Start auto-play for this item
            startAutoPlay(item)
        } else if (!shouldAutoPlay && currentAutoPlayingId == item.id) {
            // Stop auto-play if not visible enough
            stopAutoPlay()
        }
    }
    
    private fun startAutoPlay(item: MediaContent) {
        currentAutoPlayingId = item.id
        playItem(item)
        // Start muted for auto-play
        getPlayer()?.volume = 0f
    }
    
    private fun stopAutoPlay() {
        getPlayer()?.pause()
        currentAutoPlayingId = null
    }
    
    /**
     * Toggle mute for current playing video
     */
    fun toggleMute() {
        getPlayer()?.let { player ->
            player.volume = if (player.volume == 0f) 1f else 0f
        }
    }
    
    /**
     * Check if current item is muted
     */
    fun isMuted(): Boolean {
        return getPlayer()?.volume == 0f
    }
    
    /**
     * Get currently auto-playing item ID
     */
    fun getCurrentAutoPlayingId(): String? = currentAutoPlayingId
    
    /**
     * Enable/disable auto-play
     */
    fun setAutoPlayEnabled(enabled: Boolean) {
        autoPlayEnabled = enabled
        if (!enabled) {
            stopAutoPlay()
        }
    }
}

// Enhanced ViewModel with auto-play
class SimpleAutoPlayViewModel : ViewModel() {
    private var autoPlayManager: SimpleAutoPlayManager? = null
    
    // Sample content
    val feedItems = listOf(
        MediaContent("1", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
        MediaContent("2", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"),
        MediaContent("3", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"),
        MediaContent("4", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"),
        MediaContent("5", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4")
    )
    
    val preloadStatus: StateFlow<Map<String, PreloadStatus>>?
        get() = autoPlayManager?.preloadStatus
    
    val player get() = autoPlayManager?.getPlayer()
    
    fun initialize(context: Context) {
        if (autoPlayManager == null) {
            autoPlayManager = SimpleAutoPlayManager(context).apply {
                initialize()
                preloadItems(feedItems.take(3))
            }
        }
    }
    
    fun updateItemVisibility(item: MediaContent, visibilityPercentage: Float) {
        autoPlayManager?.updateVisibility(item, visibilityPercentage)
    }
    
    fun updateVisibleRange(firstVisible: Int, lastVisible: Int) {
        val startIndex = maxOf(0, firstVisible - 1)
        val endIndex = minOf(feedItems.size - 1, lastVisible + 2)
        val itemsToPreload = feedItems.subList(startIndex, endIndex + 1)
        autoPlayManager?.preloadItems(itemsToPreload)
    }
    
    fun playItem(item: MediaContent) {
        autoPlayManager?.playItem(item)
    }
    
    fun toggleMute() {
        autoPlayManager?.toggleMute()
    }
    
    fun isMuted(): Boolean {
        return autoPlayManager?.isMuted() == true
    }
    
    fun getCurrentAutoPlayingId(): String? {
        return autoPlayManager?.getCurrentAutoPlayingId()
    }
    
    override fun onCleared() {
        super.onCleared()
        autoPlayManager?.release()
    }
}

@Composable
fun SimpleAutoPlayFeedScreen(
    viewModel: SimpleAutoPlayViewModel = viewModel()
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val preloadStatuses by (viewModel.preloadStatus?.collectAsState() ?: remember { mutableStateOf(emptyMap()) })
    
    // Initialize
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }
    
    // Track visible items and update auto-play
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.map { itemInfo ->
                val item = viewModel.feedItems.getOrNull(itemInfo.index)
                if (item != null) {
                    // Calculate visibility percentage
                    val itemTop = itemInfo.offset
                    val itemBottom = itemInfo.offset + itemInfo.size
                    val viewportHeight = listState.layoutInfo.viewportEndOffset
                    
                    val visibleHeight = when {
                        itemTop >= 0 && itemBottom <= viewportHeight -> itemInfo.size
                        itemTop < 0 && itemBottom > 0 -> itemBottom
                        itemTop < viewportHeight && itemBottom > viewportHeight -> viewportHeight - itemTop
                        else -> 0
                    }
                    
                    val visibilityPercentage = if (itemInfo.size > 0) {
                        (visibleHeight.toFloat() / itemInfo.size).coerceIn(0f, 1f)
                    } else 0f
                    
                    item to visibilityPercentage
                } else null
            }.filterNotNull()
        }.collect { itemVisibilities ->
            // Update visibility for each item
            itemVisibilities.forEach { (item, visibility) ->
                viewModel.updateItemVisibility(item, visibility)
            }
            
            // Update visible range for preloading
            val visibleIndices = listState.layoutInfo.visibleItemsInfo.map { it.index }
            if (visibleIndices.isNotEmpty()) {
                viewModel.updateVisibleRange(visibleIndices.first(), visibleIndices.last())
            }
        }
    }
    
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(viewModel.feedItems) { index, item ->
            SimpleAutoPlayVideoCard(
                item = item,
                index = index,
                isAutoPlaying = viewModel.getCurrentAutoPlayingId() == item.id,
                isPreloaded = preloadStatuses[item.id]?.isPreloaded == true,
                isMuted = viewModel.isMuted(),
                onPlayClick = { viewModel.playItem(item) },
                onMuteToggle = { viewModel.toggleMute() },
                player = viewModel.player
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleAutoPlayVideoCard(
    item: MediaContent,
    index: Int,
    isAutoPlaying: Boolean,
    isPreloaded: Boolean,
    isMuted: Boolean,
    onPlayClick: () -> Unit,
    onMuteToggle: () -> Unit,
    player: androidx.media3.exoplayer.ExoPlayer?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isAutoPlaying && player != null) {
                // Show player when auto-playing
                AndroidView(
                    factory = { context ->
                        PlayerView(context).apply {
                            this.player = player
                            useController = false // Hide default controls for auto-play
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Mute/unmute button for auto-playing videos
                FloatingActionButton(
                    onClick = onMuteToggle,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(40.dp),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                // Show placeholder with play button
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        FloatingActionButton(onClick = onPlayClick) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Video ${index + 1}")
                        if (isPreloaded) {
                            Text(
                                "Preloaded",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}