// VideoListWithPreloading.kt
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch

// Data class for video items
data class VideoItem(
    val id: String,
    val title: String,
    val url: String,
    val thumbnailUrl: String? = null
)

// ViewModel to manage preloading and playback
class VideoListViewModel : ViewModel() {
    private var preloadManager: DefaultPreloadManager? = null
    private var currentPlayer: ExoPlayer? = null
    private var _currentPlayingIndex by mutableIntStateOf(-1)
    
    val currentPlayingIndex: Int get() = _currentPlayingIndex
    
    // Expose player for UI
    val player: ExoPlayer? get() = currentPlayer
    
    // Sample video data
    private val _videos = mutableStateListOf<VideoItem>()
    val videos: List<VideoItem> = _videos
    
    init {
        // Add sample videos
        _videos.addAll(
            listOf(
                VideoItem(
                    id = "1",
                    title = "Sample Video 1",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                ),
                VideoItem(
                    id = "2", 
                    title = "Sample Video 2",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
                ),
                VideoItem(
                    id = "3",
                    title = "Sample Video 3", 
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
                ),
                VideoItem(
                    id = "4",
                    title = "Sample Video 4",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
                ),
                VideoItem(
                    id = "5",
                    title = "Sample Video 5",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"
                )
            )
        )
    }
    
    fun initializePreloadManager(context: Context) {
        if (preloadManager == null) {
            // Create DefaultPreloadManager with simplified builder (Media3 1.5.0+)
            val preloadManagerBuilder = DefaultPreloadManager.Builder(context)
                .setTargetPreloadStatusControl(
                    TargetPreloadStatusControl.PreloadStatusControl { targetPreloadStatus ->
                        // Preload 5 seconds of content
                        targetPreloadStatus.buildUpon()
                            .setPreloadDurationUs(5_000_000) // 5 seconds in microseconds
                            .build()
                    }
                )
            
            preloadManager = preloadManagerBuilder.build()
            currentPlayer = preloadManagerBuilder.buildExoPlayer()
            
            // Setup player configuration
            currentPlayer?.apply {
                // Enable playlist preloading
                preloadConfiguration = androidx.media3.exoplayer.PreloadConfiguration(
                    targetPreloadDurationUs = 5_000_000L // 5 seconds
                )
                
                // Add player listener
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                // Video is ready to play
                            }
                            Player.STATE_BUFFERING -> {
                                // Video is buffering
                            }
                            Player.STATE_ENDED -> {
                                // Video playback ended
                                playNext()
                            }
                        }
                    }
                })
            }
            
            // Preload initial videos
            preloadVideos(context)
        }
    }
    
    private fun preloadVideos(context: Context) {
        viewModelScope.launch {
            preloadManager?.let { manager ->
                val dataSourceFactory = DefaultDataSource.Factory(context)
                val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
                
                videos.forEachIndexed { index, video ->
                    val mediaItem = MediaItem.fromUri(video.url)
                    val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)
                    
                    // Add to preload manager with ranking based on position
                    // Videos closer to current position get higher priority
                    val rankingData = when (index) {
                        in 0..2 -> 100 - index // Higher priority for first few videos
                        else -> 50 // Lower priority for other videos
                    }
                    
                    manager.add(mediaSource, rankingData)
                }
            }
        }
    }
    
    fun playVideo(index: Int) {
        if (index != _currentPlayingIndex && index < videos.size) {
            _currentPlayingIndex = index
            
            currentPlayer?.let { player ->
                val mediaItem = MediaItem.fromUri(videos[index].url)
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
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
        }
    }
    
    fun updateVisibleRange(firstVisibleIndex: Int, lastVisibleIndex: Int) {
        // Update preloading priorities based on visible range
        viewModelScope.launch {
            preloadManager?.let { manager ->
                videos.forEachIndexed { index, video ->
                    val priority = when {
                        index in firstVisibleIndex..lastVisibleIndex -> 100
                        index in (firstVisibleIndex - 2)..(lastVisibleIndex + 2) -> 75
                        else -> 25
                    }
                    
                    // In a real implementation, you might want to:
                    // 1. Remove low-priority items from preload manager
                    // 2. Add high-priority items to preload manager
                    // 3. Update the ranking of existing items
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        currentPlayer?.release()
        preloadManager?.release()
    }
}

@Composable
fun VideoListScreen(
    viewModel: VideoListViewModel = viewModel()
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    
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
        // Header
        TopAppBar(
            title = { Text("Video List with Preloading") }
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
                VideoItemCard(
                    video = video,
                    index = index,
                    isPlaying = viewModel.currentPlayingIndex == index,
                    onPlayClick = { viewModel.playVideo(index) },
                    onPauseClick = { viewModel.pauseVideo() },
                    player = viewModel.player
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoItemCard(
    video: VideoItem,
    index: Int,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    player: ExoPlayer?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
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
                                text = "Video ${index + 1}",
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
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Index: $index",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
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