package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.example.data.TimerEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// How fast the list scrolls, in pixels per frame, when a card is held near its edge
private const val AUTO_SCROLL_SPEED = 14f

/**
 * Drag-to-reorder for the timer list.
 *
 * While a card is held, the order lives here and nothing is saved, so the list follows the finger
 * without waiting for the database. The new order is saved once, when the card is dropped.
 */
class TimerReorderState(
    initialItems: List<TimerEntity>,
    private val listState: LazyListState,
    private val scope: CoroutineScope,
    private val edgeSizePx: Float,
    private val onSwap: () -> Unit,
    private val onDrop: (List<TimerEntity>) -> Unit
) {
    var items by mutableStateOf(initialItems)
        private set

    var draggedId by mutableStateOf<Long?>(null)
        private set

    // How far the held card sits from its place in the list
    var dragOffset by mutableFloatStateOf(0f)
        private set

    private var lastDelta = 0f
    private var autoScrollJob: Job? = null
    private var settleJob: Job? = null

    fun syncWith(timers: List<TimerEntity>) {
        if (draggedId == null) items = timers
    }

    fun start(id: Long) {
        settleJob?.cancel()
        draggedId = id
        dragOffset = 0f
        lastDelta = 0f
        autoScrollJob?.cancel()
        autoScrollJob = scope.launch {
            while (isActive) {
                withFrameNanos { }
                autoScrollOneFrame()
            }
        }
    }

    fun drag(deltaY: Float) {
        if (deltaY != 0f) lastDelta = deltaY
        dragOffset += deltaY
        swapIfCrossed()
    }

    fun end() {
        autoScrollJob?.cancel()
        if (draggedId == null) return
        onDrop(items)
        settleJob = scope.launch {
            animate(
                initialValue = dragOffset,
                targetValue = 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) { value, _ ->
                dragOffset = value
            }
            draggedId = null
        }
    }

    private fun swapIfCrossed() {
        val id = draggedId ?: return
        val info = listState.layoutInfo
        val dragged = info.visibleItemsInfo.firstOrNull { it.key == id } ?: return

        // The list hasn't been laid out since the last swap yet
        if (items.getOrNull(dragged.index)?.id != id) return

        val draggedMiddle = dragged.offset + dragOffset + dragged.size / 2f
        val target = info.visibleItemsInfo.firstOrNull {
            it.key != id && draggedMiddle >= it.offset && draggedMiddle <= it.offset + it.size
        } ?: return

        // The card's place in the list jumps; move the offset the other way so it stays under the finger
        val newOffset = if (target.index > dragged.index) {
            target.offset + target.size - dragged.size
        } else {
            target.offset
        }
        dragOffset += dragged.offset - newOffset

        // Stop the list from scrolling along with the first visible card when it moves
        val firstIndex = listState.firstVisibleItemIndex
        if (dragged.index == firstIndex || target.index == firstIndex) {
            listState.requestScrollToItem(firstIndex, listState.firstVisibleItemScrollOffset)
        }

        items = items.toMutableList().apply { add(target.index, removeAt(dragged.index)) }
        onSwap()
    }

    private suspend fun autoScrollOneFrame() {
        val id = draggedId ?: return
        val info = listState.layoutInfo
        val dragged = info.visibleItemsInfo.firstOrNull { it.key == id } ?: return

        val top = dragged.offset + dragOffset
        val bottom = top + dragged.size
        val speed = when {
            lastDelta < 0f && top < info.viewportStartOffset + edgeSizePx -> -AUTO_SCROLL_SPEED
            lastDelta > 0f && bottom > info.viewportEndOffset - edgeSizePx -> AUTO_SCROLL_SPEED
            else -> return
        }

        val scrolled = listState.scrollBy(speed)
        if (scrolled != 0f) {
            dragOffset += scrolled
            swapIfCrossed()
        }
    }
}
