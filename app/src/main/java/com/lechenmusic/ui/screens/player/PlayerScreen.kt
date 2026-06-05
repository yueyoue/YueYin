package com.lechenmusic.ui.screens.player

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
import coil.request.ImageRequest
import com.lechenmusic.player.AudiobookPlayerManager
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(bookId: String, viewModel: MainViewModel, onBack: () -> Unit) {
    val ap = viewModel.audiobookPlayerManager
    val chapter by ap.currentChapter.collectAsState()
    val chapters by ap.chapters.collectAsState()
    val chIdx by ap.currentChapterIndex.collectAsState()
    val isPlaying by ap.isPlaying.collectAsState()
    val prog by ap.progress.collectAsState()
    val pos by ap.currentPosition.collectAsState()
    val dur by ap.duration.collectAsState()
    val title by ap.currentBookTitle.collectAsState()
    val author by ap.currentBookAuthor.collectAsState()
    val narrator by ap.currentBookNarrator.collectAsState()
    val coverUrl by ap.currentBookCoverUrl.collectAsState()
    val timerSec by viewModel.timerRemainingSeconds.collectAsState()
    var showTimer by remember { mutableStateOf(false) }
    var showChapters by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    LaunchedEffect(bookId) { if (ap.currentBookId.value != bookId || ap.chapters.value.isEmpty()) viewModel.loadAndPlayAudiobook(bookId) }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Primary.copy(alpha = 0.08f), Background)))) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.KeyboardArrowDown, "返回", modifier = Modifier.size(28.dp)) }
                Text("正在播放", fontSize = 12.sp, color = OnSurfaceVariant, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, "更多") }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (coverUrl != null) AsyncImage(model = ImageRequest.Builder(ctx).data(coverUrl).crossfade(true).memoryCacheKey("pc_$bookId").build(), contentDescription = title, modifier = Modifier.size(280.dp).clip(RoundedCornerShape(24.dp)), contentScale = ContentScale.Crop)
                else Box(modifier = Modifier.size(280.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xFF667eea), Color(0xFF764ba2)))), contentAlignment = Alignment.Center) { Icon(Icons.Default.MenuBook, null, tint = Color.White.copy(0.6f), modifier = Modifier.size(64.dp)) }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.padding(horizontal = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (author.isNotBlank()) Text(author, fontSize = 13.sp, color = OnSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
                if (narrator.isNotBlank()) Text("播音：$narrator", fontSize = 12.sp, color = OnSurfaceVariant2, modifier = Modifier.padding(top = 2.dp))
                if (chapter != null) {
                    Surface(modifier = Modifier.padding(top = 10.dp), shape = RoundedCornerShape(20.dp), color = Surface, border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
                        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (isPlaying) Green else OnSurfaceVariant))
                            Text("第${chIdx + 1}章 · ${chapter!!.title}", fontSize = 12.sp, color = OnSurfaceVariant, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.padding(horizontal = 30.dp)) {
                Slider(value = prog, onValueChange = { ap.seekToProgress(it) }, modifier = Modifier.fillMaxWidth(), colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(fmtTime(pos), fontSize = 11.sp, color = OnSurfaceVariant); Text("-${fmtTime(dur - pos)}", fontSize = 11.sp, color = OnSurfaceVariant) }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {}, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Shuffle, "随机", modifier = Modifier.size(20.dp), tint = OnSurfaceVariant2) }
                IconButton(onClick = { ap.rewind15s() }, modifier = Modifier.size(48.dp)) { Surface(modifier = Modifier.size(46.dp), shape = CircleShape, color = SurfaceVariant) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Replay, "后退15秒", modifier = Modifier.size(24.dp)) } } }
                Surface(modifier = Modifier.size(68.dp), shape = CircleShape, color = Primary, shadowElevation = 8.dp) { IconButton(onClick = { ap.togglePlayPause() }) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(34.dp)) } }
                IconButton(onClick = { ap.forward15s() }, modifier = Modifier.size(48.dp)) { Surface(modifier = Modifier.size(46.dp), shape = CircleShape, color = SurfaceVariant) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Forward, "前进15秒", modifier = Modifier.size(24.dp)) } } }
                IconButton(onClick = {}, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Repeat, "循环", modifier = Modifier.size(20.dp), tint = OnSurfaceVariant2) }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                var si by remember { mutableIntStateOf(1) }; val sp = listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { si = (si + 1) % sp.size; ap.setSpeed(sp[si]) }.padding(8.dp)) {
                    Surface(shape = RoundedCornerShape(14.dp), color = Surface, border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Text("${sp[si]}×", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    Text("倍速", fontSize = 10.sp, color = OnSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showTimer = true }.padding(8.dp)) {
                    Icon(Icons.Default.Timer, "定时", tint = if (timerSec > 0) Primary else OnSurfaceVariant, modifier = Modifier.size(24.dp))
                    Text(if (timerSec > 0) "%d:%02d".format(timerSec / 60, timerSec % 60) else "定时", fontSize = 10.sp, color = if (timerSec > 0) Primary else OnSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showChapters = true }.padding(8.dp)) {
                    Icon(Icons.Default.QueueMusic, "章节", tint = OnSurfaceVariant, modifier = Modifier.size(24.dp))
                    Text("章节 ${chIdx + 1}/${chapters.size}", fontSize = 10.sp, color = OnSurfaceVariant)
                }
            }
        }
    }
    if (showTimer) AlertDialog(onDismissRequest = { showTimer = false }, title = { Text("定时停止播放") }, text = {
        Column {
            if (timerSec > 0) { Text("剩余: ${timerSec / 60}分${timerSec % 60}秒", color = Primary, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 12.dp)); Text("取消定时", modifier = Modifier.fillMaxWidth().clickable { viewModel.cancelTimer(); showTimer = false }.padding(14.dp), color = Color.Red); HorizontalDivider() }
            listOf("15分钟" to 15, "30分钟" to 30, "1小时" to 60, "2小时" to 120).forEach { (l, m) -> Text(l, modifier = Modifier.fillMaxWidth().clickable { viewModel.setAudiobookTimer(m); showTimer = false }.padding(14.dp)) }
        }
    }, confirmButton = { TextButton(onClick = { showTimer = false }) { Text("取消") } })
    if (showChapters) ModalBottomSheet(onDismissRequest = { showChapters = false }) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("章节列表", fontWeight = FontWeight.Bold); Text("共 ${chapters.size} 章", color = OnSurfaceVariant) }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                itemsIndexed(chapters) { i, c ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { ap.playChapter(i); showChapters = false }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}", fontSize = 13.sp, color = if (i == chIdx) Primary else OnSurfaceVariant, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) { Text(c.title, color = if (i == chIdx) Primary else OnSurface, fontWeight = if (i == chIdx) FontWeight.SemiBold else FontWeight.Normal); Text(fmtDur(c.duration), fontSize = 12.sp, color = OnSurfaceVariant) }
                        if (i == chIdx && isPlaying) Icon(Icons.Default.GraphicEq, null, tint = Primary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

private fun fmtTime(ms: Long): String { val s = ms / 1000; val h = s / 3600; val m = (s % 3600) / 60; val sc = s % 60; return if (h > 0) "%d:%02d:%02d".format(h, m, sc) else "%d:%02d".format(m, sc) }
private fun fmtDur(sec: Double): String { val s = sec.toLong(); val h = s / 3600; val m = (s % 3600) / 60; val sc = s % 60; return if (h > 0) "%d:%02d:%02d".format(h, m, sc) else "%d:%02d".format(m, sc) }
