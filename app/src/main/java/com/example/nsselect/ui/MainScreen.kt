package com.example.nsselect.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nsselect.viewmodel.MainViewModel

@Composable
fun MainScreen(viewModel: MainViewModel) {
    var targetMinutes by remember { mutableStateOf("15") }
    var targetSeconds by remember { mutableStateOf("0") }
    var marginSeconds by remember { mutableStateOf("30") }
    
    val generatedList by viewModel.generatedPlaylist.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("목표 시간 설정", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = targetMinutes,
                onValueChange = { targetMinutes = it },
                label = { Text("분") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = targetSeconds,
                onValueChange = { targetSeconds = it },
                label = { Text("초") },
                modifier = Modifier.weight(1f)
            )
        }
        
        OutlinedTextField(
            value = marginSeconds,
            onValueChange = { marginSeconds = it },
            label = { Text("오차 범위 (초)") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        
        Button(
            onClick = { 
                val target = (targetMinutes.toIntOrNull() ?: 0) * 60 + (targetSeconds.toIntOrNull() ?: 0)
                val margin = marginSeconds.toIntOrNull() ?: 0
                viewModel.generate(target, margin)
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            enabled = !isGenerating
        ) {
            Text(if (isGenerating) "생성 중..." else "랜덤 목록 생성")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val totalSecs = generatedList.sumOf { it.durationSeconds }
        Text("결과: 총 ${generatedList.size}항목 (총 ${totalSecs / 60}분 ${totalSecs % 60}초)")
        
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
            items(generatedList) { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("${item.id}. ${item.title}", style = MaterialTheme.typography.titleMedium)
                        val min = item.durationSeconds / 60
                        val sec = item.durationSeconds % 60
                        Text("길이: ${String.format("%02d:%02d", min, sec)} | 분류: ${item.category} | 가중치: ${item.weight}")
                    }
                }
            }
        }
    }
}
