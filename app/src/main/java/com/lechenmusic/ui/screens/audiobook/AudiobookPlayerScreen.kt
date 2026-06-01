package com.lechenmusic.ui.screens.audiobook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.lechenmusic.data.model.TingChapter
import com.lechenmusic.player.AudiobookPlayerManager
import com.lechenmusic.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudiobookPlayerScreen(
    bookId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val audiobookPlayer = viewModel.audiobookPlayerManager
    val tingServerUrl by viewModel.tingServerUrl.collectAsState()
    val tingToken by viewModel.tingToken.collectAsState()
    val currentChapter by audiobookPlayer.currentChapter.collectAsState()
    val chapters by audiobookPlayer.chapters.collectAsState()
    val currentChapterIndex by audiobookPlayer.currentChapterIndex.collectAsState()
    val isPlaying by audiobookPlayer.isPlaying.collectAsState()
    val progress by audiobookPlayer.progress.collectAsState()
    val currentPosition by audiobookPlayer.currentPosition.collectAsState()
    val duration by audiobookPlayer.duration.collectAsState()
    val bookTitle by audiobookPlayer.currentBookTitle.collectAsState()
    val bookAuthor by audiobookPlayer.currentBookAuthor.collectAsState()
    val bookCoverUrl by audiobookPlayer.currentBookCoverUrl.collectAsState()

    val timerRemainingSeconds by viewModel.timerRemainingSeconds.collectAsState()
    var showTimerDialog by remember { mutableStateOf(false) }
    var showChapterSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Load book if not already loaded
    LaunchedEffect(bookId) {
        if (audiobookPlayer.currentBookId.value != bookId || audiobookPlayer.chapters.value.isEmpty()) {
            viewModel.loadAndPlayAudiobook(bookId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "返回", modifier = Modifier.size(28.dp))
                }
                Text("正在播放", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { /* more options */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cover Image
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
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
                            .size(280.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF667eea), Color(0xFF764ba2)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Book & Chapter Info
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    bookTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (bookAuthor.isNotBlank() && bookAuthor != "Unknown") {
                    Text(
                        bookAuthor,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                // Chapter indicator
                if (currentChapter != null) {
                    Surface(
                        modifier = Modifier.padding(top = 10.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isPlaying) Color(0xFF30D158) else MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                "第${currentChapterIndex + 1}章 · ${currentChapter!!.title}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Progress Bar
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Slider(
                    value = progress,
                    onValueChange = { audiobookPlayer.seekToProgress(it) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(currentPosition), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("-${formatTime(duration - currentPosition)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Controls: Prev | Rewind30 | Play/Pause | Forward30 | Next
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous chapter
                IconButton(onClick = { audiobookPlayer.skipPrevious() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "上一章", modifier = Modifier.size(32.dp))
                }
                // Rewind 30s - same style as forward
                IconButton(onClick = { audiobookPlayer.rewind30s() }, modifier = Modifier.size(48.dp)) {
                    Surface(
                        modifier = Modifier.size(46.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Replay,
                                contentDescription = "后退30秒",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                // Play/Pause - main button
                Surface(
                    modifier = Modifier.size(68.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 8.dp
                ) {
                    IconButton(onClick = { audiobookPlayer.togglePlayPause() }) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                // Forward 30s
                IconButton(onClick = { audiobookPlayer.forward30s() }, modifier = Modifier.size(48.dp)) {
                    Surface(
                        modifier = Modifier.size(46.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Forward,
                                contentDescription = "前进30秒",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                // Next chapter
                IconButton(onClick = { audiobookPlayer.skipNext() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "下一章", modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Speed
                var speedIndex by remember { mutableIntStateOf(1) }
                val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        speedIndex = (speedIndex + 1) % speeds.size
                        audiobookPlayer.setSpeed(speeds[speedIndex])
                    }.padding(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            "${speeds[speedIndex]}×",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("倍速", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
                // Sleep timer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { showTimerDialog = true }.padding(8.dp)
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = "定时",
                        tint = if (timerRemainingSeconds > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        if (timerRemainingSeconds > 0) {
                            val min = timerRemainingSeconds / 60
                            val sec = timerRemainingSeconds % 60
                            "%d:%02d".format(min, sec)
                        } else "定时",
                        fontSize = 10.sp,
                        color = if (timerRemainingSeconds > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Chapters
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { showChapterSheet = true }.padding(8.dp)
                ) {
                    Icon(
                        Icons.Default.QueueMusic,
                        contentDescription = "章节",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "章节 ${currentChapterIndex + 1}/${chapters.size}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Sleep Timer Dialog
    if (showTimerDialog) {
        AlertDialog(
            onDismissRequest = { showTimerDialog = false },
            title = { Text("定时停止播放") },
            text = {
                Column {
                    if (timerRemainingSeconds > 0) {
                        val remainMin = timerRemainingSeconds / 60
                        val remainSec = timerRemainingSeconds % 60
                        Text(
                            "剩余时间: ${remainMin}分${remainSec}秒",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            "取消定时",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.cancelTimerWithCountdown()
                                    showTimerDialog = false
                                }
                                .padding(vertical = 14.dp),
                            fontSize = 15.sp,
                            color = Color.Red
                        )
                        HorizontalDivider()
                    }
                    listOf("15分钟" to 15, "30分钟" to 30, "1小时" to 60, "2小时" to 120).forEach { (label, min) ->
                        Text(
                            label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setAudiobookTimer(min)
                                    showTimerDialog = false
                                }
                                .padding(vertical = 14.dp),
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimerDialog = false }) { Text("取消") }
            }
        )
    }

    // Chapter List Sheet
    if (showChapterSheet) {
        ModalBottomSheet(onDismissRequest = { showChapterSheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("章节列表", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "共 ${chapters.size} 章",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(chapters) { index, chapter ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    audiobookPlayer.playChapter(index)
                                    showChapterSheet = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${index + 1}",
                                fontSize = 13.sp,
                                color = if (index == currentChapterIndex) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.Center
                            )
                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(
                                    chapter.title,
                                    fontSize = 14.sp,
                                    color = if (index == currentChapterIndex) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (index == currentChapterIndex) FontWeight.SemiBold else FontWeight.Normal
                                )
                                Text(
                                    formatDuration(chapter.duration),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (index == currentChapterIndex && isPlaying) {
                                Icon(
                                    Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private fun formatDuration(seconds: Double): String {
    val totalSec = seconds.toLong()
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val secs = totalSec % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, secs)
    } else {
        "%d:%02d".format(minutes, secs)
    }
}
