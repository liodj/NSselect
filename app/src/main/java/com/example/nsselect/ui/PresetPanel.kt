package com.example.nsselect.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nsselect.viewmodel.PresetUiState

@Composable
internal fun PresetPanel(
    state: PresetUiState,
    hasSongs: Boolean,
    onSelect: (Long) -> Unit,
    onSave: (String) -> Unit,
    onApply: (Long) -> Unit,
    onOverwrite: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var confirmation by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = state.presets.firstOrNull { it.id == state.selectedId }

    if (showSaveDialog) {
        var name by rememberSaveable { mutableStateOf("") }
        val trimmed = name.trim()
        val duplicate = state.presets.any { it.name == trimmed }
        val valid = trimmed.isNotEmpty() && trimmed.length <= 40 && !duplicate
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("새 프리셋 저장") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("현재 사용 여부·빈도와 목표 시간·전체 오차·대체 목록 범위를 함께 저장합니다.")
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("프리셋 이름") },
                        singleLine = true,
                        isError = duplicate || trimmed.length > 40,
                        supportingText = {
                            Text(when {
                                duplicate -> "같은 이름이 있습니다. 선택 후 덮어쓰기를 사용하세요."
                                trimmed.length > 40 -> "이름은 40자 이내로 입력하세요."
                                else -> "예: 평소 듣기, 운동, 연주곡 위주"
                            })
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = { onSave(trimmed); showSaveDialog = false }, enabled = valid && !state.busy && hasSongs) { Text("저장") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("취소") } }
        )
    }

    if (confirmation != null && selected != null) {
        val deleting = confirmation == "delete"
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text(if (deleting) "프리셋 삭제" else "프리셋 덮어쓰기") },
            text = { Text(if (deleting) "‘${selected.name}’ 프리셋을 삭제하시겠습니까? 현재 설정은 유지됩니다." else "‘${selected.name}’ 프리셋을 현재 사용 여부·빈도와 시간 설정으로 덮어쓰시겠습니까?") },
            confirmButton = {
                Button(onClick = {
                    if (deleting) onDelete(selected.id) else onOverwrite(selected.id)
                    confirmation = null
                }, enabled = !state.busy && (deleting || hasSongs)) { Text(if (deleting) "삭제" else "덮어쓰기") }
            },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("취소") } }
        )
    }

    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("프리셋", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { menuExpanded = true },
                        enabled = !state.busy && state.presets.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selected?.name ?: "프리셋 선택", maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "프리셋 목록")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, modifier = Modifier.heightIn(max = 280.dp)) {
                        state.presets.forEach { preset ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(preset.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${preset.categoryCount}개 분류 · ${preset.songCount}곡", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = { onSelect(preset.id); menuExpanded = false }
                            )
                        }
                    }
                }
                Button(onClick = { showSaveDialog = true }, enabled = !state.busy && hasSongs) { Text("새로 저장") }
            }
            if (selected != null) {
                Text("저장된 시간: ${durationText(selected.targetSeconds.toLong())} · 전체 ±${selected.marginSeconds}초 · 대체 목록 범위 ±${selected.replacementMarginSeconds}초", style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(onClick = { onApply(selected.id) }, enabled = !state.busy && hasSongs, modifier = Modifier.weight(1f)) { Text("적용") }
                    TextButton(onClick = { confirmation = "overwrite" }, enabled = !state.busy && hasSongs, modifier = Modifier.weight(1f)) { Text("덮어쓰기") }
                    TextButton(onClick = { confirmation = "delete" }, enabled = !state.busy, modifier = Modifier.weight(1f), colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("삭제") }
                }
            } else {
                Text(
                    if (state.presets.isEmpty()) "현재 설정을 이름 있는 프리셋으로 저장하세요." else "프리셋을 선택한 뒤 적용하세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
