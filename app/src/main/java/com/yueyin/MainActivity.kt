package com.yueyin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yueyin.ui.MainViewModel
import com.yueyin.ui.components.MiniPlayer
import com.yueyin.ui.navi.Screen
import com.yueyin.ui.screens.discover.DiscoverScreen
import com.yueyin.ui.screens.library.LibraryScreen
import com.yueyin.ui.screens.login.LoginScreen
import com.yueyin.ui.screens.player.PlayerScreen
import com.yueyin.ui.screens.profile.ProfileScreen
import com.yueyin.ui.screens.search.SearchScreen
import com.yueyin.ui.theme.YueYinTheme
import com.yueyin.update.UpdateInfo

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            YueYinTheme {
                val updateInfo by viewModel.updateInfo.collectAsState()
                val updateStatus by viewModel.updateStatus.collectAsState()
                if (updateInfo != null) {
                    AlertDialog(
                        onDismissRequest = { viewModel.dismissUpdate() },
                        title = { Text("发现新版本 v${updateInfo!!.versionName}", fontWeight = FontWeight.Bold) },
                        text = {
                            Column {
                                if (updateInfo!!.updateLog.isNotEmpty()) Text(updateInfo!!.updateLog, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (!updateStatus.isNullOrBlank()) { Spacer(modifier = Modifier.height(12.dp)); LinearProgressIndicator(modifier = Modifier.fillMaxWidth()); Spacer(modifier = Modifier.height(8.dp)); Text(updateStatus!!, color = MaterialTheme.colorScheme.primary) }
                            }
                        },
                        confirmButton = { Button(onClick = { viewModel.downloadUpdate() }, enabled = updateStatus.isNullOrBlank()) { Text("立即更新") } },
                        dismissButton = { TextButton(onClick = { viewModel.dismissUpdate() }) { Text("跳过") } }
                    )
                }
                YueYinMain(viewModel)
            }
        }
    }
}

@Composable
fun YueYinMain(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val ap = viewModel.audiobookPlayerManager
    val bookId by ap.currentBookId.collectAsState()
    val isPlaying by ap.isPlaying.collectAsState()
    val navEntry by navController.currentBackStackEntryAsState()
    val route = navEntry?.destination?.route

    data class Tab(val route: String, val label: String, val icon: ImageVector)
    val tabs = listOf(Tab(Screen.Discover.route, "首页", Icons.Default.Home), Tab(Screen.Library.route, "书架", Icons.Default.MenuBook), Tab(Screen.Search.route, "搜索", Icons.Default.Search), Tab(Screen.Profile.route, "我的", Icons.Default.Person))
    val showBar = route in tabs.map { it.route }
    val isOnPlayer = route?.startsWith("player") == true

    LaunchedEffect(isLoggedIn) { if (isLoggedIn) viewModel.checkForUpdate() }

    val snackHost = remember { SnackbarHostState() }
    val toast by viewModel.toastMessage.collectAsState()
    LaunchedEffect(toast) { toast?.let { snackHost.showSnackbar(it); viewModel.clearToast() } }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isLoggedIn) { LoginScreen(viewModel = viewModel) }
        else {
            Scaffold(snackbarHost = { SnackbarHost(snackHost) }, bottomBar = {
                AnimatedVisibility(visible = showBar || (bookId.isNotBlank() && !isOnPlayer), enter = slideInVertically(initialOffsetY = { it }), exit = slideOutVertically(targetOffsetY = { it })) {
                    Column {
                        if (bookId.isNotBlank() && !isOnPlayer && !isPlaying) MiniPlayer(playerManager = ap, onClick = { navController.navigate(Screen.Player.createRoute(bookId)) })
                        if (showBar) NavigationBar(containerColor = androidx.compose.ui.graphics.Color.White.copy(0.88f)) {
                            tabs.forEach { t ->
                                NavigationBarItem(icon = { Icon(t.icon, t.label) }, label = { Text(t.label, fontSize = 10.sp) }, selected = route == t.route, onClick = {
                                    if (route == t.route) return@NavigationBarItem
                                    navController.navigate(t.route) { popUpTo(Screen.Discover.route) { saveState = false }; launchSingleTop = true; restoreState = false }
                                })
                            }
                        }
                    }
                }
            }) { pv ->
                NavHost(navController = navController, startDestination = Screen.Discover.route, modifier = Modifier.fillMaxSize().padding(pv)) {
                    composable(Screen.Discover.route) { DiscoverScreen(viewModel, onBookClick = { navController.navigate(Screen.Player.createRoute(it)) }, onSettingsClick = { navController.navigate(Screen.Profile.route) }) }
                    composable(Screen.Library.route) { LibraryScreen(viewModel, onBookClick = { navController.navigate(Screen.Player.createRoute(it)) }) }
                    composable(Screen.Search.route) { SearchScreen(viewModel, onBookClick = { navController.navigate(Screen.Player.createRoute(it)) }) }
                    composable(Screen.Profile.route) { ProfileScreen(viewModel) }
                    composable(Screen.Player.route) { val bid = it.arguments?.getString("bookId") ?: ""; PlayerScreen(bid, viewModel) { navController.popBackStack() } }
                }
            }
        }
    }
}
