package com.yueyin.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import coil.request.ImageRequest
import com.yueyin.data.model.TingBook
import com.yueyin.data.model.TingLibrary
import com.yueyin.data.model.TingProgress
import com.yueyin.ui.MainViewModel
import com.yueyin.ui.theme.*

@Composable
fun DiscoverScreen(viewModel: MainViewModel, onBookClick: (String) -> Unit, onSettingsClick: () -> Unit) {
    val allBooks by viewModel.allBooks.collectAsState()
    val bookProgress by viewModel.bookProgress.collectAsState()
    val booksLoading by viewModel.booksLoading.collectAsState()
    val tingServerUrl by viewModel.tingServerUrl.collectAsState()
    val libraries by viewModel.libraries.collectAsState()
    val libraryBooks by viewModel.libraryBooks.collectAsState()
    val libraryBooksLoading by viewModel.libraryBooksLoading.collectAsState()
    val context = LocalContext.current
    val recentBooks = remember(allBooks, bookProgress) { viewModel.getRecentBooks() }
    val listState = rememberLazyListState()

    // Daily random recommendation based on day of year
    val dailyBook = remember(allBooks) {
        if (allBooks.isEmpty()) null
        else {
            val dayOfYear = java.time.LocalDate.now().dayOfYear
            allBooks[dayOfYear % allBooks.size]
        }
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item { Spacer(modifier = Modifier.height(48.dp)) }
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (dailyBook != null) {
            item {
                val f = dailyBook
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(200.dp).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(Color(0xFFFFF5F5), Color(0xFFFFEEEA), Color(0xFFFFE5E0))))) {
                    AsyncImage(model = ImageRequest.Builder(context).data(f.coverUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop, alpha = 0.18f)
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                        Surface(shape = RoundedCornerShape(6.dp), color = Primary) { Text("🔥 编辑推荐", modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(f.title, fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                        val sub = buildString {
                            if (f.author.isNotBlank()) append(f.author)
                            if (f.narrator?.isNotBlank() == true) append(" · 播音：${f.narrator}")
                        }
                        if (sub.isNotBlank()) Text(sub, fontSize = 12.sp, color = OnSurfaceVariant, modifier = Modifier.padding(top = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { onBookClick(f.id) }, shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(6.dp)); Text("立即收听", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        if (recentBooks.isNotEmpty()) {
            item { Text("📖 继续收听", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) }
            item { recentBooks.forEach { b -> ContinueCard(b, bookProgress[b.id], onClick = { onBookClick(b.id) }) } }
        }

        // Per-library sections: each library shows 9 newest books
        if (libraryBooksLoading) {
            item { Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) } }
        } else if (libraries.isNotEmpty()) {
            libraries.forEach { lib ->
                val books = libraryBooks[lib.id] ?: emptyList()
                if (books.isNotEmpty()) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("📚 ${lib.name}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("${books.size}本", fontSize = 12.sp, color = OnSurfaceVariant)
                        }
                    }
                    // Show books in a 3-column grid (3 rows × 3 cols = 9 books)
                    books.chunked(3).forEach { row ->
                        item {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                row.forEach { b -> BookGridItem(b, bookProgress[b.id], Modifier.weight(1f)) { onBookClick(b.id) } }
                                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }
                }
            }
        } else if (!booksLoading && allBooks.isEmpty()) {
            item { Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("📚", fontSize = 48.sp); Spacer(modifier = Modifier.height(8.dp)); Text("暂无有声书", color = OnSurfaceVariant) } } }
        }

        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
private fun ContinueCard(book: TingBook, progress: TingProgress?, onClick: () -> Unit) {
    val context = LocalContext.current
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp).clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = ImageRequest.Builder(context).data(book.coverUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(progress?.chapterTitle ?: book.author, fontSize = 12.sp, color = OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(8.dp))
                val p = if (progress != null && progress.duration != null && progress.duration > 0) (progress.position / progress.duration).toFloat().coerceIn(0f, 1f) else 0f
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))) { Box(modifier = Modifier.fillMaxWidth(p).height(4.dp).background(Primary, RoundedCornerShape(2.dp))) }
            }
        }
    }
}

@Composable
private fun BookGridItem(book: TingBook, progress: TingProgress?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Box {
            AsyncImage(model = ImageRequest.Builder(context).data(book.coverUrl).crossfade(true).memoryCacheKey("book_${book.id}").build(), contentDescription = book.title, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
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
private fun DiscoverListItem(book: TingBook, progress: TingProgress?, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp).clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = ImageRequest.Builder(ctx).data(book.coverUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
