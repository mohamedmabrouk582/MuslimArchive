// SimpleFeedScreen.kt
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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

// Simple ViewModel
class SimpleFeedViewModel : ViewModel() {
    private var preloadManager: SimplePreloadManager? = null
    
    // Sample content
    val feedItems = listOf(
        MediaContent("1", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
        MediaContent("2", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"),
        MediaContent("3", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"),
        MediaContent("4", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"),
        MediaContent("5", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4")
    )
    
    private var _currentPlayingIndex by mutableIntStateOf(-1)
    val currentPlayingIndex: Int get() = _currentPlayingIndex
    
    val preloadStatus: StateFlow<Map<String, PreloadStatus>>?
        get() = preloadManager?.preloadStatus
    
    val player get() = preloadManager?.getPlayer()
    
    fun initialize(context: Context) {
        if (preloadManager == null) {
            preloadManager = SimplePreloadManager(context).apply {
                initialize()
                // Preload first few items
                preloadItems(feedItems.take(3))
            }
        }
    }
    
    fun playItem(index: Int) {
        if (index != _currentPlayingIndex) {
            _currentPlayingIndex = index
            preloadManager?.playItem(feedItems[index])
        }
    }
    
    fun updateVisibleRange(firstVisible: Int, lastVisible: Int) {
        // Preload items around visible range
        val startIndex = maxOf(0, firstVisible - 1)
        val endIndex = minOf(feedItems.size - 1, lastVisible + 2)
        val itemsToPreload = feedItems.subList(startIndex, endIndex + 1)
        preloadManager?.preloadItems(itemsToPreload)
    }
    
    override fun onCleared() {
        super.onCleared()
        preloadManager?.release()
    }
}

@Composable
fun SimpleFeedScreen(
    viewModel: SimpleFeedViewModel = viewModel()
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val preloadStatuses by (viewModel.preloadStatus?.collectAsState() ?: remember { mutableStateOf(emptyMap()) })
    
    // Initialize
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }
    
    // Track visible items
    LaunchedEffect(listState) {
        snapshotFlow {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            if (visibleItems.isNotEmpty()) {
                visibleItems.first().index to visibleItems.last().index
            } else {
                0 to 0
            }
        }.collect { (first, last) ->
            viewModel.updateVisibleRange(first, last)
        }
    }
    
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(viewModel.feedItems) { index, item ->
            SimpleVideoCard(
                item = item,
                index = index,
                isPlaying = viewModel.currentPlayingIndex == index,
                isPreloaded = preloadStatuses[item.id]?.isPreloaded == true,
                onPlayClick = { viewModel.playItem(index) },
                player = viewModel.player
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleVideoCard(
    item: MediaContent,
    index: Int,
    isPlaying: Boolean,
    isPreloaded: Boolean,
    onPlayClick: () -> Unit,
    player: androidx.media3.exoplayer.ExoPlayer?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isPlaying && player != null) {
                // Show player when playing
                AndroidView(
                    factory = { context ->
                        PlayerView(context).apply {
                            this.player = player
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
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