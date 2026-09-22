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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nsselect.data.ItemEntity
import com.example.nsselect.viewmodel.MainViewModel
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val items by viewModel.allItems.collectAsState(initial = emptyList())
    
    var itemToEdit by remember { mutableStateOf<ItemEntity?>(null) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    val strUtf8 = String(bytes, Charsets.UTF_8)
                    // 만약 UTF-8로 읽었을 때 깨진 문자(Replacement Character)가 포함되어 있다면, 
                    // 엑셀에서 기본 저장되는 ANSI(EUC-KR/CP949)로 간주하고 다시 디코딩합니다.
                    val csvContent = if (strUtf8.contains("\uFFFD")) {
                        String(bytes, java.nio.charset.Charset.forName("EUC-KR"))
                    } else {
                        strUtf8
                    }
                    
                    // 새 파일을 불러올 때 기존 DB와 번호를 모두 초기화합니다.
                    viewModel.clearDatabase()
                    
                    viewModel.importCsv(csvContent) { count ->
                        android.widget.Toast.makeText(context, "${count}개의 데이터를 성공적으로 불러왔습니다.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "파일을 읽는 중 오류가 발생했습니다.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 아이템 수정 다이얼로그
    if (itemToEdit != null) {
        var editTitle by remember { mutableStateOf(itemToEdit!!.title) }
        var editCategory by remember { mutableStateOf(itemToEdit!!.category) }
        var editDuration by remember { mutableStateOf(itemToEdit!!.durationSeconds.toString()) }
        
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val durationInt = editDuration.toIntOrNull() ?: itemToEdit!!.durationSeconds
                    viewModel.updateItem(itemToEdit!!.copy(
                        title = editTitle,
                        category = editCategory,
                        durationSeconds = durationInt
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
            text = "CSV 파일 형식: 번호, 제목, 길이(초), 분류\n(스마트폰에 저장된 .csv 또는 .txt 파일을 선택해주세요)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { launcher.launch("*/*") }) {
                Text("CSV 파일 선택")
            }
            Button(onClick = { viewModel.clearDatabase() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("DB 초기화")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("항목 및 가중치 관리 (총 ${items.size}개)", style = MaterialTheme.typography.titleLarge)
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        
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
                        Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }
}
