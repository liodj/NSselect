package com.example.nsselect.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.nsselect.viewmodel.MainViewModel

@Composable
fun MainScreen(viewModel: MainViewModel) {
    var targetMinutes by remember { mutableStateOf("15") }
    var targetSeconds by remember { mutableStateOf("0") }
    var marginSeconds by remember { mutableStateOf("30") }
    val minutes = targetMinutes.toLongOrNull()
    val seconds = targetSeconds.toLongOrNull()
    val margin = marginSeconds.toLongOrNull()
    val total = if (minutes != null && minutes in 0..(Int.MAX_VALUE / 60).toLong() && seconds != null) {
        minutes * 60 + seconds
    } else null
    val inputError = when {
        minutes == null || minutes < 0 -> "분은 0 이상의 숫자로 입력하세요."
        seconds == null || seconds !in 0..59 -> "초는 0~59 사이로 입력하세요."
        total == null || total !in 1..Int.MAX_VALUE.toLong() -> "목표 시간은 1초 이상인 유효한 범위로 입력하세요."
        margin == null || margin < 0 || margin > Int.MAX_VALUE.toLong() - total -> "오차범위는 0 이상의 숫자로 입력하세요."
        else -> null
    }
    
    val generatedList by viewModel.generatedPlaylist.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val generationMessage by viewModel.generationMessage.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("목표 시간 설정", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = targetMinutes,
                onValueChange = { targetMinutes = it },
                label = { Text("분") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = targetSeconds,
                onValueChange = { targetSeconds = it },
                label = { Text("초") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        
        OutlinedTextField(
            value = marginSeconds,
            onValueChange = { marginSeconds = it },
            label = { Text("오차 범위 (초)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        if (inputError != null) {
            Text(inputError, color = MaterialTheme.colorScheme.error)
        }
        
        Button(
            onClick = { 
                viewModel.generate(total!!.toInt(), margin!!.toInt())
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            enabled = !isGenerating && inputError == null
        ) {
            Text(if (isGenerating) "생성 중..." else "랜덤 목록 생성")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        if (generationMessage != null) {
            Text(generationMessage!!, color = MaterialTheme.colorScheme.error)
        }
        
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
