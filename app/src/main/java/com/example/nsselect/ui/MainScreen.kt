package com.example.nsselect.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.nsselect.data.GeneratorSettings
import com.example.nsselect.data.ItemEntity
import com.example.nsselect.generator.PlaylistEditor
import com.example.nsselect.viewmodel.MainViewModel

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val settings by viewModel.generatorSettings.collectAsState()
    val playlist by viewModel.generatedPlaylist.collectAsState()
    val catalogue by viewModel.allItems.collectAsState(initial = emptyList())
    val generating by viewModel.isGenerating.collectAsState()
    val message by viewModel.generationMessage.collectAsState()
    val presets by viewModel.presetUiState.collectAsState()
    GeneratorScreen(settings, playlist, catalogue, generating, message,
        viewModel::updateGeneratorSettings, viewModel::generate, viewModel::moveSong, viewModel::replaceSong,
        presetControls = {
            PresetPanel(presets, catalogue.isNotEmpty(), viewModel::selectPreset, viewModel::savePreset,
                viewModel::applyPreset, viewModel::overwritePreset, viewModel::deletePreset)
        })
}

@Composable
internal fun GeneratorScreen(
    settings: GeneratorSettings, playlist: List<ItemEntity>, catalogue: List<ItemEntity>,
    generating: Boolean, message: String?, onSettingsChange: (GeneratorSettings) -> Unit,
    onGenerate: () -> Unit, onMove: (Int, Int) -> Unit, onReplace: (Int, Int) -> Unit,
    presetControls: @Composable () -> Unit = {}
) {
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var replacingId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showPresets by rememberSaveable { mutableStateOf(false) }
    val total = playlist.sumOf { it.durationSeconds.toLong() }
    editing?.let { field ->
        TimeEditDialog(field, settings, onDismiss = { editing = null }, onSave = { onSettingsChange(it); editing = null })
    }
    val selectedSong = playlist.firstOrNull { it.id == replacingId }
    if (selectedSong != null && !generating) {
        ReplacementDialog(selectedSong, total, PlaylistEditor.candidates(playlist, selectedSong.id, catalogue, settings),
            onDismiss = { replacingId = null }, onSelect = { onReplace(selectedSong.id, it); replacingId = null })
    }
    if (showPresets) {
        AlertDialog(onDismissRequest = { showPresets = false }, title = { Text("생성 설정 프리셋") },
            text = { presetControls() }, confirmButton = { TextButton(onClick = { showPresets = false }) { Text("닫기") } })
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("목표 시간 설정", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { showPresets = true }, enabled = !generating) {
                Icon(Icons.Default.Bookmarks, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("프리셋")
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("목표 시간", style = MaterialTheme.typography.labelLarge)
                    Text(durationText(settings.targetSeconds.toLong()), style = MaterialTheme.typography.headlineMedium)
                }
                IconButton(onClick = { editing = "target" }, enabled = !generating) { Icon(Icons.Default.Edit, "목표 시간 수정") }
            }
        }
        OutlinedCard {
            Column(Modifier.padding(start = 12.dp, end = 4.dp)) {
                TimeSettingRow("전체 오차범위", settings.marginSeconds, !generating) { editing = "margin" }
                HorizontalDivider()
                TimeSettingRow("대체 목록 범위", settings.replacementMarginSeconds, !generating) { editing = "replacement" }
            }
        }
        Button(onClick = onGenerate, enabled = !generating, modifier = Modifier.fillMaxWidth()) {
            if (generating) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
            Text(if (generating) "생성 중…" else "랜덤 목록 생성")
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        if (playlist.isEmpty()) {
            Text("목록을 생성하면 곡을 교체하거나 순서를 바꿀 수 있습니다.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("${playlist.size}곡 · ${durationText(total)}", style = MaterialTheme.typography.titleMedium)
            ReorderablePlaylist(playlist, !generating, onMove, { replacingId = it }, Modifier.weight(1f))
        }
    }
}

internal fun durationText(seconds: Long) = "${seconds / 60}분 ${seconds % 60}초"

@Composable
private fun TimeSettingRow(label: String, value: Int, enabled: Boolean, onEdit: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text("±${value}초", style = MaterialTheme.typography.titleSmall)
        IconButton(onClick = onEdit, enabled = enabled) { Icon(Icons.Default.Edit, "$label 수정", Modifier.size(20.dp)) }
    }
}

@Composable
private fun TimeEditDialog(field: String, settings: GeneratorSettings, onDismiss: () -> Unit, onSave: (GeneratorSettings) -> Unit) {
    val target = field == "target"
    val title = when (field) { "target" -> "목표 시간"; "margin" -> "전체 오차범위"; else -> "대체 목록 범위" }
    val initial = when (field) { "target" -> settings.targetSeconds; "margin" -> settings.marginSeconds; else -> settings.replacementMarginSeconds }
    var minutes by rememberSaveable(field) { mutableStateOf((initial / 60).toString()) }
    var seconds by rememberSaveable(field) { mutableStateOf((if (target) initial % 60 else initial).toString()) }
    val min = minutes.toLongOrNull()
    val sec = seconds.toLongOrNull()
    val value = if (target && min != null && min in 0..(Int.MAX_VALUE / 60).toLong() && sec != null && sec in 0..59) min * 60 + sec else if (!target) sec else null
    val changed = if (value != null && value in 0..Int.MAX_VALUE.toLong()) when (field) {
        "target" -> settings.copy(targetSeconds = value.toInt())
        "margin" -> settings.copy(marginSeconds = value.toInt())
        else -> settings.copy(replacementMarginSeconds = value.toInt())
    } else null
    val valid = changed?.isValid() == true
    AlertDialog(onDismissRequest = onDismiss, title = { Text("$title 수정") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!target) Text(if (field == "margin") "목표 시간에서 허용할 차이입니다." else "원래 곡과 교체할 곡의 길이 차이입니다. 교체 후 총 시간도 전체 오차범위를 충족해야 합니다.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (target) OutlinedTextField(minutes, { minutes = it }, label = { Text("분") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(seconds, { seconds = it }, label = { Text("초") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }
            if (!valid) Text(if (target) "1초 이상으로 입력하세요. 초는 0~59입니다." else "0 이상의 유효한 초 값을 입력하세요.", color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = { Button(onClick = { onSave(changed!!) }, enabled = valid) { Text("저장") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } })
}

@Composable
private fun ReplacementDialog(song: ItemEntity, total: Long, candidates: List<ItemEntity>, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("곡 교체 · ${song.title}") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (candidates.isEmpty()) Text("사용 가능한 대체 곡이 없습니다. 전체 오차범위나 대체 목록 범위를 수정해 보세요.")
            else LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(candidates, key = { it.id }) { candidate ->
                    OutlinedCard(onClick = { onSelect(candidate.id) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${candidate.id}. ${candidate.title}", style = MaterialTheme.typography.titleSmall)
                            val delta = candidate.durationSeconds.toLong() - song.durationSeconds
                            Text("${candidate.category} · ${formatSongDuration(candidate.durationSeconds)} (${if (delta > 0) "+" else ""}${delta}초)", style = MaterialTheme.typography.bodySmall)
                            Text("교체 후 총 ${durationText(total + delta)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } })
}
