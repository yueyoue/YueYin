package com.yueyin.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yueyin.ui.MainViewModel
import com.yueyin.ui.theme.*

@Composable
fun ProfileScreen(viewModel: MainViewModel) {
    val url by viewModel.tingServerUrl.collectAsState()
    val darkMode by viewModel.darkMode.collectAsState()
    val cacheSizeGB by viewModel.cacheSizeGB.collectAsState()
    var showLogout by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editUrl by remember { mutableStateOf("") }
    var editUsername by remember { mutableStateOf("") }
    var editPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // Cache sizes
    var audioCacheSize by remember { mutableStateOf(0L) }
    var otherCacheSize by remember { mutableStateOf(0L) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val (audio, other) = viewModel.computeCacheSizes()
        audioCacheSize = audio
        otherCacheSize = other
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(48.dp))
        Text("👤 我的", fontSize = 28.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("服务器信息", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                    if (!isEditing) {
                        TextButton(onClick = {
                            editUrl = url
                            editUsername = ""
                            editPassword = ""
                            isEditing = true
                        }) { Text("编辑", fontSize = 13.sp) }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (isEditing) {
                    OutlinedTextField(
                        value = editUrl, onValueChange = { editUrl = it },
                        label = { Text("服务器地址") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editUsername, onValueChange = { editUsername = it },
                        label = { Text("用户名") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editPassword, onValueChange = { editPassword = it },
                        label = { Text("密码") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, "显示密码")
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { isEditing = false }) { Text("取消") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (editUrl.isNotBlank() && editUsername.isNotBlank() && editPassword.isNotBlank()) {
                                    viewModel.updateServerInfo(editUrl, editUsername, editPassword)
                                    isEditing = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("保存") }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Language, null, tint = Primary, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)); Text(url, fontSize = 13.sp) }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Cache settings card
        Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, null, tint = Primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("缓存设置", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("缓存空间大小", fontSize = 14.sp, color = OnSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 4, 8).forEach { gb ->
                        val isSelected = cacheSizeGB == gb
                        Surface(
                            modifier = Modifier.weight(1f).clickable { viewModel.setCacheSize(gb) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Primary else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                "${gb}G",
                                modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally),
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("存储空间", fontSize = 14.sp, color = OnSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("有声数据缓存", fontSize = 13.sp, color = OnSurfaceVariant)
                            Text(formatFileSize(audioCacheSize), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("图片及其他缓存", fontSize = 13.sp, color = OnSurfaceVariant)
                            Text(formatFileSize(otherCacheSize), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("总计", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(formatFileSize(audioCacheSize + otherCacheSize), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Primary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showClearCacheDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("清除缓存")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            Column {
                MenuItem(Icons.Default.Refresh, "刷新数据") { viewModel.loadBooks() }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline)
                MenuItem(Icons.Default.Brightness4, "深色模式", if (darkMode) "已开启" else "已关闭") { viewModel.setDarkMode(!darkMode) }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline)
                MenuItem(Icons.Default.Update, "检查更新") { viewModel.checkForUpdate(silent = false) }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline)
                MenuItem(Icons.Default.Info, "关于", "悦音听书 v${com.yueyin.BuildConfig.VERSION_NAME}") {}
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline)
                MenuItem(Icons.Default.Logout, "退出登录", null, tint = Primary) { showLogout = true }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
    if (showLogout) AlertDialog(onDismissRequest = { showLogout = false }, title = { Text("确认退出") }, text = { Text("退出后需要重新输入服务器信息登录") }, confirmButton = { Button(onClick = { viewModel.logout(); showLogout = false }, colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("退出") } }, dismissButton = { TextButton(onClick = { showLogout = false }) { Text("取消") } })

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("清除缓存") },
            text = { Text("确定要清除所有缓存数据吗？有声数据和图片缓存将被清除，下次播放需要重新下载。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCache()
                        audioCacheSize = 0L
                        otherCacheSize = 0L
                        showClearCacheDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("清除") }
            },
            dismissButton = { TextButton(onClick = { showClearCacheDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun MenuItem(icon: ImageVector, label: String, sub: String? = null, tint: Color = OnSurfaceVariant, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) { Text(label, fontWeight = FontWeight.Medium, color = tint); if (sub != null) Text(sub, fontSize = 12.sp, color = OnSurfaceVariant) }
        Icon(Icons.Default.ChevronRight, null, tint = OnSurfaceVariant2, modifier = Modifier.size(20.dp))
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    return "%.1f %s".format(size, units[unitIndex])
}
