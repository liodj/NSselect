package com.example.nsselect.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.nsselect.data.ItemEntity
import kotlinx.coroutines.delay

@Composable
internal fun ReorderablePlaylist(
    songs: List<ItemEntity>, enabled: Boolean, onMove: (Int, Int) -> Unit,
    onSongClick: (Int) -> Unit, modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val currentSongs by rememberUpdatedState(songs)
    val move by rememberUpdatedState(onMove)
    var draggedId by remember { mutableStateOf<Int?>(null) }
    var visualTop by remember { mutableFloatStateOf(0f) }
    var draggedHeight by remember { mutableIntStateOf(0) }
    var pendingIndex by remember { mutableStateOf<Int?>(null) }
    val edge = with(LocalDensity.current) { 48.dp.toPx() }

    fun checkMove() {
        val id = draggedId ?: return
        val info = listState.layoutInfo
        val current = info.visibleItemsInfo.firstOrNull { it.key == id } ?: return
        if (pendingIndex != null && current.index != pendingIndex) return
        pendingIndex = null
        val center = visualTop + draggedHeight / 2f
        val target = info.visibleItemsInfo.firstOrNull {
            it.key != id && center in it.offset.toFloat()..(it.offset + it.size).toFloat() &&
                (if (it.index > current.index) center >= it.offset + it.size / 2f else center <= it.offset + it.size / 2f)
        } ?: return
        pendingIndex = target.index
        move(id, target.key as Int)
    }

    LaunchedEffect(draggedId, enabled) {
        while (draggedId != null && enabled) {
            val layout = listState.layoutInfo
            val center = visualTop + draggedHeight / 2f
            val speed = when {
                center < layout.viewportStartOffset + edge -> -((layout.viewportStartOffset + edge - center) / 5f).coerceIn(2f, 18f)
                center > layout.viewportEndOffset - edge -> ((center - layout.viewportEndOffset + edge) / 5f).coerceIn(2f, 18f)
                else -> 0f
            }
            if (speed != 0f) listState.scrollBy(speed)
            checkMove()
            delay(16)
        }
    }

    LazyColumn(modifier.fillMaxWidth(), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            val dragging = draggedId == song.id
            val offset = if (dragging) visualTop - (listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == song.id }?.offset ?: 0) else 0f
            Card(onClick = { onSongClick(song.id) }, enabled = enabled,
                modifier = Modifier.fillMaxWidth().zIndex(if (dragging) 1f else 0f).graphicsLayer { translationY = offset },
                elevation = CardDefaults.cardElevation(defaultElevation = if (dragging) 8.dp else 0.dp),
                colors = CardDefaults.cardColors(containerColor = if (dragging) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer)) {
                Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(28.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("${song.id}. ${song.title}", style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${formatSongDuration(song.durationSeconds)} · ${song.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(Modifier.size(48.dp).semantics {
                        contentDescription = "${song.id}. ${song.title} 순서 변경"
                        customActions = if (enabled) buildList {
                            if (index > 0) add(CustomAccessibilityAction("위로 이동") { move(song.id, currentSongs[index - 1].id); true })
                            if (index < songs.lastIndex) add(CustomAccessibilityAction("아래로 이동") { move(song.id, currentSongs[index + 1].id); true })
                        } else emptyList()
                    }.pointerInput(song.id, enabled) {
                        if (enabled) awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == song.id }
                            if (info != null) {
                                visualTop = info.offset.toFloat()
                                draggedHeight = info.size
                                pendingIndex = null
                                draggedId = song.id
                                try {
                                    drag(down.id) { change ->
                                        visualTop += change.positionChange().y
                                        change.consume()
                                        checkMove()
                                    }
                                } finally { draggedId = null; pendingIndex = null }
                            }
                        }
                    }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.DragHandle, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
