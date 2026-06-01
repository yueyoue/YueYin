package com.lechenmusic.ui.screens.audiobook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.model.TingBook
import com.lechenmusic.data.model.TingProgress
import com.lechenmusic.data.api.TingReaderApiClient
import com.lechenmusic.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudiobookListScreen(
    viewModel: MainViewModel,
    onBookClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val audiobooks by viewModel.audiobooks.collectAsState()
    val audiobookProgress by viewModel.audiobookProgress.collectAsState()
    val audiobooksLoading by viewModel.audiobooksLoading.collectAsState()
    val tingServerUrl by viewModel.tingServerUrl.collectAsState()
    val tingToken by viewModel.tingToken.collectAsState()
    val searchQuery by viewModel.audiobookSearchQuery.collectAsState()
    val searchResults by viewModel.audiobookSearchResults.collectAsState()
    val context = LocalContext.current

    var selectedFilter by remember { mutableStateOf("全部") }
    var showSearch by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadAudiobooks()
    }

    val displayBooks = if (showSearch && searchQuery.isNotBlank()) {
        searchResults
    } else {
        when (selectedFilter) {
            "在听" -> audiobooks.filter { book ->
                val prog = audiobookProgress[book.id]
                prog != null && (prog.position ?: 0.0) > 0
            }
            "未开始" -> audiobooks.filter { book ->
                audiobookProgress[book.id] == null
            }
            "已完成" -> audiobooks.filter { book ->
                val prog = audiobookProgress[book.id]
                prog != null && (prog.position ?: 0.0) > 0 // simplified
            }
            else -> audiobooks
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
            Text("有声小说", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = { showSearch = !showSearch }) {
                Icon(
                    if (showSearch) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "搜索"
                )
            }
            IconButton(onClick = { viewModel.loadAudiobooks() }) {
                Icon(Icons.Default.Refresh, contentDescription = "刷新")
            }
        }

        // Search bar
        if (showSearch) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchAudiobooks(it) },
                placeholder = { Text("搜索有声书...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.searchAudiobooks("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除")
                        }
                    }
                }
            )
        }

        // Filter chips
        if (!showSearch) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("全部", "在听", "未开始", "已完成").forEach { filter ->
                    Surface(
                        modifier = Modifier.clickable { selectedFilter = filter },
                        shape = RoundedCornerShape(20.dp),
                        color = if (selectedFilter == filter) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            filter,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 13.sp,
                            fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedFilter == filter) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Loading indicator
        if (audiobooksLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (displayBooks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (showSearch) "未找到相关有声书" else "暂无有声书",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            // Book grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(displayBooks) { book ->
                    val progress = audiobookProgress[book.id]
                    AudiobookGridItem(
                        book = book,
                        progress = progress,
                        serverUrl = tingServerUrl,
                        token = tingToken,
                        onClick = { onBookClick(book.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AudiobookGridItem(
    book: TingBook,
    progress: TingProgress?,
    serverUrl: String,
    token: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box {
            val coverUrl = book.coverUrl
            if (coverUrl != null) {
                val proxyUrl = TingReaderApiClient.getCoverProxyUrl(serverUrl, coverUrl, token)
                AsyncImage(
                    model = proxyUrl,
                    contentDescription = book.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF667eea), Color(0xFF764ba2)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Progress bar at bottom
            if (progress != null && progress.position > 0) {
                val progressPercent = if (progress.duration != null && progress.duration > 0) {
                    (progress.position / progress.duration).toFloat().coerceIn(0f, 1f)
                } else 0f
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressPercent)
                            .height(3.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        Text(
            book.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            if (book.author.isNotBlank() && book.author != "Unknown") book.author else "未知作者",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
