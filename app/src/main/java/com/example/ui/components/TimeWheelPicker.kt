package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.abs
import kotlin.math.cos

private val WheelRowHeight = 44.dp
private const val WheelVisibleRows = 5

// Each wheel repeats its numbers this many times, and starts in the middle, so it feels endless
private const val WheelLoops = 400

/**
 * Apple-style time picker: three scrolling wheels for hours, minutes and seconds.
 * The row in the middle band is the selected one.
 */
@Composable
fun TimeWheelPicker(
    initialHours: Int,
    initialMinutes: Int,
    initialSeconds: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    onSecondsChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(WheelRowHeight * WheelVisibleRows),
        contentAlignment = Alignment.Center
    ) {
        // Selection band behind the middle row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(WheelRowHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurfaceVariant)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            NumberWheel(
                initialValue = initialHours,
                valueCount = 24,
                unit = "hours",
                onValueChange = onHoursChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("wheel_hours")
            )
            NumberWheel(
                initialValue = initialMinutes,
                valueCount = 60,
                unit = "min",
                onValueChange = onMinutesChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("wheel_minutes")
            )
            NumberWheel(
                initialValue = initialSeconds,
                valueCount = 60,
                unit = "sec",
                onValueChange = onSecondsChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("wheel_seconds")
            )
        }
    }
}

@Composable
private fun NumberWheel(
    initialValue: Int,
    valueCount: Int,
    unit: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = WheelLoops / 2 * valueCount + initialValue - WheelVisibleRows / 2
    )

    LaunchedEffect(listState) {
        snapshotFlow { listState.middleItemIndex() }
            .filterNotNull()
            .drop(1) // The first value is the wheel's starting point, not a change
            .collect { index ->
                onValueChange(index % valueCount)
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
    }

    Box(modifier = modifier.fillMaxHeight()) {
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            modifier = Modifier.fillMaxSize()
        ) {
            items(valueCount * WheelLoops) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WheelRowHeight)
                        .graphicsLayer {
                            // Rows flatten and fade away from the middle, like the side of a wheel
                            val info = listState.layoutInfo
                            val item = info.visibleItemsInfo.firstOrNull { it.index == index }
                                ?: return@graphicsLayer
                            val middle = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                            val rowsAway = (item.offset + item.size / 2f - middle) / item.size
                            scaleY = cos(rowsAway * 0.4f).coerceAtLeast(0f)
                            alpha = (1f - abs(rowsAway) * 0.32f).coerceAtLeast(0f)
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = (index % valueCount).toString(),
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(0.5f)
                    )
                }
            }
        }

        // The unit stays still beside the middle row while the numbers scroll
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = unit,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.dp)
            )
        }
    }
}

private fun LazyListState.middleItemIndex(): Int? {
    val info = layoutInfo
    val middle = (info.viewportStartOffset + info.viewportEndOffset) / 2
    return info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - middle) }?.index
}
