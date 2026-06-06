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
    var showLogout by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editUrl by remember { mutableStateOf("") }
    var editUsername by remember { mutableStateOf("") }
    var editPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
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
        Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            Column {
                MenuItem(Icons.Default.Refresh, "刷新数据") { viewModel.loadBooks() }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline)
                MenuItem(Icons.Default.Brightness4, "深色模式", if (darkMode) "已开启" else "已关闭") { viewModel.setDarkMode(!darkMode) }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline)
                MenuItem(Icons.Default.Update, "检查更新") { viewModel.checkForUpdate(silent = false) }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline)
                MenuItem(Icons.Default.Info, "关于", "悦音听书 v1.0.6") {}
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline)
                MenuItem(Icons.Default.Logout, "退出登录", null, tint = Primary) { showLogout = true }
            }
        }
    }
    if (showLogout) AlertDialog(onDismissRequest = { showLogout = false }, title = { Text("确认退出") }, text = { Text("退出后需要重新输入服务器信息登录") }, confirmButton = { Button(onClick = { viewModel.logout(); showLogout = false }, colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("退出") } }, dismissButton = { TextButton(onClick = { showLogout = false }) { Text("取消") } })
}

@Composable
private fun MenuItem(icon: ImageVector, label: String, sub: String? = null, tint: Color = OnSurfaceVariant, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) { Text(label, fontWeight = FontWeight.Medium, color = tint); if (sub != null) Text(sub, fontSize = 12.sp, color = OnSurfaceVariant) }
        Icon(Icons.Default.ChevronRight, null, tint = OnSurfaceVariant2, modifier = Modifier.size(20.dp))
    }
}
