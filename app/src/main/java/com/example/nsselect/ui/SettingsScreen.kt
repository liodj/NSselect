package com.example.nsselect.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.nsselect.viewmodel.ImportResult
import com.example.nsselect.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val items by viewModel.allItems.collectAsState(initial = emptyList())
    var showClearConfirmation by remember { mutableStateOf(false) }
    var isWorking by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            isWorking = true
            viewModel.importCsv(it) { result ->
                isWorking = false
                val message = when (result) {
                    is ImportResult.Success -> "${result.count}개의 노래를 불러왔습니다."
                    is ImportResult.Failure -> "가져오기 실패: ${result.message} 기존 데이터는 유지됩니다."
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            isWorking = true
            viewModel.exportCsv(it) { result ->
                isWorking = false
                val message = when (result) {
                    is ImportResult.Success -> "${result.count}개의 노래를 내보냈습니다."
                    is ImportResult.Failure -> "내보내기 실패: ${result.message}"
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("데이터베이스 초기화") },
            text = { Text("저장된 노래 ${items.size}개를 모두 삭제하시겠습니까? 저장한 프리셋은 유지됩니다.") },
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

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("설정", style = MaterialTheme.typography.headlineSmall)
        Text("CSV 파일과 저장된 데이터를 관리합니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
            Text(
                "저장된 노래 ${items.size}개 · 분류 ${items.map { it.category }.distinct().size}개",
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }
        if (isWorking) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("CSV 불러오기", style = MaterialTheme.typography.titleMedium)
                Text("첫 행은 제외합니다. 열: 번호, 제목, 길이(초), 분류, 빈도(선택·기본값 5)")
                Text("정상적인 파일을 불러오면 현재 노래 목록이 교체됩니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { importLauncher.launch("*/*") }, enabled = !isWorking) { Text("불러오기") }
            }
        }
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("CSV 내보내기", style = MaterialTheme.typography.titleMedium)
                Text("노래 목록과 곡별 빈도를 CSV로 저장합니다.")
                OutlinedButton(
                    onClick = { exportLauncher.launch("nsselect.csv") },
                    enabled = !isWorking && items.isNotEmpty()
                ) { Text("내보내기") }
            }
        }
        HorizontalDivider()
        Text("저장된 데이터 삭제", style = MaterialTheme.typography.titleMedium)
        Text("초기화하면 노래 목록과 현재 사용 여부·빈도 설정이 삭제됩니다. 저장한 프리셋은 유지됩니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(
            onClick = { showClearConfirmation = true },
            enabled = !isWorking && items.isNotEmpty(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) { Text("DB 초기화") }
    }
}
