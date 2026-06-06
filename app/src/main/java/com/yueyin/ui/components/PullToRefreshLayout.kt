package com.yueyin.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.yueyin.ui.theme.Primary
import kotlin.math.abs
import kotlin.math.min

@Composable
fun PullToRefreshLayout(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    triggerDistance: Dp = 120.dp,
    listState: LazyListState? = null,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val triggerPx = with(density) { triggerDistance.toPx() }
    val maxPullPx = with(density) { 200.dp.toPx() }

    var pullOffset by remember { mutableFloatStateOf(0f) }
    // Track whether content has been scrolled in current gesture
    var contentScrolledInGesture by remember { mutableStateOf(false) }

    // Animated offset for smooth spring-back
    val animatedOffset by animateFloatAsState(
        targetValue = if (isRefreshing) with(density) { 50.dp.toPx() } else pullOffset,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "pull"
    )

    // Reset when refresh finishes
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            pullOffset = 0f
            contentScrolledInGesture = false
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isRefreshing) return Offset.Zero

                // When user scrolls UP (negative y) while we have pull offset, consume it
                if (available.y < 0 && pullOffset > 0) {
                    val consumed = min(pullOffset, -available.y)
                    pullOffset -= consumed
                    return Offset(0f, -consumed)
                }

                // When user scrolls UP (negative y), mark that content has been scrolled
                // This prevents pull-to-refresh from activating during normal scrolling
                if (available.y < 0) {
                    contentScrolledInGesture = true
                }

                // When user scrolls DOWN (positive y) and is at the very top,
                // AND content was previously scrolled in this gesture,
                // reset the flag — this means user reversed direction back to top
                val isAtTop = listState == null ||
                    (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0)
                if (available.y > 0 && isAtTop && contentScrolledInGesture) {
                    contentScrolledInGesture = false
                }

                return Offset.Zero
            }

            override fun onPostScroll(available: Offset, consumed: Offset, source: NestedScrollSource): Offset {
                if (isRefreshing) return Offset.Zero

                // Only activate pull-to-refresh when ALL conditions are met:
                // 1. User is pulling down (available.y > 0)
                // 2. No content was scrolled in this gesture
                // 3. We're at the very top of the list
                val isAtTop = listState == null ||
                    (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0)

                if (available.y > 0 && !contentScrolledInGesture && isAtTop) {
                    val resistance = 1f - (pullOffset / maxPullPx).coerceIn(0f, 0.7f)
                    pullOffset = min(pullOffset + available.y * resistance * 0.4f, maxPullPx)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!isRefreshing && pullOffset >= triggerPx) {
                    onRefresh()
                }
                if (!isRefreshing) {
                    pullOffset = 0f
                }
                contentScrolledInGesture = false
                return Velocity.Zero
            }
        }
    }

    Box(modifier = modifier.nestedScroll(nestedScrollConnection)) {
        // Content moves down as a whole
        Box(
            modifier = Modifier.graphicsLayer {
                translationY = animatedOffset
            }
        ) {
            content()
        }

        // Refresh indicator
        if (animatedOffset > 10f || (isRefreshing && animatedOffset > 0f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    color = Primary,
                    strokeWidth = 2.5.dp
                )
            }
        }
    }
}
