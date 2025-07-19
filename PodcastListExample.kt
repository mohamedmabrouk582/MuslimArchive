// PodcastListExample.kt
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import kotlinx.coroutines.flow.StateFlow

// Podcast data class implementing PreloadableItem
data class PodcastEpisode(
    override val id: String,
    val title: String,
    val description: String,
    override val url: String,
    val duration: Long, // in milliseconds
    val showName: String,
    val publishDate: String,
    val isDownloaded: Boolean = false,
    override val priority: Int = 50
) : PreloadableItem

// Podcast-specific ViewModel
class PodcastListViewModel : ViewModel() {
    private var preloadingHelper: PreloadingHelper? = null
    private var _currentPlayingIndex by mutableIntStateOf(-1)
    private var _isPlaying by mutableStateOf(false)
    
    val currentPlayingIndex: Int get() = _currentPlayingIndex
    val isPlaying: Boolean get() = _isPlaying
    
    // Sample podcast episodes
    private val _episodes = mutableStateListOf<PodcastEpisode>()
    val episodes: List<PodcastEpisode> = _episodes
    
    // Expose preload statuses
    val preloadStatuses: StateFlow<Map<String, PreloadItemStatus>>?
        get() = preloadingHelper?.itemStatuses
    
    // Expose player for audio visualization if needed
    val player get() = preloadingHelper?.getPlayer()
    
    init {
        // Initialize with sample podcast episodes
        _episodes.addAll(
            listOf(
                PodcastEpisode(
                    id = "ep1",
                    title = "The Future of AI in Mobile Development",
                    description = "A deep dive into how artificial intelligence is reshaping mobile app development, with expert insights and real-world examples.",
                    url = "https://www.soundjay.com/misc/sounds/bell-ringing-05.wav", // Sample audio
                    duration = 2340000L, // 39 minutes
                    showName = "Tech Talk Daily",
                    publishDate = "2024-01-15",
                    priority = 95
                ),
                PodcastEpisode(
                    id = "ep2",
                    title = "Building Scalable Android Architecture",
                    description = "Learn about clean architecture, MVVM patterns, and best practices for building maintainable Android applications.",
                    url = "https://www.soundjay.com/misc/sounds/clock-chimes-01.wav",
                    duration = 2880000L, // 48 minutes
                    showName = "Android Developers",
                    publishDate = "2024-01-12",
                    priority = 90
                ),
                PodcastEpisode(
                    id = "ep3",
                    title = "Jetpack Compose Performance Tips",
                    description = "Optimization techniques for Jetpack Compose applications, including recomposition strategies and state management.",
                    url = "https://www.soundjay.com/misc/sounds/typewriter-key-01.wav",
                    duration = 1920000L, // 32 minutes
                    showName = "Compose Corner",
                    publishDate = "2024-01-10",
                    priority = 85
                ),
                PodcastEpisode(
                    id = "ep4",
                    title = "Media3 and ExoPlayer Deep Dive",
                    description = "Everything you need to know about implementing video and audio playback in Android apps using Media3.",
                    url = "https://www.soundjay.com/misc/sounds/fail-buzzer-02.wav",
                    duration = 3600000L, // 60 minutes
                    showName = "Media Masters",
                    publishDate = "2024-01-08",
                    priority = 80
                ),
                PodcastEpisode(
                    id = "ep5",
                    title = "Kotlin Multiplatform in Practice",
                    description = "Real-world experiences with Kotlin Multiplatform Mobile, sharing code between Android and iOS.",
                    url = "https://www.soundjay.com/misc/sounds/magic-chime-02.wav",
                    duration = 2160000L, // 36 minutes
                    showName = "Kotlin Talks",
                    publishDate = "2024-01-05",
                    priority = 75
                )
            )
        )
    }
    
    fun initializePreloading(context: Context) {
        if (preloadingHelper == null) {
            // Audio-optimized configuration
            val config = PreloadConfig(
                preloadDurationUs = 30_000_000L, // 30 seconds for audio
                maxPreloadItems = 8, // More items for audio (smaller files)
                preloadRange = 4, // Wider range for audio content
                minBufferMs = 5000, // Shorter buffer for audio
                maxBufferMs = 15000,
                bufferForPlaybackMs = 1000,
                bufferForPlaybackAfterRebufferMs = 2000
            )
            
            // Audio-specific preload listener
            val listener = object : PreloadListener {
                override fun onPreloadStarted(itemId: String) {
                    println("Audio preload started: $itemId")
                }
                
                override fun onPreloadCompleted(itemId: String) {
                    println("Audio preload completed: $itemId")
                }
                
                override fun onPreloadError(itemId: String, error: String) {
                    println("Audio preload error for $itemId: $error")
                }
            }
            
            // Initialize helper
            preloadingHelper = context.createPreloadingHelper(config, listener)
            
            // Add player listener for audio playback
            preloadingHelper?.getPlayer()?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    _isPlaying = playbackState == Player.STATE_READY && 
                               preloadingHelper?.getPlayer()?.playWhenReady == true
                    
                    when (playbackState) {
                        Player.STATE_ENDED -> {
                            _isPlaying = false
                            playNext()
                        }
                    }
                }
                
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying = isPlaying
                }
            })
            
            // Start preloading initial episodes
            preloadingHelper?.addAll(episodes.take(5))
        }
    }
    
    fun playEpisode(index: Int) {
        if (index < episodes.size) {
            _currentPlayingIndex = index
            val episode = episodes[index]
            preloadingHelper?.playItem(episode)
        }
    }
    
    fun pauseEpisode() {
        preloadingHelper?.getPlayer()?.pause()
    }
    
    fun resumeEpisode() {
        preloadingHelper?.getPlayer()?.play()
    }
    
    fun seekTo(positionMs: Long) {
        preloadingHelper?.getPlayer()?.seekTo(positionMs)
    }
    
    fun getCurrentPosition(): Long {
        return preloadingHelper?.getPlayer()?.currentPosition ?: 0L
    }
    
    fun getDuration(): Long {
        return preloadingHelper?.getPlayer()?.duration ?: 0L
    }
    
    private fun playNext() {
        val nextIndex = _currentPlayingIndex + 1
        if (nextIndex < episodes.size) {
            playEpisode(nextIndex)
        }
    }
    
    fun playPrevious() {
        val previousIndex = _currentPlayingIndex - 1
        if (previousIndex >= 0) {
            playEpisode(previousIndex)
        }
    }
    
    fun updateVisibleRange(firstVisibleIndex: Int, lastVisibleIndex: Int) {
        preloadingHelper?.updateForLazyList(
            currentPlayingIndex = _currentPlayingIndex,
            items = episodes,
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
fun PodcastListScreen(
    viewModel: PodcastListViewModel = viewModel()
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
        // Header
        PodcastHeader(
            stats = viewModel.getPreloadStats(),
            currentEpisode = if (viewModel.currentPlayingIndex >= 0) 
                viewModel.episodes.getOrNull(viewModel.currentPlayingIndex) else null
        )
        
        // Currently playing episode (sticky)
        if (viewModel.currentPlayingIndex >= 0) {
            NowPlayingBar(
                episode = viewModel.episodes[viewModel.currentPlayingIndex],
                isPlaying = viewModel.isPlaying,
                onPlayPause = {
                    if (viewModel.isPlaying) {
                        viewModel.pauseEpisode()
                    } else {
                        viewModel.resumeEpisode()
                    }
                },
                onPrevious = viewModel::playPrevious,
                onNext = { /* viewModel.playNext() - handled automatically */ },
                currentPosition = 0L, // You'd get this from the player
                duration = viewModel.getDuration()
            )
        }
        
        // Episode list
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(
                items = viewModel.episodes,
                key = { _, episode -> episode.id }
            ) { index, episode ->
                PodcastEpisodeCard(
                    episode = episode,
                    index = index,
                    isCurrentlyPlaying = viewModel.currentPlayingIndex == index,
                    isPlaying = viewModel.isPlaying && viewModel.currentPlayingIndex == index,
                    onPlayClick = { viewModel.playEpisode(index) },
                    preloadStatus = preloadStatuses[episode.id]
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastHeader(
    stats: PreloadStats?,
    currentEpisode: PodcastEpisode?
) {
    TopAppBar(
        title = {
            Column {
                Text("Podcast Player")
                if (stats != null) {
                    Text(
                        text = "Cached: ${stats.preloadedItems}/${stats.totalItems}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = { /* Settings */ }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    )
}

@Composable
fun NowPlayingBar(
    episode: PodcastEpisode,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    currentPosition: Long,
    duration: Long
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art placeholder
                Surface(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Episode info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = episode.showName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Playback controls
                Row {
                    IconButton(onClick = onPrevious) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                    }
                    
                    IconButton(onClick = onPlayPause) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play"
                        )
                    }
                    
                    IconButton(onClick = onNext) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next")
                    }
                }
            }
            
            // Progress bar
            if (duration > 0) {
                val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastEpisodeCard(
    episode: PodcastEpisode,
    index: Int,
    isCurrentlyPlaying: Boolean,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    preloadStatus: PreloadItemStatus?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrentlyPlaying) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyPlaying) 
                MaterialTheme.colorScheme.primaryContainer
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Episode header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Play button
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = if (isCurrentlyPlaying) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Episode details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = episode.showName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = episode.publishDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // More options
                IconButton(onClick = { /* Show options */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Description
            Text(
                text = episode.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Footer with metadata and preload status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Duration and priority
                Column {
                    Text(
                        text = formatDuration(episode.duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Priority: ${episode.priority}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Preload status
                preloadStatus?.let { status ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val statusIcon = when {
                            status.error != null -> Icons.Default.Error
                            status.isPreloaded -> Icons.Default.CloudDone
                            status.isLoading -> Icons.Default.CloudDownload
                            else -> Icons.Default.CloudQueue
                        }
                        
                        val statusColor = when {
                            status.error != null -> MaterialTheme.colorScheme.error
                            status.isPreloaded -> MaterialTheme.colorScheme.primary
                            status.isLoading -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = when {
                                status.error != null -> "Error"
                                status.isPreloaded -> "Cached"
                                status.isLoading -> "Loading"
                                else -> "Pending"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor
                        )
                    }
                }
            }
            
            // Progress indicator for loading
            preloadStatus?.let { status ->
                if (status.isLoading && status.progress > 0f) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { status.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

// Utility function to format duration
fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}