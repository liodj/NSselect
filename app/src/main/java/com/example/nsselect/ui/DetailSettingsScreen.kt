package com.example.nsselect.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nsselect.data.ItemEntity
import kotlin.math.roundToInt

private sealed interface WeightTarget {
    val label: String
    val value: Int
    data class Category(val category: String, override val value: Int) : WeightTarget {
        override val label = "$category 분류"
    }
    data class Song(val id: Int, val title: String, override val value: Int) : WeightTarget {
        override val label = "$id. $title"
    }
}

@Composable
internal fun DetailSettingsScreen(
    songs: List<ItemEntity>,
    onBack: () -> Unit,
    onCategoryUseChange: (String, Boolean) -> Unit,
    onCategoryWeightChange: (String, Int) -> Unit,
    onSongUseChange: (Int, Boolean) -> Unit,
    onSongWeightChange: (Int, Int) -> Unit,
    presetControls: @Composable () -> Unit = {}
) {
    var expandedCategories by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var weightTarget by remember { mutableStateOf<WeightTarget?>(null) }
    val grouped = songs.sortedBy { it.id }.groupBy { it.category }
    weightTarget?.let { target ->
        WeightDialog(target.label, target.value, onDismiss = { weightTarget = null }, onSave = { weight ->
            when (target) {
                is WeightTarget.Category -> onCategoryWeightChange(target.category, weight)
                is WeightTarget.Song -> onSongWeightChange(target.id, weight)
            }
            weightTarget = null
        })
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "데이터로 돌아가기") }
            Text("세부 설정", style = MaterialTheme.typography.headlineSmall)
        }
        Text("사용할 분류와 곡을 선택하세요. 빈도가 높을수록 더 자주 선택됩니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("빈도는 1~10단계이며 기본값은 5입니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (songs.isEmpty()) Text("설정할 노래가 없습니다. 설정 탭에서 CSV를 불러오세요.")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "presets") { presetControls() }
            grouped.forEach { (category, categorySongs) ->
                val expanded = category in expandedCategories
                val categoryEnabled = !categorySongs.first().isCategoryExcluded
                val categoryWeight = categorySongs.first().categoryWeight
                item(key = "category:$category") {
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        CategoryDropdownHeader(category, categorySongs.size, expanded) {
                            expandedCategories = if (expanded) expandedCategories - category else expandedCategories + category
                        }
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            UseAndWeightControls(
                                label = "$category 분류",
                                included = categoryEnabled,
                                weight = categoryWeight,
                                onUseChange = { onCategoryUseChange(category, it) },
                                onWeightClick = { weightTarget = WeightTarget.Category(category, categoryWeight) }
                            )
                            if (!categoryEnabled) {
                                Text("분류 전체가 생성에서 제외됩니다. 곡별 설정은 유지됩니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (expanded) {
                    items(categorySongs, key = { "song:${it.id}" }) { song ->
                        OutlinedCard(modifier = Modifier.fillMaxWidth().padding(start = 12.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("${song.id}. ${song.title}", style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(formatSongDuration(song.durationSeconds), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                UseAndWeightControls(
                                    label = "${song.id}. ${song.title}",
                                    included = !song.isExcluded,
                                    weight = song.weight,
                                    onUseChange = { onSongUseChange(song.id, it) },
                                    onWeightClick = { weightTarget = WeightTarget.Song(song.id, song.title, song.weight) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UseAndWeightControls(label: String, included: Boolean, weight: Int, onUseChange: (Boolean) -> Unit, onWeightClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FrequencyButton(label, weight, onWeightClick)
        Spacer(Modifier.weight(1f))
        Text(if (included) "사용" else "미사용", style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = included,
            onCheckedChange = onUseChange,
            modifier = Modifier.semantics { contentDescription = "$label 사용 여부" }
        )
    }
}

@Composable
private fun FrequencyButton(label: String, value: Int, onClick: () -> Unit) {
    val level = value.coerceIn(1, 10)
    val fill by animateFloatAsState(targetValue = level / 10f, label = "frequencyFill")
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.semantics {
            contentDescription = "$label 빈도 조정"
            stateDescription = "$level / 10"
            role = Role.Button
        }
    ) {
        Box(Modifier.width(132.dp).height(48.dp)) {
            Box(
                Modifier.fillMaxHeight().fillMaxWidth(fill)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
            Text(
                "빈도 $level",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun WeightDialog(label: String, initialValue: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var value by rememberSaveable(label, initialValue) { mutableStateOf(initialValue.coerceIn(1, 10).toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("빈도 조정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(label)
                Text(value.roundToInt().toString(), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
                Slider(
                    value = value,
                    onValueChange = { value = it.roundToInt().toFloat() },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.semantics { contentDescription = "$label 빈도 슬라이더" }
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("1 · 낮음")
                    Text("10 · 높음")
                }
                TextButton(onClick = { value = 5f }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("기본값 5") }
            }
        },
        confirmButton = { Button(onClick = { onSave(value.roundToInt()) }) { Text("저장") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}
