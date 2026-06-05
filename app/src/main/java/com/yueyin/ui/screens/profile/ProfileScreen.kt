package com.yueyin.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yueyin.ui.MainViewModel
import com.yueyin.ui.theme.*

@Composable
fun ProfileScreen(viewModel: MainViewModel) {
    val url by viewModel.tingServerUrl.collectAsState()
    var showLogout by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(48.dp))
        Text("👤 我的", fontSize = 28.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(16.dp), color = Surface, border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("服务器信息", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant); Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Language, null, tint = Primary, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)); Text(url, fontSize = 13.sp) }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(16.dp), color = Surface, border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
            Column {
                MenuItem(Icons.Default.Refresh, "刷新数据") { viewModel.loadBooks() }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorderLight)
                MenuItem(Icons.Default.Update, "检查更新") { viewModel.checkForUpdate(silent = false) }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorderLight)
                MenuItem(Icons.Default.Info, "关于", "悦音听书 v1.0.4") {}
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable { showLogout = true }, shape = RoundedCornerShape(16.dp), color = Surface, border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(0.3f))) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Logout, null, tint = Primary, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("退出登录", fontWeight = FontWeight.Medium, color = Primary) }
        }
    }
    if (showLogout) AlertDialog(onDismissRequest = { showLogout = false }, title = { Text("确认退出") }, text = { Text("退出后需要重新输入服务器信息登录") }, confirmButton = { Button(onClick = { viewModel.logout(); showLogout = false }, colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("退出") } }, dismissButton = { TextButton(onClick = { showLogout = false }) { Text("取消") } })
}

@Composable
private fun MenuItem(icon: ImageVector, label: String, sub: String? = null, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) { Text(label, fontWeight = FontWeight.Medium); if (sub != null) Text(sub, fontSize = 12.sp, color = OnSurfaceVariant) }
        Icon(Icons.Default.ChevronRight, null, tint = OnSurfaceVariant2, modifier = Modifier.size(20.dp))
    }
}
