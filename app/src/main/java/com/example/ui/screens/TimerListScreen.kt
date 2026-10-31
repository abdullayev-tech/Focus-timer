package com.example.ui.screens

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TimerEntity
import com.example.ui.theme.BrightWhite
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.OLEDBlack
import com.example.ui.theme.StoppedRingArc
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimerListScreen(
    timers: List<TimerEntity>,
    onAddTimerClick: () -> Unit,
    onTimerSelect: (TimerEntity) -> Unit,
    onDeleteTimer: (TimerEntity) -> Unit,
    onReorderTimers: (fromIndex: Int, toIndex: Int) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isReorderMode by remember { mutableStateOf(false) }
    val view = LocalView.current

    BackHandler(enabled = isReorderMode) {
        isReorderMode = false
    }

    Scaffold(
        containerColor = OLEDBlack,
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (!isReorderMode) {
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
                Column {
                    Text(
                        text = if (isReorderMode) "Reorder" else "Timers",
                        color = TextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    if (isReorderMode) {
                        Text(
                            text = "Tap arrows to rearrange order",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isReorderMode) {
                        TextButton(
                            onClick = {
                                try {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                } catch (e: Exception) {
                                    // Ignore
                                }
                                isReorderMode = false
                            },
                            modifier = Modifier.testTag("done_reorder_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = BrightWhite,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Done",
                                color = BrightWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
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
                }
            }

            // Main Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (timers.isEmpty()) {
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
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.Bottom,
                        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
                    ) {
                        itemsIndexed(
                            items = timers,
                            key = { _, item -> item.id }
                        ) { index, timer ->
                            TimerRowCard(
                                timer = timer,
                                index = index,
                                totalCount = timers.size,
                                isReorderMode = isReorderMode,
                                onClick = {
                                    if (isReorderMode) {
                                        // In reorder mode, clicking row can do nothing or highlight
                                    } else {
                                        onTimerSelect(timer)
                                    }
                                },
                                onLongClick = {
                                    try {
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                    isReorderMode = true
                                },
                                onMoveUp = {
                                    if (index > 0) {
                                        try {
                                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        } catch (e: Exception) {
                                            // Ignore
                                        }
                                        onReorderTimers(index, index - 1)
                                    }
                                },
                                onMoveDown = {
                                    if (index < timers.size - 1) {
                                        try {
                                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        } catch (e: Exception) {
                                            // Ignore
                                        }
                                        onReorderTimers(index, index + 1)
                                    }
                                },
                                onDelete = { onDeleteTimer(timer) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimerRowCard(
    timer: TimerEntity,
    index: Int,
    totalCount: Int,
    isReorderMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isReorderMode) DarkSurfaceVariant else DarkSurface
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isReorderMode) StoppedRingArc.copy(alpha = 0.5f) else DarkBorder,
                RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("timer_card_${timer.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Drag handle icon in reorder mode
            if (isReorderMode) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Reorder handle",
                    tint = TextSecondary,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(24.dp)
                )
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
                    text = timer.formattedDuration(),
                    color = TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }

            if (isReorderMode) {
                // Up / Down reorder controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("move_up_${timer.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Move Up",
                            tint = if (index > 0) BrightWhite else TextMuted.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalCount - 1,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("move_down_${timer.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Move Down",
                            tint = if (index < totalCount - 1) BrightWhite else TextMuted.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
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
}
