package com.yueyin.ui.screens.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yueyin.R
import com.yueyin.player.AudiobookPlayerManager
import com.yueyin.ui.MainViewModel
import com.yueyin.ui.theme.*

@Composable
fun Seek15Icon(isForward: Boolean, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = if (isForward) R.drawable.ic_seek_forward_15 else R.drawable.ic_seek_back_15),
        contentDescription = if (isForward) "前进15秒" else "后退15秒",
        modifier = modifier.size(24.dp),
        contentScale = ContentScale.Fit
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    // Track user drag state to avoid slider fighting with player position updates
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }
    val displayProgress = if (isSeeking) seekProgress else prog
    val title by ap.currentBookTitle.collectAsState()
    val author by ap.currentBookAuthor.collectAsState()
    val narrator by ap.currentBookNarrator.collectAsState()
    val coverUrl by ap.currentBookCoverUrl.collectAsState()
    val bookDesc by ap.currentBookDescription.collectAsState()
    val timerSec by viewModel.timerRemainingSeconds.collectAsState()
    val playbackSpeed by ap.playbackSpeed.collectAsState()
    val favoriteBookIds by viewModel.favoriteBookIds.collectAsState()
    val isFavorite = remember(favoriteBookIds, bookId) { favoriteBookIds.contains(bookId) }
    val currentLoadedBookId by ap.currentBookId.collectAsState()
    // Detect if we're switching to a different book — show loading instead of stale data
    val isBookReady = remember(currentLoadedBookId, bookId, chapters.size) { currentLoadedBookId == bookId && chapters.isNotEmpty() }
    var showTimer by remember { mutableStateOf(false) }
    var showChapters by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(bookId) {
        // Always sync favorite status from server when entering player
        viewModel.syncFavoriteFromServer(bookId)
        if (currentLoadedBookId != bookId || chapters.isEmpty()) viewModel.loadAndPlayAudiobook(bookId)
    }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(primaryColor.copy(alpha = 0.08f), MaterialTheme.colorScheme.background)))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.KeyboardArrowDown, "返回", modifier = Modifier.size(28.dp)) }
                Text("正在播放", fontSize = 12.sp, color = OnSurfaceVariant, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (!isBookReady) {
                // Loading placeholder — no stale data flash
                Box(modifier = Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primaryColor, modifier = Modifier.size(48.dp))
                }
                Spacer(modifier = Modifier.weight(1f))
            } else {

            // Cover + Description pager (swipe left for description)
            val hasDesc = bookDesc.isNotBlank()
            val pagerState = rememberPagerState(pageCount = { if (hasDesc) 2 else 1 })

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().height(280.dp)
            ) { page ->
                if (page == 0) {
                    // Cover page
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (coverUrl != null) AsyncImage(model = ImageRequest.Builder(ctx).data(coverUrl).crossfade(true).memoryCacheKey("pc_${title.hashCode()}").build(), contentDescription = title, modifier = Modifier.size(250.dp).clip(RoundedCornerShape(24.dp)), contentScale = ContentScale.Crop)
                        else Box(modifier = Modifier.size(250.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xFF667eea), Color(0xFF764ba2)))), contentAlignment = Alignment.Center) { Icon(Icons.Default.MenuBook, null, tint = Color.White.copy(0.6f), modifier = Modifier.size(64.dp)) }
                    }
                } else {
                    // Description page
                    Surface(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).clip(RoundedCornerShape(20.dp)), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                        Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoStories, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("内容简介", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(bookDesc, fontSize = 13.sp, color = OnSurfaceVariant, lineHeight = 22.sp)
                        }
                    }
                }
            }

            // Page indicators
            if (hasDesc) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center) {
                    repeat(2) { i ->
                        Box(modifier = Modifier.padding(horizontal = 4.dp).size(if (i == pagerState.currentPage) 8.dp else 6.dp).clip(CircleShape).background(if (i == pagerState.currentPage) primaryColor else OnSurfaceVariant.copy(alpha = 0.3f)))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Book info
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.basicMarquee())
                if (author.isNotBlank()) Text(author, fontSize = 13.sp, color = OnSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 3.dp))
                if (narrator.isNotBlank()) Text("播音：$narrator", fontSize = 12.sp, color = OnSurfaceVariant2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
                if (chapter != null) {
                    Surface(modifier = Modifier.padding(top = 10.dp), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (isPlaying) Green else OnSurfaceVariant))
                            Text("第${chIdx + 1}章 · ${chapter!!.title}", fontSize = 12.sp, color = OnSurfaceVariant, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))

            // Progress bar
            Column(modifier = Modifier.padding(horizontal = 30.dp)) {
                Slider(
                    value = displayProgress,
                    onValueChange = { newValue ->
                        isSeeking = true
                        seekProgress = newValue
                    },
                    onValueChangeFinished = {
                        ap.seekToProgress(seekProgress)
                        isSeeking = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(thumbColor = primaryColor, activeTrackColor = primaryColor)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(fmtTime(if (isSeeking) (seekProgress * dur).toLong() else pos), fontSize = 11.sp, color = OnSurfaceVariant)
                    Text("-${fmtTime(dur - (if (isSeeking) (seekProgress * dur).toLong() else pos))}", fontSize = 11.sp, color = OnSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Controls: -15s | prev | play/pause | next | +15s
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { ap.rewind15s() }, modifier = Modifier.size(40.dp)) {
                    Seek15Icon(isForward = false)
                }
                IconButton(onClick = { ap.skipPrevious() }, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.SkipPrevious, "上一章", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
                Surface(modifier = Modifier.size(68.dp), shape = CircleShape, color = primaryColor, shadowElevation = 8.dp) {
                    IconButton(onClick = { ap.togglePlayPause() }) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(34.dp))
                    }
                }
                IconButton(onClick = { ap.skipNext() }, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.SkipNext, "下一章", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
                IconButton(onClick = { ap.forward15s() }, modifier = Modifier.size(40.dp)) {
                    Seek15Icon(isForward = true)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Bottom tools: speed | timer | chapters | favorite
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Box {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showSpeedMenu = true }.padding(8.dp)) {
                        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                            Text("${playbackSpeed}×", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("倍速", fontSize = 10.sp, color = OnSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                    DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }) {
                        listOf(1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            DropdownMenuItem(
                                text = { Text("${speed}×", fontWeight = if (speed == playbackSpeed) FontWeight.Bold else FontWeight.Normal, color = if (speed == playbackSpeed) primaryColor else MaterialTheme.colorScheme.onSurface) },
                                onClick = { ap.setSpeed(speed); showSpeedMenu = false },
                                leadingIcon = { if (speed == playbackSpeed) Icon(Icons.Default.Check, null, tint = primaryColor, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showTimer = true }.padding(8.dp)) {
                    Icon(Icons.Default.Timer, "定时", tint = if (timerSec > 0) primaryColor else OnSurfaceVariant, modifier = Modifier.size(24.dp))
                    Text(if (timerSec > 0) "%d:%02d".format(timerSec / 60, timerSec % 60) else "定时", fontSize = 10.sp, color = if (timerSec > 0) primaryColor else OnSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showChapters = true }.padding(8.dp)) {
                    Icon(Icons.Default.QueueMusic, "章节", tint = OnSurfaceVariant, modifier = Modifier.size(24.dp))
                    Text("章节 ${chIdx + 1}/${chapters.size}", fontSize = 10.sp, color = OnSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { viewModel.toggleFavorite(bookId) }.padding(8.dp)) {
                    Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "收藏", tint = if (isFavorite) primaryColor else OnSurfaceVariant, modifier = Modifier.size(24.dp))
                    Text("收藏", fontSize = 10.sp, color = if (isFavorite) primaryColor else OnSurfaceVariant)
                }
            }
            } // end else (isBookReady)
        }
    }

    // 定时弹窗
    if (showTimer) {
        var customMinutes by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showTimer = false }, title = { Text("定时停止播放") }, text = {
            Column {
                if (timerSec > 0) {
                    Text("剩余: ${timerSec / 60}分${timerSec % 60}秒", color = primaryColor, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 12.dp))
                    Text("取消定时", modifier = Modifier.fillMaxWidth().clickable { viewModel.cancelTimer(); showTimer = false }.padding(14.dp), color = Color.Red)
                    HorizontalDivider()
                }
                listOf("15分钟" to 15, "30分钟" to 30, "1小时" to 60).forEach { (l, m) ->
                    Text(l, modifier = Modifier.fillMaxWidth().clickable { viewModel.setAudiobookTimer(m); showTimer = false }.padding(14.dp))
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = customMinutes, onValueChange = { customMinutes = it.filter { c -> c.isDigit() } },
                        placeholder = { Text("自定义分钟") }, singleLine = true,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { val m = customMinutes.toIntOrNull(); if (m != null && m > 0) { viewModel.setAudiobookTimer(m); showTimer = false } },
                        enabled = customMinutes.toIntOrNull()?.let { it > 0 } == true,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) { Text("确定") }
                }
            }
        }, confirmButton = { TextButton(onClick = { showTimer = false }) { Text("关闭") } })
    }

    // 章节列表
    if (showChapters) ModalBottomSheet(onDismissRequest = { showChapters = false }) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("章节列表", fontWeight = FontWeight.Bold); Text("共 ${chapters.size} 章", color = OnSurfaceVariant) }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                itemsIndexed(chapters) { i, c ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { ap.playChapter(i); showChapters = false }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}", fontSize = 13.sp, color = if (i == chIdx) primaryColor else OnSurfaceVariant, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) { Text(c.title, color = if (i == chIdx) primaryColor else MaterialTheme.colorScheme.onSurface, fontWeight = if (i == chIdx) FontWeight.SemiBold else FontWeight.Normal); Text(fmtDur(c.duration), fontSize = 12.sp, color = OnSurfaceVariant) }
                        if (i == chIdx && isPlaying) Icon(Icons.Default.GraphicEq, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

private fun fmtTime(ms: Long): String { val s = ms / 1000; val h = s / 3600; val m = (s % 3600) / 60; val sc = s % 60; return if (h > 0) "%d:%02d:%02d".format(h, m, sc) else "%d:%02d".format(m, sc) }
private fun fmtDur(sec: Double): String { val s = sec.toLong(); val h = s / 3600; val m = (s % 3600) / 60; val sc = s % 60; return if (h > 0) "%d:%02d:%02d".format(h, m, sc) else "%d:%02d".format(m, sc) }
