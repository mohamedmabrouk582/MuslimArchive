// NewsFeedWithAutoPlay.kt
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// News feed item data classes
data class VideoFeedItem(
    override val id: String,
    override val url: String,
    override val title: String,
    override val description: String?,
    override val authorName: String?,
    override val publishTime: Long,
    override val duration: Long?,
    override val thumbnailUrl: String?,
    val likes: Int = 0,
    val comments: Int = 0,
    val shares: Int = 0,
    override val priority: Int = 80
) : NewsFeedItem {
    override val contentType = FeedContentType.VIDEO
}

data class AudioFeedItem(
    override val id: String,
    override val url: String,
    override val title: String,
    override val description: String?,
    override val authorName: String?,
    override val publishTime: Long,
    override val duration: Long?,
    override val thumbnailUrl: String?,
    val podcastName: String? = null,
    val likes: Int = 0,
    val comments: Int = 0,
    override val priority: Int = 70
) : NewsFeedItem {
    override val contentType = FeedContentType.AUDIO
}

data class ImageNewsFeedItem(
    override val id: String,
    override val title: String,
    override val description: String?,
    override val authorName: String?,
    override val publishTime: Long,
    override val thumbnailUrl: String?,
    val imageUrls: List<String> = emptyList(),
    val newsCategory: String? = null,
    val readTime: String? = null,
    val likes: Int = 0,
    val comments: Int = 0,
    val shares: Int = 0,
    override val priority: Int = 60
) : NewsFeedItem {
    override val contentType = FeedContentType.IMAGE_NEWS
    override val url: String = "" // Not applicable for image news
    override val duration: Long? = null
}

data class TextNewsFeedItem(
    override val id: String,
    override val title: String,
    override val description: String?,
    override val authorName: String?,
    override val publishTime: Long,
    val content: String,
    val newsCategory: String? = null,
    val readTime: String? = null,
    val likes: Int = 0,
    val comments: Int = 0,
    val shares: Int = 0,
    override val priority: Int = 50
) : NewsFeedItem {
    override val contentType = FeedContentType.TEXT_ONLY
    override val url: String = "" // Not applicable
    override val duration: Long? = null
    override val thumbnailUrl: String? = null
}

// ViewModel for news feed
class NewsFeedViewModel : ViewModel() {
    private var feedHelper: NewsFeedPreloadingHelper? = null
    private val _feedItems = mutableStateListOf<NewsFeedItem>()
    val feedItems: List<NewsFeedItem> = _feedItems
    
    // Current visibility tracking
    private val _visibleItems = mutableStateMapOf<String, Float>()
    
    // Expose states
    val itemStatuses: StateFlow<Map<String, PreloadItemStatus>>?
        get() = feedHelper?.itemStatuses
    
    val playbackStatuses: StateFlow<Map<String, FeedItemPlaybackStatus>>?
        get() = feedHelper?.playbackStatuses
    
    init {
        // Initialize with sample mixed content
        generateSampleFeed()
    }
    
    private fun generateSampleFeed() {
        _feedItems.addAll(
            listOf(
                // Video content
                VideoFeedItem(
                    id = "video_1",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    title = "Big Buck Bunny - Open Source Animation",
                    description = "A classic open source animation project showcasing the power of Blender.",
                    authorName = "Animation Studios",
                    publishTime = System.currentTimeMillis() - 3600000, // 1 hour ago
                    duration = 596000L,
                    thumbnailUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
                    likes = 1240,
                    comments = 89,
                    shares = 45,
                    priority = 90
                ),
                
                // Image news
                ImageNewsFeedItem(
                    id = "news_1",
                    title = "Tech Giant Announces New AI Features",
                    description = "Revolutionary AI capabilities coming to mobile devices this year with enhanced performance and privacy features.",
                    authorName = "Tech Reporter",
                    publishTime = System.currentTimeMillis() - 7200000, // 2 hours ago
                    thumbnailUrl = "https://via.placeholder.com/400x200/0080FF/FFFFFF?text=AI+News",
                    imageUrls = listOf(
                        "https://via.placeholder.com/400x200/0080FF/FFFFFF?text=AI+Feature+1",
                        "https://via.placeholder.com/400x200/00FF80/FFFFFF?text=AI+Feature+2"
                    ),
                    newsCategory = "Technology",
                    readTime = "3 min read",
                    likes = 892,
                    comments = 156,
                    shares = 78
                ),
                
                // Audio content
                AudioFeedItem(
                    id = "audio_1",
                    url = "https://www.soundjay.com/misc/sounds/bell-ringing-05.wav",
                    title = "The Future of Mobile Development",
                    description = "Industry experts discuss the latest trends in mobile app development and what to expect in the coming years.",
                    authorName = "Tech Podcast Network",
                    publishTime = System.currentTimeMillis() - 10800000, // 3 hours ago
                    duration = 2340000L, // 39 minutes
                    thumbnailUrl = "https://via.placeholder.com/200x200/FF8000/FFFFFF?text=Podcast",
                    podcastName = "Developer Insights",
                    likes = 567,
                    comments = 43
                ),
                
                // Video content
                VideoFeedItem(
                    id = "video_2",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    title = "Elephant's Dream - Surreal Animation",
                    description = "An artistic journey through a surreal world of imagination and creativity.",
                    authorName = "Indie Filmmakers",
                    publishTime = System.currentTimeMillis() - 14400000, // 4 hours ago
                    duration = 653000L,
                    thumbnailUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ElephantsDream.jpg",
                    likes = 789,
                    comments = 67,
                    shares = 23,
                    priority = 85
                ),
                
                // Text news
                TextNewsFeedItem(
                    id = "text_1",
                    title = "Breaking: New Privacy Regulations Announced",
                    description = "Government announces stricter data protection laws for tech companies.",
                    authorName = "News Desk",
                    publishTime = System.currentTimeMillis() - 18000000, // 5 hours ago
                    content = "In a landmark decision today, regulators have announced comprehensive new privacy laws that will reshape how technology companies handle user data. The regulations include stricter consent requirements, enhanced transparency measures, and significant penalties for violations...",
                    newsCategory = "Legal",
                    readTime = "5 min read",
                    likes = 1534,
                    comments = 298,
                    shares = 156
                ),
                
                // Audio content
                AudioFeedItem(
                    id = "audio_2",
                    url = "https://www.soundjay.com/misc/sounds/clock-chimes-01.wav",
                    title = "Building Scalable Android Apps",
                    description = "Expert tips and techniques for creating robust, maintainable Android applications.",
                    authorName = "Android Weekly",
                    publishTime = System.currentTimeMillis() - 21600000, // 6 hours ago
                    duration = 1800000L, // 30 minutes
                    thumbnailUrl = "https://via.placeholder.com/200x200/00FF80/FFFFFF?text=Android",
                    podcastName = "Code Talks",
                    likes = 423,
                    comments = 67
                ),
                
                // Image news
                ImageNewsFeedItem(
                    id = "news_2",
                    title = "Startup Raises $50M for Revolutionary Health App",
                    description = "Digital health company secures major funding to expand AI-powered diagnostic tools.",
                    authorName = "Business Reporter",
                    publishTime = System.currentTimeMillis() - 25200000, // 7 hours ago
                    thumbnailUrl = "https://via.placeholder.com/400x200/FF0080/FFFFFF?text=Health+Tech",
                    imageUrls = listOf(
                        "https://via.placeholder.com/400x200/FF0080/FFFFFF?text=Health+App",
                        "https://via.placeholder.com/400x200/8000FF/FFFFFF?text=AI+Diagnosis"
                    ),
                    newsCategory = "Business",
                    readTime = "4 min read",
                    likes = 678,
                    comments = 89,
                    shares = 34
                )
            )
        )
    }
    
    fun initializeFeedHelper(context: Context) {
        if (feedHelper == null) {
            // Configure for social media-like auto-play
            val autoPlayConfig = AutoPlayConfig(
                enableAutoPlay = true,
                autoPlayOnWifi = true,
                autoPlayOnMobile = false, // Conservative for mobile data
                minVisibilityPercentage = 0.6f, // 60% visibility like Facebook
                autoMuteVideos = true, // Start muted like most social platforms
                pauseWhenNotVisible = true,
                continuePlayingWhenScrolling = false,
                maxConcurrentPlayers = 1, // Only one video/audio at a time
                prioritizeVideoOverAudio = true // Videos take precedence
            )
            
            val config = NewsFeedPreloadConfig(
                videoPreloadDurationUs = 10_000_000L, // 10 seconds for smooth playback
                audioPreloadDurationUs = 20_000_000L, // 20 seconds for audio
                maxPreloadItems = 6,
                preloadRange = 3,
                videoBufferMs = 10000,
                audioBufferMs = 5000,
                autoPlayConfig = autoPlayConfig
            )
            
            val listener = object : PreloadListener {
                override fun onPreloadStarted(itemId: String) {
                    println("Feed preload started: $itemId")
                }
                
                override fun onPreloadCompleted(itemId: String) {
                    println("Feed preload completed: $itemId")
                }
                
                override fun onPreloadError(itemId: String, error: String) {
                    println("Feed preload error: $itemId - $error")
                }
            }
            
            feedHelper = context.createNewsFeedPreloadingHelper(config, listener)
            
            // Initial preload of media items
            val mediaItems = feedItems.filter { it.canAutoPlay }
            feedHelper?.addFeedItems(mediaItems)
        }
    }
    
    fun updateItemVisibility(itemId: String, visibilityPercentage: Float) {
        _visibleItems[itemId] = visibilityPercentage
        
        val item = feedItems.find { it.id == itemId }
        if (item != null) {
            feedHelper?.updateItemVisibility(itemId, visibilityPercentage, item)
        }
    }
    
    fun updateVisibleRange(firstVisibleIndex: Int, lastVisibleIndex: Int) {
        val visibleItems = (firstVisibleIndex..lastVisibleIndex).mapNotNull { index ->
            feedItems.getOrNull(index)?.let { index to it }
        }
        
        feedHelper?.updateForFeedPosition(visibleItems, feedItems)
    }
    
    fun playItem(item: NewsFeedItem) {
        feedHelper?.playItem(item)
    }
    
    fun pauseItem(itemId: String) {
        feedHelper?.pauseItem(itemId)
    }
    
    fun toggleMute(itemId: String) {
        feedHelper?.toggleMute(itemId)
    }
    
    fun likeItem(itemId: String) {
        // Implementation for like functionality
        // Update the item's like count
    }
    
    fun shareItem(itemId: String) {
        // Implementation for share functionality
    }
    
    fun commentOnItem(itemId: String) {
        // Implementation for comment functionality
    }
    
    override fun onCleared() {
        super.onCleared()
        feedHelper?.release()
    }
}

@Composable
fun NewsFeedScreen(
    viewModel: NewsFeedViewModel = viewModel()
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    
    val preloadStatuses by (viewModel.itemStatuses?.collectAsState() ?: remember { mutableStateOf(emptyMap()) })
    val playbackStatuses by (viewModel.playbackStatuses?.collectAsState() ?: remember { mutableStateOf(emptyMap()) })
    
    // Initialize feed helper
    LaunchedEffect(Unit) {
        viewModel.initializeFeedHelper(context)
    }
    
    // Track visible items and update visibility
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
                        itemTop >= 0 && itemBottom <= viewportHeight -> itemInfo.size // Fully visible
                        itemTop < 0 && itemBottom > 0 -> itemBottom // Top cut off
                        itemTop < viewportHeight && itemBottom > viewportHeight -> viewportHeight - itemTop // Bottom cut off
                        else -> 0 // Not visible
                    }
                    
                    val visibilityPercentage = if (itemInfo.size > 0) {
                        (visibleHeight.toFloat() / itemInfo.size).coerceIn(0f, 1f)
                    } else 0f
                    
                    item.id to visibilityPercentage
                } else null
            }.filterNotNull()
        }.collect { visibilityMap ->
            // Update visibility for each item
            visibilityMap.forEach { (itemId, visibility) ->
                viewModel.updateItemVisibility(itemId, visibility)
            }
            
            // Update visible range
            val visibleIndices = listState.layoutInfo.visibleItemsInfo.map { it.index }
            if (visibleIndices.isNotEmpty()) {
                viewModel.updateVisibleRange(visibleIndices.first(), visibleIndices.last())
            }
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        NewsFeedHeader()
        
        // Feed content
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(
                items = viewModel.feedItems,
                key = { _, item -> item.id }
            ) { index, item ->
                when (item) {
                    is VideoFeedItem -> VideoFeedCard(
                        item = item,
                        playbackStatus = playbackStatuses[item.id],
                        preloadStatus = preloadStatuses[item.id],
                        onPlayClick = { viewModel.playItem(item) },
                        onPauseClick = { viewModel.pauseItem(item.id) },
                        onMuteToggle = { viewModel.toggleMute(item.id) },
                        onLike = { viewModel.likeItem(item.id) },
                        onShare = { viewModel.shareItem(item.id) },
                        onComment = { viewModel.commentOnItem(item.id) }
                    )
                    
                    is AudioFeedItem -> AudioFeedCard(
                        item = item,
                        playbackStatus = playbackStatuses[item.id],
                        preloadStatus = preloadStatuses[item.id],
                        onPlayClick = { viewModel.playItem(item) },
                        onPauseClick = { viewModel.pauseItem(item.id) },
                        onLike = { viewModel.likeItem(item.id) },
                        onShare = { viewModel.shareItem(item.id) },
                        onComment = { viewModel.commentOnItem(item.id) }
                    )
                    
                    is ImageNewsFeedItem -> ImageNewsFeedCard(
                        item = item,
                        onLike = { viewModel.likeItem(item.id) },
                        onShare = { viewModel.shareItem(item.id) },
                        onComment = { viewModel.commentOnItem(item.id) }
                    )
                    
                    is TextNewsFeedItem -> TextNewsFeedCard(
                        item = item,
                        onLike = { viewModel.likeItem(item.id) },
                        onShare = { viewModel.shareItem(item.id) },
                        onComment = { viewModel.commentOnItem(item.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsFeedHeader() {
    TopAppBar(
        title = { Text("News Feed") },
        actions = {
            IconButton(onClick = { /* Search */ }) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
            IconButton(onClick = { /* Settings */ }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoFeedCard(
    item: VideoFeedItem,
    playbackStatus: FeedItemPlaybackStatus?,
    preloadStatus: PreloadItemStatus?,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onMuteToggle: () -> Unit,
    onLike: () -> Unit,
    onShare: () -> Unit,
    onComment: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Author header
            FeedItemHeader(
                authorName = item.authorName ?: "Unknown",
                publishTime = item.publishTime,
                contentType = "Video"
            )
            
            // Video content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                if (playbackStatus?.isPlaying == true) {
                    // Show video player when playing
                    AndroidView(
                        factory = { context ->
                            PlayerView(context).apply {
                                useController = false // We'll use custom controls
                                // Set player from helper
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Show thumbnail
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Play button overlay
                    FloatingActionButton(
                        onClick = onPlayClick,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                    }
                }
                
                // Video controls overlay
                if (playbackStatus?.isPlaying == true) {
                    VideoControlsOverlay(
                        isPlaying = true,
                        isMuted = playbackStatus.isMuted,
                        onPlayPause = onPauseClick,
                        onMuteToggle = onMuteToggle,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
                
                // Visibility indicator (for debugging)
                playbackStatus?.let { status ->
                    if (status.visibilityPercentage > 0f) {
                        LinearProgressIndicator(
                            progress = { status.visibilityPercentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // Content info
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (item.description != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Engagement row
                FeedItemActions(
                    likes = item.likes,
                    comments = item.comments,
                    shares = item.shares,
                    onLike = onLike,
                    onComment = onComment,
                    onShare = onShare
                )
            }
        }
    }
}

@Composable
fun VideoControlsOverlay(
    isPlaying: Boolean,
    isMuted: Boolean,
    onPlayPause: () -> Unit,
    onMuteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        
        IconButton(
            onClick = onMuteToggle,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = if (isMuted) "Unmute" else "Mute",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

// Additional composables for other feed item types would go here...
// AudioFeedCard, ImageNewsFeedCard, TextNewsFeedCard, etc.

@Composable
fun FeedItemHeader(
    authorName: String,
    publishTime: Long,
    contentType: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Author avatar placeholder
        Surface(
            modifier = Modifier.size(40.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.padding(8.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = authorName,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = formatTimeAgo(publishTime),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Chip(
            onClick = { },
            label = { Text(contentType) }
        )
    }
}

@Composable
fun FeedItemActions(
    likes: Int,
    comments: Int,
    shares: Int,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ActionButton(
                icon = Icons.Default.ThumbUp,
                count = likes,
                onClick = onLike
            )
            ActionButton(
                icon = Icons.Default.Comment,
                count = comments,
                onClick = onComment
            )
            ActionButton(
                icon = Icons.Default.Share,
                count = shares,
                onClick = onShare
            )
        }
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (count > 1000) "${count / 1000}k" else count.toString(),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// Utility functions
fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> "${diff / 86400000}d ago"
    }
}