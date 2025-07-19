// AdvancedVideoListWithPreloading.kt
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Enhanced data class for video items
data class AdvancedVideoItem(
    val id: String,
    val title: String,
    val url: String,
    val thumbnailUrl: String? = null,
    val duration: Long = 0L,
    val description: String? = null
)

// Preload status tracking
data class PreloadStatus(
    val isPreloaded: Boolean = false,
    val preloadProgress: Float = 0f,
    val error: String? = null
)

// Advanced ViewModel with better preloading management
class AdvancedVideoListViewModel : ViewModel() {
    private var preloadManager: DefaultPreloadManager? = null
    private var currentPlayer: ExoPlayer? = null
    private var _currentPlayingIndex by mutableIntStateOf(-1)
    
    val currentPlayingIndex: Int get() = _currentPlayingIndex
    val player: ExoPlayer? get() = currentPlayer
    
    // Track preload status for each video
    private val _preloadStatuses = MutableStateFlow<Map<String, PreloadStatus>>(emptyMap())
    val preloadStatuses: StateFlow<Map<String, PreloadStatus>> = _preloadStatuses.asStateFlow()
    
    // Sample video data with more realistic content
    private val _videos = mutableStateListOf<AdvancedVideoItem>()
    val videos: List<AdvancedVideoItem> = _videos
    
    // Preloading configuration
    private val preloadRange = 3 // Number of videos to preload ahead and behind
    private val highPriorityPreloadDuration = 10_000_000L // 10 seconds
    private val lowPriorityPreloadDuration = 3_000_000L // 3 seconds
    
    init {
        // Add sample videos with different lengths and content
        _videos.addAll(
            listOf(
                AdvancedVideoItem(
                    id = "1",
                    title = "Big Buck Bunny",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    duration = 596000L,
                    description = "A classic open source animation"
                ),
                AdvancedVideoItem(
                    id = "2", 
                    title = "Elephant Dream",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    duration = 653000L,
                    description = "Surreal open source animation"
                ),
                AdvancedVideoItem(
                    id = "3",
                    title = "For Bigger Blazes",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    duration = 15000L,
                    description = "A short action sequence"
                ),
                AdvancedVideoItem(
                    id = "4",
                    title = "For Bigger Escapes",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                    duration = 15000L,
                    description = "Another short action sequence"
                ),
                AdvancedVideoItem(
                    id = "5",
                    title = "For Bigger Fun",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                    duration = 60000L,
                    description = "Fun with friends"
                ),
                AdvancedVideoItem(
                    id = "6",
                    title = "For Bigger Joyrides",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
                    duration = 15000L,
                    description = "Racing excitement"
                ),
                AdvancedVideoItem(
                    id = "7",
                    title = "For Bigger Meltdowns",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
                    duration = 15000L,
                    description = "Action-packed sequence"
                ),
                AdvancedVideoItem(
                    id = "8",
                    title = "Sintel",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                    duration = 888000L,
                    description = "Beautiful fantasy short film"
                )
            )
        )
    }
    
    fun initializePreloadManager(context: Context) {
        if (preloadManager == null) {
            // Create custom components for better control
            val bandwidthMeter = DefaultBandwidthMeter.Builder(context).build()
            val trackSelector = DefaultTrackSelector(context)
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs= */ 15000,
                    /* maxBufferMs= */ 50000,
                    /* bufferForPlaybackMs= */ 2500,
                    /* bufferForPlaybackAfterRebufferMs= */ 5000
                )
                .build()
            
            // Create DefaultPreloadManager with custom configuration
            val preloadManagerBuilder = DefaultPreloadManager.Builder(context)
                .setBandwidthMeter(bandwidthMeter)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .setTargetPreloadStatusControl(
                    TargetPreloadStatusControl.PreloadStatusControl { targetPreloadStatus ->
                        targetPreloadStatus.buildUpon()
                            .setPreloadDurationUs(highPriorityPreloadDuration)
                            .build()
                    }
                )
            
            preloadManager = preloadManagerBuilder.build()
            currentPlayer = preloadManagerBuilder.buildExoPlayer()
            
            // Configure player
            currentPlayer?.apply {
                // Enable playlist preloading
                preloadConfiguration = androidx.media3.exoplayer.PreloadConfiguration(
                    targetPreloadDurationUs = highPriorityPreloadDuration
                )
                
                // Add comprehensive player listener
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                // Video is ready to play
                                updateCurrentVideoPreloadStatus(true, 1.0f)
                            }
                            Player.STATE_BUFFERING -> {
                                // Video is buffering
                                updateCurrentVideoPreloadStatus(false, 0.5f)
                            }
                            Player.STATE_ENDED -> {
                                // Video playback ended, auto-play next
                                playNext()
                            }
                            Player.STATE_IDLE -> {
                                // Player is idle
                            }
                        }
                    }
                    
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        // Handle playback error
                        updateCurrentVideoPreloadStatus(false, 0f, error.message)
                    }
                })
            }
            
            // Start initial preloading
            preloadInitialVideos(context)
        }
    }
    
    private fun preloadInitialVideos(context: Context) {
        viewModelScope.launch {
            preloadManager?.let { manager ->
                val dataSourceFactory = DefaultDataSource.Factory(context)
                val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
                
                // Preload first few videos with high priority
                videos.take(preloadRange).forEachIndexed { index, video ->
                    val mediaItem = MediaItem.fromUri(video.url)
                    val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)
                    
                    // Higher ranking for videos closer to the beginning
                    val rankingData = 1000 - index
                    
                    manager.add(mediaSource, rankingData)
                    updatePreloadStatus(video.id, PreloadStatus(isPreloaded = false, preloadProgress = 0.1f))
                }
            }
        }
    }
    
    fun playVideo(index: Int) {
        if (index != _currentPlayingIndex && index < videos.size) {
            val previousIndex = _currentPlayingIndex
            _currentPlayingIndex = index
            
            currentPlayer?.let { player ->
                val mediaItem = MediaItem.fromUri(videos[index].url)
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
            }
            
            // Update preloading for new position
            updatePreloadingForCurrentPosition(index)
        }
    }
    
    private fun updatePreloadingForCurrentPosition(currentIndex: Int) {
        viewModelScope.launch {
            preloadManager?.let { manager ->
                val context = currentPlayer?.applicationLooper?.let { 
                    // Get context from somewhere accessible
                    // In a real app, you'd pass this properly
                    return@launch
                } ?: return@launch
                
                // Calculate which videos to preload
                val startIndex = maxOf(0, currentIndex - preloadRange)
                val endIndex = minOf(videos.size - 1, currentIndex + preloadRange)
                
                // Preload videos in range
                for (i in startIndex..endIndex) {
                    if (i != currentIndex) {
                        val video = videos[i]
                        val distance = kotlin.math.abs(i - currentIndex)
                        val priority = when (distance) {
                            1 -> 900 // Next/previous video - highest priority
                            2 -> 800 // Two videos away - high priority
                            else -> 700 // Further away - lower priority
                        }
                        
                        // Add to preload manager if not already added
                        // Note: In a real implementation, you'd track which videos are already added
                        updatePreloadStatus(video.id, PreloadStatus(isPreloaded = false, preloadProgress = 0.2f))
                    }
                }
            }
        }
    }
    
    fun pauseVideo() {
        currentPlayer?.pause()
    }
    
    fun resumeVideo() {
        currentPlayer?.play()
    }
    
    private fun playNext() {
        val nextIndex = _currentPlayingIndex + 1
        if (nextIndex < videos.size) {
            playVideo(nextIndex)
        } else {
            // Reached end of playlist
            _currentPlayingIndex = -1
        }
    }
    
    fun updateVisibleRange(firstVisibleIndex: Int, lastVisibleIndex: Int) {
        // Optimize preloading based on visible range
        viewModelScope.launch {
            // Update preload priorities based on visibility
            videos.forEachIndexed { index, video ->
                val priority = when {
                    index in firstVisibleIndex..lastVisibleIndex -> 1000 // Visible - highest priority
                    index in (firstVisibleIndex - 1)..(lastVisibleIndex + 1) -> 900 // Adjacent to visible
                    index in (firstVisibleIndex - 2)..(lastVisibleIndex + 2) -> 800 // Near visible
                    else -> 600 // Lower priority
                }
                
                // In a real implementation, you would update the preload manager priorities here
            }
        }
    }
    
    private fun updatePreloadStatus(videoId: String, status: PreloadStatus) {
        val currentStatuses = _preloadStatuses.value.toMutableMap()
        currentStatuses[videoId] = status
        _preloadStatuses.value = currentStatuses
    }
    
    private fun updateCurrentVideoPreloadStatus(isReady: Boolean, progress: Float, error: String? = null) {
        if (_currentPlayingIndex >= 0 && _currentPlayingIndex < videos.size) {
            val videoId = videos[_currentPlayingIndex].id
            updatePreloadStatus(videoId, PreloadStatus(isReady, progress, error))
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        currentPlayer?.release()
        preloadManager?.release()
    }
}

@Composable
fun AdvancedVideoListScreen(
    viewModel: AdvancedVideoListViewModel = viewModel()
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val preloadStatuses by viewModel.preloadStatuses.collectAsState()
    
    // Initialize preload manager
    LaunchedEffect(Unit) {
        viewModel.initializePreloadManager(context)
    }
    
    // Track visible items for preloading optimization
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
        // Header with preload status
        TopAppBar(
            title = { 
                Column {
                    Text("Advanced Video List")
                    Text(
                        text = "Preloaded: ${preloadStatuses.count { it.value.isPreloaded }}/${preloadStatuses.size}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
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
                AdvancedVideoItemCard(
                    video = video,
                    index = index,
                    isPlaying = viewModel.currentPlayingIndex == index,
                    onPlayClick = { viewModel.playVideo(index) },
                    onPauseClick = { viewModel.pauseVideo() },
                    player = viewModel.player,
                    preloadStatus = preloadStatuses[video.id] ?: PreloadStatus()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedVideoItemCard(
    video: AdvancedVideoItem,
    index: Int,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    player: ExoPlayer?,
    preloadStatus: PreloadStatus
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Video player area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                if (isPlaying && player != null) {
                    // Show actual video player when playing
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
                    // Show thumbnail or placeholder when not playing
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
                                contentDescription = "Play video",
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = video.title,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    // Play button overlay
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
                if (preloadStatus.preloadProgress > 0f && !isPlaying) {
                    LinearProgressIndicator(
                        progress = { preloadStatus.preloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        color = if (preloadStatus.isPreloaded) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            // Video info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
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
                        maxLines = 2
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Duration: ${video.duration / 1000}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (preloadStatus.isPreloaded) "Preloaded" else "Loading...",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (preloadStatus.isPreloaded) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (preloadStatus.error != null) {
                                Text(
                                    text = " (Error)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    
                    if (isPlaying) {
                        Row {
                            IconButton(onClick = onPauseClick) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Pause"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}