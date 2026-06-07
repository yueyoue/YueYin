package com.yueyin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yueyin.player.AudiobookPlayerManager
import com.yueyin.ui.theme.*

@Composable
fun MiniPlayer(playerManager: AudiobookPlayerManager, onClick: () -> Unit) {
    val bookTitle by playerManager.currentBookTitle.collectAsState()
    val currentChapter by playerManager.currentChapter.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val progress by playerManager.progress.collectAsState()
    val bookCoverUrl by playerManager.currentBookCoverUrl.collectAsState()
    val context = LocalContext.current
    if (bookTitle.isBlank()) return

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp), color = Surface, shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column {
            Row(modifier = Modifier.padding(10.dp, 10.dp, 14.dp, 10.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(bookCoverUrl).crossfade(true).memoryCacheKey("mini_cover").build(),
                    contentDescription = null, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(bookTitle, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(currentChapter?.title ?: "", fontSize = 11.sp, color = OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { playerManager.togglePlayPause() }, modifier = Modifier.size(40.dp)) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = OnSurface, modifier = Modifier.size(24.dp))
                }
            }
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(3.dp).background(Border, RoundedCornerShape(2.dp))) {
                Box(modifier = Modifier.fillMaxWidth(progress).height(3.dp).background(Primary, RoundedCornerShape(2.dp)))
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
