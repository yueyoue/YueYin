package com.yueyin.ui.screens.discover

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
import com.yueyin.data.model.TingBook
import com.yueyin.data.model.TingProgress
import com.yueyin.ui.MainViewModel
import com.yueyin.ui.theme.*

@Composable
fun LibraryDetailScreen(
    libraryId: String,
    libraryName: String,
    viewModel: MainViewModel,
    onBookClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val books by viewModel.libraryDetailBooks.collectAsState()
    val loading by viewModel.libraryDetailLoading.collectAsState()
    val bookProgress by viewModel.bookProgress.collectAsState()
    var viewMode by remember { mutableStateOf("grid") }

    LaunchedEffect(libraryId) {
        viewModel.loadLibraryDetailBooks(libraryId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(48.dp))
        // Top bar
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") }
            Text(libraryName, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${books.size}本", fontSize = 14.sp, color = OnSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
            IconButton(onClick = { viewMode = "grid" }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.GridView, "网格", tint = if (viewMode == "grid") Primary else OnSurfaceVariant, modifier = Modifier.size(20.dp)) }
            IconButton(onClick = { viewMode = "list" }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ViewList, "列表", tint = if (viewMode == "list") Primary else OnSurfaceVariant, modifier = Modifier.size(20.dp)) }
        }

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) }
        } else if (books.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📚", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("暂无有声书", color = OnSurfaceVariant)
                }
            }
        } else {
            if (viewMode == "grid") {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    books.chunked(3).forEach { row ->
                        item {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                row.forEach { b -> DetailGridItem(b, bookProgress[b.id], Modifier.weight(1f)) { onBookClick(b.id) } }
                                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(books) { b -> DetailListItem(b, bookProgress[b.id]) { onBookClick(b.id) } }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }
    }
}

@Composable
private fun DetailGridItem(book: TingBook, progress: TingProgress?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Box {
            AsyncImage(model = ImageRequest.Builder(ctx).data(book.coverUrl).crossfade(true).memoryCacheKey("detail_${book.id}").build(), contentDescription = book.title, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
            if (progress != null && progress.position > 0) {
                val p = if (progress.duration != null && progress.duration > 0) (progress.position / progress.duration).toFloat().coerceIn(0f, 1f) else 0f
                Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp).background(Color.Black.copy(alpha = 0.08f))) { Box(modifier = Modifier.fillMaxWidth(p).height(4.dp).background(Primary)) }
            }
        }
        Text(book.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        Text(book.narrator?.takeIf { it.isNotBlank() } ?: book.author, fontSize = 10.sp, color = OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun DetailListItem(book: TingBook, progress: TingProgress?, onClick: () -> Unit) {
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
                if (progress != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val p = if (progress.duration != null && progress.duration > 0) (progress.position / progress.duration).toFloat().coerceIn(0f, 1f) else 0f
                    Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))) { Box(modifier = Modifier.fillMaxWidth(p).height(3.dp).background(Primary, RoundedCornerShape(2.dp))) }
                }
            }
            if (progress != null && progress.position > 0) { Surface(shape = RoundedCornerShape(8.dp), color = PrimaryBg) { Text("继续", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Primary) } }
        }
    }
}
