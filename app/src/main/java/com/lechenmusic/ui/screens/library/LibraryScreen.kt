package com.lechenmusic.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.lechenmusic.data.model.TingBook
import com.lechenmusic.data.model.TingProgress
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.theme.*

@Composable
fun LibraryScreen(viewModel: MainViewModel, onBookClick: (String) -> Unit) {
    val allBooks by viewModel.allBooks.collectAsState()
    val bookProgress by viewModel.bookProgress.collectAsState()
    val stats by viewModel.stats.collectAsState()
    var viewMode by remember { mutableStateOf("grid") }
    var filterMode by remember { mutableStateOf("全部") }
    val filtered = remember(allBooks, bookProgress, filterMode) { when (filterMode) { "在听" -> allBooks.filter { bookProgress.containsKey(it.id) }; "未开始" -> allBooks.filter { !bookProgress.containsKey(it.id) }; else -> allBooks } }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(48.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("📚 书架", fontSize = 28.sp, fontWeight = FontWeight.Black)
            IconButton(onClick = { viewModel.loadBooks() }) { Icon(Icons.Default.Refresh, "刷新", tint = OnSurfaceVariant) }
        }
        Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(16.dp), color = Surface, border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
            Row(modifier = Modifier.padding(vertical = 14.dp)) {
                listOf("${stats.totalBooks}" to "书籍", "${allBooks.count { bookProgress.containsKey(it.id) }}" to "在听", "${bookProgress.size}" to "进度").forEach { (n, l) ->
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { Text(n, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Primary); Text(l, fontSize = 11.sp, color = OnSurfaceVariant, modifier = Modifier.padding(top = 2.dp)) }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("全部", "在听", "未开始").forEach { f ->
                Surface(modifier = Modifier.clickable { filterMode = f }, shape = RoundedCornerShape(20.dp), color = if (filterMode == f) Primary else Surface, border = if (filterMode != f) androidx.compose.foundation.BorderStroke(1.dp, Border) else null) {
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
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp).clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), color = Surface, border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = ImageRequest.Builder(ctx).data(book.coverUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = buildString { if (book.author.isNotBlank()) append(book.author); if (book.narrator?.isNotBlank() == true) append(" · ${book.narrator}") }
                Text(sub, fontSize = 11.sp, color = OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (progress != null) { Spacer(modifier = Modifier.height(4.dp)); val p = if (progress.duration != null && progress.duration > 0) (progress.position / progress.duration).toFloat().coerceIn(0f, 1f) else 0f; Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Border, RoundedCornerShape(2.dp))) { Box(modifier = Modifier.fillMaxWidth(p).height(3.dp).background(Primary, RoundedCornerShape(2.dp))) } }
            }
            if (progress != null && progress.position > 0) { Surface(shape = RoundedCornerShape(8.dp), color = PrimaryBg) { Text("继续", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Primary) } }
        }
    }
}
