package com.lechenmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lechenmusic.player.AudiobookPlayerManager

@Composable
fun AudiobookMiniPlayer(
    audiobookPlayerManager: AudiobookPlayerManager,
    onClick: () -> Unit
) {
    val bookTitle by audiobookPlayerManager.currentBookTitle.collectAsState()
    val bookCoverUrl by audiobookPlayerManager.currentBookCoverUrl.collectAsState()
    val currentChapter by audiobookPlayerManager.currentChapter.collectAsState()
    val currentChapterIndex by audiobookPlayerManager.currentChapterIndex.collectAsState()
    val isPlaying by audiobookPlayerManager.isPlaying.collectAsState()
    val progress by audiobookPlayerManager.progress.collectAsState()
    val bookId by audiobookPlayerManager.currentBookId.collectAsState()

    if (bookTitle.isBlank()) return

    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        shadowElevation = 8.dp
    ) {
        Column {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cover
                val coverUrl = bookCoverUrl
                if (coverUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(coverUrl)
                            .crossfade(true)
                            .memoryCacheKey("audiobook_cover_$bookId")
                            .diskCacheKey("audiobook_cover_$bookId")
                            .build(),
                        contentDescription = bookTitle,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF667eea), Color(0xFF764ba2)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                // Info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = bookTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "第${currentChapterIndex + 1}章 · ${currentChapter?.title ?: ""}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // Controls
                IconButton(onClick = { audiobookPlayerManager.rewind30s() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Replay, contentDescription = "后退30秒", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { audiobookPlayerManager.togglePlayPause() }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(onClick = { audiobookPlayerManager.forward30s() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Forward, contentDescription = "前进30秒", modifier = Modifier.size(20.dp))
                }
            }
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 0.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}
