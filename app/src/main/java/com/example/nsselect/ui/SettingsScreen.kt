package com.example.nsselect.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nsselect.data.ItemEntity
import com.example.nsselect.viewmodel.MainViewModel
import com.example.nsselect.viewmodel.ImportResult

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val items by viewModel.allItems.collectAsState(initial = emptyList())
    
    var itemToEdit by remember { mutableStateOf<ItemEntity?>(null) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selectedUri ->
            isImporting = true
            viewModel.importCsv(selectedUri) { result ->
                isImporting = false
                val message = when (result) {
                    is ImportResult.Success -> "${result.count}개의 노래를 불러왔습니다."
                    is ImportResult.Failure -> "가져오기 실패: ${result.message} 기존 데이터는 유지됩니다."
                }
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { selectedUri ->
            isExporting = true
            viewModel.exportCsv(selectedUri) { result ->
                isExporting = false
                val message = when (result) {
                    is ImportResult.Success -> "${result.count}개의 노래를 내보냈습니다."
                    is ImportResult.Failure -> "내보내기 실패: ${result.message}"
                }
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("데이터베이스 초기화") },
            text = { Text("저장된 노래 ${items.size}개를 모두 삭제하시겠습니까?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearDatabase()
                    showClearConfirmation = false
                }) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) { Text("취소") }
            }
        )
    }

    // 아이템 수정 다이얼로그
    if (itemToEdit != null) {
        var editTitle by remember(itemToEdit!!.id) { mutableStateOf(itemToEdit!!.title) }
        var editCategory by remember(itemToEdit!!.id) { mutableStateOf(itemToEdit!!.category) }
        var editDuration by remember(itemToEdit!!.id) { mutableStateOf(itemToEdit!!.durationSeconds.toString()) }
        val durationInt = editDuration.toIntOrNull()
        val canSave = editTitle.isNotBlank() && editCategory.isNotBlank() && durationInt != null && durationInt > 0
        
        AlertDialog(
            onDismissRequest = { itemToEdit = null },
            title = { Text("항목 수정 (No. ${itemToEdit!!.id})") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("제목") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        label = { Text("분류") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = editDuration,
                        onValueChange = { editDuration = it },
                        label = { Text("시간 (초)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { if (durationInt == null || durationInt <= 0) Text("1초 이상의 숫자를 입력하세요.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(enabled = canSave, onClick = {
                    viewModel.updateItem(itemToEdit!!.copy(
                        title = editTitle.trim(),
                        category = editCategory.trim(),
                        durationSeconds = durationInt!!
                    ))
                    itemToEdit = null
                }) { Text("저장") }
            },
            dismissButton = {
                TextButton(onClick = { itemToEdit = null }) { Text("취소") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("데이터 관리", style = MaterialTheme.typography.titleLarge)
        
        Text(
            text = "CSV 첫 행은 제외합니다. 열: 번호, 제목, 길이(초), 분류, 가중치(선택·기본값 5)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { launcher.launch("*/*") }, enabled = !isImporting && !isExporting, modifier = Modifier.weight(1f)) {
                Text("불러오기")
            }
            Button(onClick = { exportLauncher.launch("nsselect.csv") }, enabled = !isImporting && !isExporting && items.isNotEmpty(), modifier = Modifier.weight(1f)) {
                Text("내보내기")
            }
            Button(onClick = { showClearConfirmation = true }, enabled = !isImporting && !isExporting && items.isNotEmpty(), modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("DB 초기화")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("항목 및 가중치 관리 (총 ${items.size}개)", style = MaterialTheme.typography.titleLarge)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        val groupedItems = items.groupBy { it.category }
        val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }
        
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            groupedItems.forEach { (category, categoryItems) ->
                val isExpanded = expandedCategories[category] ?: false
                val startId = categoryItems.minOfOrNull { it.id } ?: 0
                val endId = categoryItems.maxOfOrNull { it.id } ?: 0
                
                item(key = "header_$category") {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = { expandedCategories[category] = !isExpanded },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        val categoryWeight = categoryItems.firstOrNull()?.categoryWeight ?: 5
                        val isCategoryIncluded = categoryItems.any { !it.isExcluded }
                        
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "[$category] $startId ~ $endId",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            
                            // 분류별 가중치 조절
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (categoryWeight > 1) viewModel.updateCategoryWeight(category, categoryWeight - 1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDropDown, "내리기")
                                }
                                Text("${categoryWeight}", fontSize = 14.sp, modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
                                IconButton(
                                    onClick = { if (categoryWeight < 10) viewModel.updateCategoryWeight(category, categoryWeight + 1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDropUp, "올리기")
                                }
                            }
                            
                            // 분류 전체 제외/포함 스위치
                            Switch(
                                checked = isCategoryIncluded,
                                onCheckedChange = { isChecked -> viewModel.updateCategoryExclusion(category, !isChecked) },
                                modifier = Modifier.scale(0.8f)
                            )
                            
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "펼치기/접기"
                            )
                        }
                    }
                }
                
                if (isExpanded) {
                    items(categoryItems, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 왼쪽 정보 (번호, 제목, 시간)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${item.id}. ${item.title}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1
                                )
                                val min = item.durationSeconds / 60
                                val sec = item.durationSeconds % 60
                                Text(
                                    text = String.format("%02d:%02d", min, sec),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // 가중치 조절
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (item.weight > 1) viewModel.updateItem(item.copy(weight = item.weight - 1)) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDropDown, "내리기")
                                }
                                
                                Text(
                                    text = "${item.weight}",
                                    fontSize = 14.sp,
                                    modifier = Modifier.width(20.dp),
                                    textAlign = TextAlign.Center
                                )
                                
                                IconButton(
                                    onClick = { if (item.weight < 10) viewModel.updateItem(item.copy(weight = item.weight + 1)) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDropUp, "올리기")
                                }
                            }
                            
                            // 편집 버튼
                            IconButton(
                                onClick = { itemToEdit = item },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Edit, "편집", modifier = Modifier.size(20.dp))
                            }
                            
                            // 제외 스위치
                            Switch(
                                checked = !item.isExcluded,
                                onCheckedChange = { isChecked -> viewModel.updateItem(item.copy(isExcluded = !isChecked)) },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }
}
