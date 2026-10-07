package com.example.nsselect.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nsselect.data.ItemEntity
import com.example.nsselect.viewmodel.MainViewModel

@Composable
fun DataScreen(viewModel: MainViewModel) {
    val songs by viewModel.allItems.collectAsState(initial = emptyList())
    val presetState by viewModel.presetUiState.collectAsState()
    var showDetails by rememberSaveable { mutableStateOf(false) }
    val screenState = rememberSaveableStateHolder()
    BackHandler(enabled = showDetails) { showDetails = false }

    screenState.SaveableStateProvider(if (showDetails) "details" else "table") {
        if (showDetails) {
            DetailSettingsScreen(
                songs = songs,
                onBack = { showDetails = false },
                onCategoryUseChange = { category, included -> viewModel.updateCategoryExclusion(category, !included) },
                onCategoryWeightChange = viewModel::updateCategoryWeight,
                onSongUseChange = { id, included -> viewModel.updateItemExclusion(id, !included) },
                onSongWeightChange = viewModel::updateItemWeight,
                presetControls = {
                    PresetPanel(
                        state = presetState,
                        hasSongs = songs.isNotEmpty(),
                        onSelect = viewModel::selectPreset,
                        onSave = viewModel::savePreset,
                        onApply = viewModel::applyPreset,
                        onOverwrite = viewModel::overwritePreset,
                        onDelete = viewModel::deletePreset
                    )
                }
            )
        } else {
            DataTableScreen(songs, onOpenDetails = { showDetails = true }, onUpdateSong = viewModel::updateItem)
        }
    }
}

@Composable
internal fun DataTableScreen(songs: List<ItemEntity>, onOpenDetails: () -> Unit, onUpdateSong: (ItemEntity) -> Unit) {
    var expandedCategories by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var editId by rememberSaveable { mutableStateOf<Int?>(null) }
    val grouped = songs.sortedBy { it.id }.groupBy { it.category }
    val editedSong = songs.firstOrNull { it.id == editId }
    if (editedSong != null) {
        SongEditDialog(editedSong, onDismiss = { editId = null }, onSave = {
            onUpdateSong(it)
            editId = null
        })
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("데이터", style = MaterialTheme.typography.headlineSmall)
                Text("${songs.size}곡 · ${grouped.size}개 분류", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onOpenDetails) {
                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("세부 설정")
            }
        }
        if (songs.isEmpty()) {
            Text("등록된 노래가 없습니다. 설정 탭에서 CSV를 불러오세요.")
        } else {
            Text("분류를 펼쳐 목록을 확인하고, 곡을 눌러 정보를 수정할 수 있습니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            grouped.forEach { (category, categorySongs) ->
                val expanded = category in expandedCategories
                item(key = "category:$category") {
                    CategoryDropdownHeader(category, categorySongs.size, expanded) {
                        expandedCategories = if (expanded) expandedCategories - category else expandedCategories + category
                    }
                }
                if (expanded) {
                    item(key = "columns:$category") {
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                            SongTableCells("번호", "제목", "길이", "분류")
                        }
                    }
                    items(categorySongs, key = { "song:${it.id}" }) { song ->
                        Surface(onClick = { editId = song.id }, modifier = Modifier.fillMaxWidth()) {
                            Column {
                                SongTableCells(song.id.toString(), song.title, formatSongDuration(song.durationSeconds), song.category)
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CategoryDropdownHeader(category: String, count: Int, expanded: Boolean, onToggle: () -> Unit) {
    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(category, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${count}곡", style = MaterialTheme.typography.bodyMedium)
            Icon(
                if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = "$category ${if (expanded) "접기" else "펼치기"}",
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun SongTableCells(number: String, title: String, duration: String, category: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(number, modifier = Modifier.width(36.dp), style = MaterialTheme.typography.bodySmall)
        Text(title, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(duration, modifier = Modifier.width(48.dp), style = MaterialTheme.typography.bodySmall)
        Text(category, modifier = Modifier.width(52.dp), maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
    }
}

internal fun formatSongDuration(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)

@Composable
private fun SongEditDialog(song: ItemEntity, onDismiss: () -> Unit, onSave: (ItemEntity) -> Unit) {
    var title by rememberSaveable(song.id) { mutableStateOf(song.title) }
    var category by rememberSaveable(song.id) { mutableStateOf(song.category) }
    var duration by rememberSaveable(song.id) { mutableStateOf(song.durationSeconds.toString()) }
    val durationSeconds = duration.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("곡 정보 수정 · ${song.id}번") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, onValueChange = { title = it }, label = { Text("제목") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(category, onValueChange = { category = it }, label = { Text("분류") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    duration,
                    onValueChange = { duration = it },
                    label = { Text("길이 (초)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = durationSeconds == null || durationSeconds <= 0,
                    supportingText = { Text("1초 이상의 숫자를 입력하세요.") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(song.copy(title = title.trim(), category = category.trim(), durationSeconds = durationSeconds!!)) },
                enabled = title.isNotBlank() && category.isNotBlank() && durationSeconds != null && durationSeconds > 0
            ) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}
