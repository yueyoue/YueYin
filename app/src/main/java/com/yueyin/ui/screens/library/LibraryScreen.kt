package com.yueyin.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yueyin.data.model.TingBook
import com.yueyin.data.model.TingProgress
import com.yueyin.ui.MainViewModel
import com.yueyin.ui.components.PullToRefreshLayout
import com.yueyin.ui.theme.*

@Composable
fun LibraryScreen(viewModel: MainViewModel, onBookClick: (String) -> Unit) {
    val allBooks by viewModel.allBooks.collectAsState()
    val bookProgress by viewModel.bookProgress.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val totalHours by viewModel.totalListeningHours.collectAsState()
    var viewMode by remember { mutableStateOf("grid") }
    var filterMode by remember { mutableStateOf("全部") }
    var showAllBooks by remember { mutableStateOf(false) }
    val booksLoading by viewModel.booksLoading.collectAsState()
    val filtered = remember(allBooks, bookProgress, filterMode) {
        when (filterMode) {
            "在听" -> allBooks.filter { bookProgress.containsKey(it.id) }
            "收藏" -> allBooks.filter { it.isFavorite }
            else -> allBooks
        }
    }
    val listeningCount = allBooks.count { bookProgress.containsKey(it.id) }

    if (showAllBooks) {
        AllBooksPage(viewModel = viewModel, allBooks = allBooks, bookProgress = bookProgress, onBookClick = onBookClick, onBack = { showAllBooks = false })
        return
    }

    PullToRefreshLayout(
        isRefreshing = booksLoading,
        onRefresh = { viewModel.loadBooks() },
        modifier = Modifier.fillMaxSize()
    ) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(48.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("📚 书架", fontSize = 28.sp, fontWeight = FontWeight.Black)
        }
        Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            Row(modifier = Modifier.padding(vertical = 14.dp)) {
                // 书籍 - clickable
                Column(modifier = Modifier.weight(1f).clickable { showAllBooks = true }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${stats.totalBooks}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Primary)
                    Text("书籍", fontSize = 11.sp, color = OnSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                }
                // 总时长
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(String.format("%.1f", totalHours), fontSize = 22.sp, fontWeight = FontWeight.Black, color = Primary)
                    Text("总时长(h)", fontSize = 11.sp, color = OnSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                }
                // 在听
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$listeningCount", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Primary)
                    Text("在听", fontSize = 11.sp, color = OnSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("全部", "在听", "收藏").forEach { f ->
                Surface(modifier = Modifier.clickable { filterMode = f }, shape = RoundedCornerShape(20.dp), color = if (filterMode == f) Primary else MaterialTheme.colorScheme.surface, border = if (filterMode != f) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null) {
                    Text(f, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 13.sp, fontWeight = if (filterMode == f) FontWeight.Bold else FontWeight.Medium, color = if (filterMode == f) Color.White else OnSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { viewMode = "grid" }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.GridView, "网格", tint = if (viewMode == "grid") Primary else OnSurfaceVariant, modifier = Modifier.size(20.dp)) }
            IconButton(onClick = { viewMode = "list" }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ViewList, "列表", tint = if (viewMode == "list") Primary else OnSurfaceVariant, modifier = Modifier.size(20.dp)) }
        }
        if (viewMode == "grid") {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                filtered.chunked(3).forEach { row ->
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            row.forEach { b -> GridItem(b, bookProgress[b.id], Modifier.weight(1f)) { onBookClick(b.id) } }
                            repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered) { b -> ListItem(b, bookProgress[b.id]) { onBookClick(b.id) } }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
    } // PullToRefreshBox
}

@Composable
private fun AllBooksPage(viewModel: MainViewModel, allBooks: List<TingBook>, bookProgress: Map<String, TingProgress>, onBookClick: (String) -> Unit, onBack: () -> Unit) {
    val genres by viewModel.genres.collectAsState()
    var selectedCategory by remember { mutableStateOf("全部") }
    val filteredBooks = remember(allBooks, selectedCategory) {
        if (selectedCategory == "全部") allBooks
        else allBooks.filter { it.genre?.contains(selectedCategory, true) == true }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(48.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") }
            Text("所有书籍", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Text("${filteredBooks.size}本", fontSize = 14.sp, color = OnSurfaceVariant, modifier = Modifier.padding(end = 16.dp))
        }
        // Category chips
        if (genres.size > 1) {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                genres.forEach { g ->
                    Surface(modifier = Modifier.clickable { selectedCategory = g }, shape = RoundedCornerShape(20.dp), color = if (g == selectedCategory) Primary else MaterialTheme.colorScheme.surface, border = if (g != selectedCategory) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null) {
                        Text(g, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 13.sp, fontWeight = if (g == selectedCategory) FontWeight.Bold else FontWeight.Medium, color = if (g == selectedCategory) Color.White else OnSurfaceVariant)
                    }
                }
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredBooks) { b ->
                AllBooksListItem(b, bookProgress[b.id]) { onBookClick(b.id) }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun AllBooksListItem(book: TingBook, progress: TingProgress?, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp).clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = ImageRequest.Builder(ctx).data(book.coverUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(book.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (book.isFavorite) {
                        Icon(Icons.Default.Favorite, "收藏", tint = Primary, modifier = Modifier.size(14.dp).padding(start = 4.dp))
                    }
                }
                val sub = buildString { if (book.author.isNotBlank()) append(book.author); if (book.narrator?.isNotBlank() == true) append(" · ${book.narrator}") }
                Text(sub, fontSize = 11.sp, color = OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (progress != null) { Spacer(modifier = Modifier.height(4.dp)); val p = if (progress.duration != null && progress.duration > 0) (progress.position / progress.duration).toFloat().coerceIn(0f, 1f) else 0f; Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))) { Box(modifier = Modifier.fillMaxWidth(p).height(3.dp).background(Primary, RoundedCornerShape(2.dp))) } }
            }
            if (progress != null && progress.position > 0) { Surface(shape = RoundedCornerShape(8.dp), color = PrimaryBg) { Text("继续", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Primary) } }
        }
    }
}

@Composable
private fun GridItem(book: TingBook, progress: TingProgress?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Box {
            AsyncImage(model = ImageRequest.Builder(ctx).data(book.coverUrl).crossfade(true).memoryCacheKey("lib_${book.id}").build(), contentDescription = book.title, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
            if (progress != null && progress.position > 0) {
                val p = if (progress.duration != null && progress.duration > 0) (progress.position / progress.duration).toFloat().coerceIn(0f, 1f) else 0f
                Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp).background(Color.Black.copy(alpha = 0.08f))) { Box(modifier = Modifier.fillMaxWidth(p).height(4.dp).background(Primary)) }
            }
        }
        Text(book.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        Text(book.author, fontSize = 10.sp, color = OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun ListItem(book: TingBook, progress: TingProgress?, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp).clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = ImageRequest.Builder(ctx).data(book.coverUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(book.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (book.isFavorite) {
                        Icon(Icons.Default.Favorite, "收藏", tint = Primary, modifier = Modifier.size(14.dp).padding(start = 4.dp))
                    }
                }
                val sub = buildString { if (book.author.isNotBlank()) append(book.author); if (book.narrator?.isNotBlank() == true) append(" · ${book.narrator}") }
                Text(sub, fontSize = 11.sp, color = OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (progress != null) { Spacer(modifier = Modifier.height(4.dp)); val p = if (progress.duration != null && progress.duration > 0) (progress.position / progress.duration).toFloat().coerceIn(0f, 1f) else 0f; Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))) { Box(modifier = Modifier.fillMaxWidth(p).height(3.dp).background(Primary, RoundedCornerShape(2.dp))) } }
            }
            if (progress != null && progress.position > 0) { Surface(shape = RoundedCornerShape(8.dp), color = PrimaryBg) { Text("继续", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Primary) } }
        }
    }
}


