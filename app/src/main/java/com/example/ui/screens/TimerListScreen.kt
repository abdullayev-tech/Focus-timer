package com.example.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.TimerEntity
import com.example.ui.components.TimerReorderState
import com.example.ui.theme.AppleTimerOrange
import com.example.ui.theme.BrightWhite
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.OLEDBlack
import com.example.ui.theme.StoppedRingArc
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TimerListScreen(
    timers: List<TimerEntity>,
    runningTimerId: Long?,
    runningTimeText: String,
    onAddTimerClick: () -> Unit,
    onTimerSelect: (TimerEntity) -> Unit,
    onDeleteTimer: (TimerEntity) -> Unit,
    onSaveOrder: (List<TimerEntity>) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val edgeSizePx = with(LocalDensity.current) { 72.dp.toPx() }
    val reorder = remember(listState, scope) {
        TimerReorderState(
            initialItems = timers,
            listState = listState,
            scope = scope,
            edgeSizePx = edgeSizePx,
            onSwap = { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) },
            onDrop = { ordered ->
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onSaveOrder(ordered)
            }
        )
    }

    LaunchedEffect(timers) {
        reorder.syncWith(timers)
    }

    Scaffold(
        containerColor = OLEDBlack,
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTimerClick,
                containerColor = BrightWhite,
                contentColor = OLEDBlack,
                shape = CircleShape,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp, end = 12.dp)
                    .size(60.dp)
                    .testTag("add_timer_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Timer",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Timers",
                    color = TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Sound Settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Main Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (reorder.items.isEmpty()) {
                    // Empty State
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = "No Timers",
                                color = TextSecondary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Tap + to create a timer",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
                    ) {
                        items(
                            items = reorder.items,
                            key = { it.id }
                        ) { timer ->
                            val isDragged = reorder.draggedId == timer.id
                            TimerRowCard(
                                timer = timer,
                                runningTimeText = runningTimeText.takeIf { timer.id == runningTimerId },
                                isDragged = isDragged,
                                showDragHandle = reorder.items.size > 1,
                                onClick = { onTimerSelect(timer) },
                                onDragStart = {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    reorder.start(timer.id)
                                },
                                onDrag = { deltaY -> reorder.drag(deltaY) },
                                onDragEnd = { reorder.end() },
                                onDelete = { onDeleteTimer(timer) },
                                modifier = if (isDragged) {
                                    // The held card follows the finger and floats above the others
                                    Modifier
                                        .zIndex(1f)
                                        .graphicsLayer {
                                            translationY = reorder.dragOffset
                                            scaleX = 1.03f
                                            scaleY = 1.03f
                                        }
                                } else {
                                    // The other cards slide out of its way
                                    Modifier.animateItem()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimerRowCard(
    timer: TimerEntity,
    runningTimeText: String?,
    isDragged: Boolean,
    showDragHandle: Boolean,
    onClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (deltaY: Float) -> Unit,
    onDragEnd: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragged) DarkSurfaceVariant else DarkSurface
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                when {
                    isDragged -> StoppedRingArc
                    runningTimeText != null -> AppleTimerOrange
                    else -> DarkBorder
                },
                RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("timer_card_${timer.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (showDragHandle) 6.dp else 18.dp, end = 18.dp)
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // On the side away from delete, so reaching for one never hits the other
            if (showDragHandle) {
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 56.dp)
                        .pointerInput(timer.id) {
                            detectVerticalDragGestures(
                                onDragStart = { onDragStart() },
                                onDragEnd = { onDragEnd() },
                                onDragCancel = { onDragEnd() },
                                onVerticalDrag = { change, deltaY ->
                                    change.consume()
                                    onDrag(deltaY)
                                }
                            )
                        }
                        // Swallows taps, so a missed drag doesn't open the timer
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { }
                        .testTag("drag_handle_${timer.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Drag to reorder",
                        tint = if (isDragged) BrightWhite else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timer.label,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    // A running session shows its live time, so it can't be forgotten
                    text = runningTimeText ?: timer.formattedDuration(),
                    color = if (runningTimeText != null) AppleTimerOrange else TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("delete_timer_${timer.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
