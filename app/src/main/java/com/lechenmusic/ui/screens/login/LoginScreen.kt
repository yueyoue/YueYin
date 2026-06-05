package com.lechenmusic.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.theme.*

@Composable
fun LoginScreen(viewModel: MainViewModel) {
    var serverUrl by remember { mutableStateOf("http://j.tthsdd.top:3001/") }
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("admin123") }
    var passwordVisible by remember { mutableStateOf(false) }
    val isLoading by viewModel.isLoading.collectAsState()
    val loginError by viewModel.loginError.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFFF5F5), Color(0xFFF2F2F7)))), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎧", fontSize = 48.sp); Spacer(modifier = Modifier.height(12.dp))
            Text("悦音听书", fontSize = 28.sp, fontWeight = FontWeight.Black, color = OnBackground)
            Text("连接到你的有声书服务器", fontSize = 14.sp, color = OnSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            Spacer(modifier = Modifier.height(40.dp))
            OutlinedTextField(value = serverUrl, onValueChange = { serverUrl = it }, label = { Text("服务器地址") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), leadingIcon = { Icon(Icons.Default.Language, null) })
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("用户名") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), leadingIcon = { Icon(Icons.Default.Person, null) })
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("密码") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } })
            if (loginError != null) { Spacer(modifier = Modifier.height(8.dp)); Text(loginError!!, color = Primary, fontSize = 13.sp) }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { viewModel.login(serverUrl, username, password) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary), enabled = !isLoading) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("连接服务器", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
