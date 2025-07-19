// NewsFeedCardComponents.kt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioFeedCard(
    item: AudioFeedItem,
    playbackStatus: FeedItemPlaybackStatus?,
    preloadStatus: PreloadItemStatus?,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onLike: () -> Unit,
    onShare: () -> Unit,
    onComment: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (playbackStatus?.isPlaying == true) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Author header
            FeedItemHeader(
                authorName = item.authorName ?: "Unknown",
                publishTime = item.publishTime,
                contentType = "Audio"
            )
            
            // Audio content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Audio thumbnail/waveform placeholder
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    if (item.thumbnailUrl != null) {
                        AsyncImage(
                            model = item.thumbnailUrl,
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Audio",
                                modifier = Modifier.padding(24.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    // Play/Pause overlay
                    FloatingActionButton(
                        onClick = if (playbackStatus?.isPlaying == true) onPauseClick else onPlayClick,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(32.dp),
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    ) {
                        Icon(
                            imageVector = if (playbackStatus?.isPlaying == true) 
                                Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackStatus?.isPlaying == true) "Pause" else "Play",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Audio info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (item.podcastName != null) {
                        Text(
                            text = item.podcastName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    if (item.description != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Duration
                    if (item.duration != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatDuration(item.duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Audio progress bar
            if (playbackStatus?.isPlaying == true && playbackStatus.duration > 0) {
                val progress = playbackStatus.currentPosition.toFloat() / playbackStatus.duration
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Preload status indicator
            preloadStatus?.let { status ->
                if (status.isLoading && status.progress > 0f) {
                    LinearProgressIndicator(
                        progress = { status.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // Engagement row
            Padding(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                FeedItemActions(
                    likes = item.likes,
                    comments = item.comments,
                    shares = 0, // Audio typically doesn't show shares
                    onLike = onLike,
                    onComment = onComment,
                    onShare = onShare
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageNewsFeedCard(
    item: ImageNewsFeedItem,
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
                contentType = item.newsCategory ?: "News"
            )
            
            // Main image
            if (item.thumbnailUrl != null) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Content
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // News category chip
                if (item.newsCategory != null) {
                    AssistChip(
                        onClick = { },
                        label = { 
                            Text(
                                text = item.newsCategory,
                                style = MaterialTheme.typography.bodySmall
                            ) 
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (item.description != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Read time
                if (item.readTime != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.readTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Additional images carousel
                if (item.imageUrls.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(item.imageUrls) { imageUrl ->
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Additional image",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextNewsFeedCard(
    item: TextNewsFeedItem,
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
                contentType = item.newsCategory ?: "News"
            )
            
            // Content
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // News category chip
                if (item.newsCategory != null) {
                    AssistChip(
                        onClick = { },
                        label = { 
                            Text(
                                text = item.newsCategory,
                                style = MaterialTheme.typography.bodySmall
                            ) 
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (item.description != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Article preview
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = item.content,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        TextButton(
                            onClick = { /* Navigate to full article */ }
                        ) {
                            Text("Read more")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                
                // Read time
                if (item.readTime != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.readTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
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

// Utility function for padding (since we can't use Padding directly)
@Composable
fun Padding(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
    }
}

// Utility function to format duration for audio content
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