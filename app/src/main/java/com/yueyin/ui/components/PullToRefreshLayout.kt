package com.yueyin.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.yueyin.ui.theme.Primary
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

@Composable
fun PullToRefreshLayout(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    triggerDistance: Dp = 100.dp,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val triggerPx = with(density) { triggerDistance.toPx() }
    val scope = rememberCoroutineScope()

    var pullOffset by remember { mutableFloatStateOf(0f) }
    var isTriggered by remember { mutableStateOf(false) }

    // Reset when refresh finishes
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            isTriggered = false
            pullOffset = 0f
        }
    }

    val animatedOffset by animateFloatAsState(targetValue = pullOffset, label = "pull")

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isRefreshing) return Offset.Zero
                // If pulling down and we have offset, consume it
                if (pullOffset > 0 && available.y < 0) {
                    val consumed = min(pullOffset, -available.y)
                    pullOffset -= consumed
                    return Offset(0f, -consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(available: Offset, consumed: Offset, source: NestedScrollSource): Offset {
                if (isRefreshing) return Offset.Zero
                // Only allow pull when at top (no more content consumed upward)
                if (available.y > 0 && consumed.y == 0f) {
                    val dampened = available.y * 0.5f
                    pullOffset = min(pullOffset + dampened, triggerPx * 1.5f)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!isRefreshing && pullOffset >= triggerPx) {
                    isTriggered = true
                    onRefresh()
                }
                // Snap back
                scope.launch {
                    pullOffset = 0f
                }
                return Velocity.Zero
            }
        }
    }

    Box(modifier = modifier.nestedScroll(nestedScrollConnection)) {
        content()

        // Refresh indicator
        val indicatorOffset = with(density) { (animatedOffset * 0.6f).toDp() }
        if (animatedOffset > 10f || isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = (indicatorOffset - 20.dp).coerceAtLeast(0.dp)),
                contentAlignment = Alignment.TopCenter
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Primary,
                    strokeWidth = 2.5.dp
                )
            }
        }
    }
}
