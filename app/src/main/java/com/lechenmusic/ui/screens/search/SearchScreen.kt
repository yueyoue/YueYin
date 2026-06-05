package com.lechenmusic.ui.screens.search

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.theme.*

@Composable
fun SearchScreen(viewModel: MainViewModel, onBookClick: (String) -> Unit) {
    val q by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val ctx = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(48.dp))
        Text("🔍 搜索", fontSize = 28.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        OutlinedTextField(value = q, onValueChange = { viewModel.searchBooks(it) }, placeholder = { Text("搜索有声书...") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), shape = RoundedCornerShape(12.dp), leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = { if (q.isNotBlank()) IconButton(onClick = { viewModel.searchBooks("") }) { Icon(Icons.Default.Clear, "清除") } })
        if (results.isEmpty() && q.isNotBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("🔍", fontSize = 48.sp); Spacer(modifier = Modifier.height(8.dp)); Text("未找到相关有声书", color = OnSurfaceVariant) } }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(results) { b ->
                    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp).clickable { onBookClick(b.id) }, shape = RoundedCornerShape(12.dp), color = Surface, border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(model = ImageRequest.Builder(ctx).data(b.coverUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(b.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val sub = buildString { if (b.author.isNotBlank()) append(b.author); if (b.narrator?.isNotBlank() == true) append(" · ${b.narrator}") }
                                Text(sub, fontSize = 11.sp, color = OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}
