package com.example.nsselect.viewmodel

import android.app.Application
import android.net.Uri
import androidx.room.withTransaction
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nsselect.data.AppDatabase
import com.example.nsselect.data.ItemEntity
import com.example.nsselect.generator.PlaylistGenerator
import com.example.nsselect.util.CsvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ImportResult {
    data class Success(val count: Int) : ImportResult
    data class Failure(val message: String) : ImportResult
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val dao = database.itemDao()
    
    val allItems = dao.getAllItemsFlow()

    private val _generatedPlaylist = MutableStateFlow<List<ItemEntity>>(emptyList())
    val generatedPlaylist: StateFlow<List<ItemEntity>> = _generatedPlaylist.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _generationMessage = MutableStateFlow<String?>(null)
    val generationMessage = _generationMessage.asStateFlow()

    fun generate(targetSeconds: Int, marginSeconds: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _isGenerating.value = true
            _generationMessage.value = null
            try {
                val validItems = dao.getIncludedItems()
                val result = PlaylistGenerator.generatePlaylist(validItems, targetSeconds, marginSeconds)
                _generatedPlaylist.value = result
                _generationMessage.value = when {
                    validItems.isEmpty() -> "사용할 노래가 없습니다. CSV를 가져오거나 제외 설정을 확인하세요."
                    result.isEmpty() -> "오차범위 안의 목록을 찾지 못했습니다. 오차범위를 늘려 다시 시도하세요."
                    else -> null
                }
            } catch (e: Exception) {
                _generatedPlaylist.value = emptyList()
                _generationMessage.value = "목록 생성 중 오류가 발생했습니다."
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun updateItem(item: ItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateItem(item)
            _generatedPlaylist.value = emptyList()
            _generationMessage.value = null
        }
    }

    fun updateCategoryExclusion(category: String, isExcluded: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateCategoryExclusion(category, isExcluded)
            _generatedPlaylist.value = emptyList()
            _generationMessage.value = null
        }
    }

    fun updateCategoryWeight(category: String, weight: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateCategoryWeight(category, weight)
            _generatedPlaylist.value = emptyList()
            _generationMessage.value = null
        }
    }

    fun importCsv(uri: Uri, onResult: (ImportResult) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                val bytes = getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalArgumentException("파일을 열 수 없습니다.")
                val utf8 = String(bytes, Charsets.UTF_8)
                val content = if ('\uFFFD' in utf8) {
                    String(bytes, java.nio.charset.Charset.forName("EUC-KR"))
                } else utf8
                val items = CsvParser.parseCsv(content)
                database.withTransaction {
                    dao.deleteAllItems()
                    dao.insertItems(items)
                }
                _generatedPlaylist.value = emptyList()
                _generationMessage.value = null
                ImportResult.Success(items.size)
            } catch (e: Exception) {
                ImportResult.Failure(e.message ?: "파일을 가져오지 못했습니다.")
            }
            withContext(Dispatchers.Main) { onResult(result) }
        }
    }

    fun exportCsv(uri: Uri, onResult: (ImportResult) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                val items = dao.getAllItems()
                if (items.isEmpty()) throw IllegalArgumentException("내보낼 노래가 없습니다.")
                val output = getApplication<Application>().contentResolver.openOutputStream(uri)
                    ?: throw IllegalArgumentException("파일을 쓸 수 없습니다.")
                output.use { stream ->
                    stream.write("\uFEFF".toByteArray(Charsets.UTF_8))
                    stream.write(CsvParser.toCsv(items).toByteArray(Charsets.UTF_8))
                }
                ImportResult.Success(items.size)
            } catch (e: Exception) {
                ImportResult.Failure(e.message ?: "파일을 내보내지 못했습니다.")
            }
            withContext(Dispatchers.Main) { onResult(result) }
        }
    }

    fun clearDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAllItems()
            _generatedPlaylist.value = emptyList()
            _generationMessage.value = null
        }
    }
}
