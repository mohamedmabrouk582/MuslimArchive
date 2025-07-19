// VideoListWithGenericHelper.kt
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch

// Video data class implementing PreloadableItem
data class Video(
    override val id: String,
    val title: String,
    override val url: String,
    val description: String? = null,
    val duration: Long = 0L,
    override val priority: Int = 50
) : PreloadableItem

// ViewModel using the generic helper
class VideoListWithHelperViewModel : ViewModel() {
    private var preloadingHelper: PreloadingHelper? = null
    private var _currentPlayingIndex by mutableIntStateOf(-1)
    
    val currentPlayingIndex: Int get() = _currentPlayingIndex
    
    // Sample videos
    private val _videos = mutableStateListOf<Video>()
    val videos: List<Video> = _videos
    
    // Expose preload statuses
    val preloadStatuses: StateFlow<Map<String, PreloadItemStatus>>?
        get() = preloadingHelper?.itemStatuses
    
    // Expose player
    val player get() = preloadingHelper?.getPlayer()
    
    init {
        // Initialize with sample videos
        _videos.addAll(
            listOf(
                Video(
                    id = "1",
                    title = "Big Buck Bunny",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    description = "Classic open source animation",
                    duration = 596000L,
                    priority = 90
                ),
                Video(
                    id = "2",
                    title = "Elephant Dream",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    description = "Surreal animation",
                    duration = 653000L,
                    priority = 85
                ),
                Video(
                    id = "3",
                    title = "For Bigger Blazes",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    description = "Action sequence",
                    duration = 15000L,
                    priority = 80
                ),
                Video(
                    id = "4",
                    title = "For Bigger Escapes",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                    description = "Another action sequence",
                    duration = 15000L,
                    priority = 75
                ),
                Video(
                    id = "5",
                    title = "Sintel",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                    description = "Beautiful fantasy film",
                    duration = 888000L,
                    priority = 70
                )
            )
        )
    }
    
    fun initializePreloading(context: Context) {
        if (preloadingHelper == null) {
            // Create custom configuration
            val config = PreloadConfig(
                preloadDurationUs = 8_000_000L, // 8 seconds
                maxPreloadItems = 5,
                preloadRange = 2,
                minBufferMs = 10000,
                maxBufferMs = 30000,
                bufferForPlaybackMs = 2000,
                bufferForPlaybackAfterRebufferMs = 3000
            )
            
            // Create listener for preload events
            val listener = object : PreloadListener {
                override fun onPreloadStarted(itemId: String) {
                    println("Preload started for: $itemId")
                }
                
                override fun onPreloadCompleted(itemId: String) {
                    println("Preload completed for: $itemId")
                }
                
                override fun onPreloadError(itemId: String, error: String) {
                    println("Preload error for $itemId: $error")
                }
            }
            
            // Initialize helper
            preloadingHelper = context.createPreloadingHelper(config, listener)
            
            // Add player listener
            preloadingHelper?.getPlayer()?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_ENDED -> playNext()
                    }
                }
            })
            
            // Start preloading initial videos
            preloadingHelper?.addAll(videos.take(3))
        }
    }
    
    fun playVideo(index: Int) {
        if (index != _currentPlayingIndex && index < videos.size) {
            _currentPlayingIndex = index
            val video = videos[index]
            preloadingHelper?.playItem(video)
        }
    }
    
    fun pauseVideo() {
        preloadingHelper?.getPlayer()?.pause()
    }
    
    fun resumeVideo() {
        preloadingHelper?.getPlayer()?.play()
    }
    
    private fun playNext() {
        val nextIndex = _currentPlayingIndex + 1
        if (nextIndex < videos.size) {
            playVideo(nextIndex)
        }
    }
    
    fun updateVisibleRange(firstVisibleIndex: Int, lastVisibleIndex: Int) {
        preloadingHelper?.updateForLazyList(
            currentPlayingIndex = _currentPlayingIndex,
            items = videos,
            firstVisibleIndex = firstVisibleIndex,
            lastVisibleIndex = lastVisibleIndex
        )
    }
    
    fun getPreloadStats(): PreloadStats? {
        return preloadingHelper?.getStats()
    }
    
    override fun onCleared() {
        super.onCleared()
        preloadingHelper?.release()
    }
}

@Composable
fun VideoListWithHelperScreen(
    viewModel: VideoListWithHelperViewModel = viewModel()
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val preloadStatuses by (viewModel.preloadStatuses?.collectAsState() ?: remember { mutableStateOf(emptyMap()) })
    
    // Initialize preloading
    LaunchedEffect(Unit) {
        viewModel.initializePreloading(context)
    }
    
    // Track visible items
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
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header with stats
        PreloadStatsHeader(
            stats = viewModel.getPreloadStats(),
            preloadStatuses = preloadStatuses
        )
        
        // Video list
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(
                items = viewModel.videos,
                key = { _, video -> video.id }
            ) { index, video ->
                VideoCard(
                    video = video,
                    index = index,
                    isPlaying = viewModel.currentPlayingIndex == index,
                    onPlayClick = { viewModel.playVideo(index) },
                    onPauseClick = { viewModel.pauseVideo() },
                    player = viewModel.player,
                    preloadStatus = preloadStatuses[video.id]
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreloadStatsHeader(
    stats: PreloadStats?,
    preloadStatuses: Map<String, PreloadItemStatus>
) {
    TopAppBar(
        title = {
            Column {
                Text("Generic Helper Example")
                if (stats != null) {
                    Text(
                        text = "Preloaded: ${stats.preloadedItems}/${stats.totalItems} " +
                                "(${(stats.preloadRatio * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        actions = {
            if (stats != null) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Loading: ${stats.loadingItems}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Errors: ${stats.errorItems}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (stats.errorItems > 0) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCard(
    video: Video,
    index: Int,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    player: androidx.media3.exoplayer.ExoPlayer?,
    preloadStatus: PreloadItemStatus?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Video player area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                if (isPlaying && player != null) {
                    AndroidView(
                        factory = { context ->
                            PlayerView(context).apply {
                                useController = true
                                this.player = player
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Placeholder
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = video.title,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    // Play button
                    FloatingActionButton(
                        onClick = onPlayClick,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play"
                        )
                    }
                }
                
                // Preload status indicator
                preloadStatus?.let { status ->
                    if (status.isLoading || status.progress > 0f) {
                        LinearProgressIndicator(
                            progress = { status.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                            color = when {
                                status.error != null -> MaterialTheme.colorScheme.error
                                status.isPreloaded -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.secondary
                            }
                        )
                    }
                }
            }
            
            // Video info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        if (video.description != null) {
                            Text(
                                text = video.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                    
                    if (isPlaying) {
                        IconButton(onClick = onPauseClick) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause"
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Status row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Priority: ${video.priority}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    preloadStatus?.let { status ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val statusText = when {
                                status.error != null -> "Error"
                                status.isPreloaded -> "Preloaded"
                                status.isLoading -> "Loading..."
                                else -> "Pending"
                            }
                            
                            val statusColor = when {
                                status.error != null -> MaterialTheme.colorScheme.error
                                status.isPreloaded -> MaterialTheme.colorScheme.primary
                                status.isLoading -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = statusColor
                            )
                        }
                    }
                }
            }
        }
    }
}