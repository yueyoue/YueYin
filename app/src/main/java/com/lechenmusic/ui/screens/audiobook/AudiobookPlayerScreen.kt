package com.lechenmusic.ui.screens.audiobook

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
    val shuffleMode by audiobookPlayer.shuffleMode.collectAsState()
    val repeatMode by audiobookPlayer.repeatMode.collectAsState()
    val bookTitle by audiobookPlayer.currentBookTitle.collectAsState()
    val bookAuthor by audiobookPlayer.currentBookAuthor.collectAsState()
    val bookCoverUrl by audiobookPlayer.currentBookCoverUrl.collectAsState()

    val timerRemainingSeconds by viewModel.timerRemainingSeconds.collectAsState()
    var showTimerDialog by remember { mutableStateOf(false) }
    var showChapterSheet by remember { mutableStateOf(false) }

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
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "返回")
                }
                Text("有声书播放", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        model = coverUrl,
                        contentDescription = bookTitle,
                        modifier = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(20.dp))
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

            Spacer(modifier = Modifier.height(20.dp))

            // Book & Chapter Info
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    bookTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (bookAuthor.isNotBlank() && bookAuthor != "Unknown") bookAuthor else "",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
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

            Spacer(modifier = Modifier.height(20.dp))

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
                    Text(formatTime(duration), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Controls with 30s forward/backward
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = { audiobookPlayer.toggleShuffle() }) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "随机",
                        tint = if (shuffleMode) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
                // Previous chapter
                IconButton(onClick = { audiobookPlayer.skipPrevious() }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "上一章", modifier = Modifier.size(30.dp))
                }
                // Rewind 30s
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { audiobookPlayer.rewind30s() }) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Replay,
                                    contentDescription = "后退30秒",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                    Text("30s", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Play/Pause
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 8.dp
                ) {
                    IconButton(onClick = { audiobookPlayer.togglePlayPause() }) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                // Forward 30s
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { audiobookPlayer.forward30s() }) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Forward,
                                    contentDescription = "前进30秒",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                    Text("30s", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Next chapter
                IconButton(onClick = { audiobookPlayer.skipNext() }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "下一章", modifier = Modifier.size(30.dp))
                }
                // Repeat
                IconButton(onClick = { audiobookPlayer.toggleRepeat() }) {
                    Icon(
                        when (repeatMode) {
                            0 -> Icons.Default.Repeat    // OFF
                            1 -> Icons.Default.Repeat    // ALL
                            else -> Icons.Default.RepeatOne // ONE
                        },
                        contentDescription = "循环",
                        tint = if (repeatMode != 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action bar: Sleep timer, Chapters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { showTimerDialog = true }
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { showChapterSheet = true }
                ) {
                    Icon(
                        Icons.Default.QueueMusic,
                        contentDescription = "章节",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
