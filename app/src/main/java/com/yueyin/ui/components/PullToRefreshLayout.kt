package com.yueyin.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.launch
import kotlin.math.min

@Composable
fun PullToRefreshLayout(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    triggerDistance: Dp = 120.dp,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val triggerPx = with(density) { triggerDistance.toPx() }
    val maxPullPx = with(density) { 200.dp.toPx() }
    val scope = rememberCoroutineScope()

    var pullOffset by remember { mutableFloatStateOf(0f) }
    var isUserPulling by remember { mutableStateOf(false) }

    // Animated offset for smooth spring-back
    val animatedOffset by animateFloatAsState(
        targetValue = if (isRefreshing && !isUserPulling) with(density) { 60.dp.toPx() } else pullOffset,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "pull"
    )

    // Reset when refresh finishes
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            pullOffset = 0f
            isUserPulling = false
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isRefreshing) return Offset.Zero
                // If pulling down and we have offset, consume upward scroll first
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
                    isUserPulling = true
                    // Apply diminishing resistance as pull further
                    val resistance = 1f - (pullOffset / maxPullPx).coerceIn(0f, 0.7f)
                    val dampened = available.y * resistance * 0.6f
                    pullOffset = min(pullOffset + dampened, maxPullPx)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                isUserPulling = false
                if (!isRefreshing && pullOffset >= triggerPx) {
                    onRefresh()
                    // Keep offset at a small value while refreshing
                    pullOffset = with(density) { 60.dp.toPx() }
                } else if (!isRefreshing) {
                    pullOffset = 0f
                }
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

        // Refresh indicator at the top
        if (animatedOffset > 20f || (isRefreshing && animatedOffset > 0f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
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
