package com.example.nsselect.viewmodel

import android.app.Application
import android.net.Uri
import androidx.room.withTransaction
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nsselect.data.AppDatabase
import com.example.nsselect.data.ItemEntity
import com.example.nsselect.data.GeneratorSettings
import com.example.nsselect.generator.PlaylistEditor
import com.example.nsselect.data.SettingsPreset
import com.example.nsselect.data.SettingsPresetRepository
import com.example.nsselect.generator.PlaylistGenerator
import com.example.nsselect.util.CsvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

data class PresetUiState(
    val presets: List<SettingsPreset> = emptyList(),
    val selectedId: Long? = null,
    val busy: Boolean = false,
    val message: String? = null
)

sealed interface ImportResult {
    data class Success(val count: Int) : ImportResult
    data class Failure(val message: String) : ImportResult
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val dao = database.itemDao()
    private val presetRepository = SettingsPresetRepository(database)
    private val mutationMutex = Mutex()
    private val settingsRevision = AtomicLong()
    private val selectedPresetId = MutableStateFlow<Long?>(null)
    private val isPresetBusy = MutableStateFlow(false)
    private val presetMessage = MutableStateFlow<String?>(null)
    val presetUiState = combine(presetRepository.allPresets, selectedPresetId, isPresetBusy, presetMessage) { presets, id, busy, message ->
        PresetUiState(presets, id, busy, message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PresetUiState())
    
    val allItems = dao.getAllItemsFlow()
    val generatorSettings = database.generatorSettingsDao().observe().map { it ?: GeneratorSettings() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, GeneratorSettings())

    fun updateGeneratorSettings(settings: GeneratorSettings) {
        if (!settings.isValid()) return
        viewModelScope.launch {
            mutationMutex.withLock {
                database.generatorSettingsDao().save(settings)
                settingsRevision.incrementAndGet()
                _generationMessage.value = null
            }
        }
    }

    fun moveSong(songId: Int, destinationId: Int) {
        if (_isGenerating.value) return
        _generatedPlaylist.value = PlaylistEditor.move(_generatedPlaylist.value, songId, destinationId)
    }

    fun replaceSong(songId: Int, replacementId: Int) {
        viewModelScope.launch {
            mutationMutex.withLock {
                if (_isGenerating.value) return@withLock
                val settings = database.generatorSettingsDao().get() ?: GeneratorSettings()
                val candidates = PlaylistEditor.candidates(_generatedPlaylist.value, songId, dao.getAllItems(), settings)
                val replacement = candidates.firstOrNull { it.id == replacementId }
                if (replacement != null) {
                    _generatedPlaylist.value = _generatedPlaylist.value.map { if (it.id == songId) replacement else it }
                    _generationMessage.value = null
                } else _generationMessage.value = "설정이 변경되어 교체할 수 없습니다. 후보를 다시 확인하세요."
            }
        }
    }

    private val _generatedPlaylist = MutableStateFlow<List<ItemEntity>>(emptyList())
    val generatedPlaylist: StateFlow<List<ItemEntity>> = _generatedPlaylist.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _generationMessage = MutableStateFlow<String?>(null)
    val generationMessage = _generationMessage.asStateFlow()

    fun generate() {
        if (!_isGenerating.compareAndSet(false, true)) return
        viewModelScope.launch {
            _generationMessage.value = null
            try {
                val (validItems, settings, revision) = mutationMutex.withLock {
                    Triple(dao.getIncludedItems(), database.generatorSettingsDao().get() ?: GeneratorSettings(), settingsRevision.get())
                }
                val result = withContext(Dispatchers.Default) {
                    PlaylistGenerator.generatePlaylist(validItems, settings.targetSeconds, settings.marginSeconds)
                }
                if (revision != settingsRevision.get()) return@launch
                _generatedPlaylist.value = result
                _generationMessage.value = when {
                    validItems.isEmpty() -> "사용할 노래가 없습니다. CSV를 가져오거나 제외 설정을 확인하세요."
                    result.isEmpty() -> "오차범위 안의 목록을 찾지 못했습니다. 오차범위를 늘려 다시 시도하세요."
                    else -> null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _generatedPlaylist.value = emptyList()
                _generationMessage.value = "목록 생성 중 오류가 발생했습니다."
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun updateItem(item: ItemEntity) {
        updateSettings {
            database.withTransaction {
                val existing = dao.getAllItems()
                val previous = existing.firstOrNull { it.id == item.id } ?: return@withTransaction
                val targetCategory = existing.firstOrNull { it.category == item.category && it.id != item.id }
                val moved = previous.category != item.category
                dao.updateItem(previous.copy(
                    title = item.title,
                    durationSeconds = item.durationSeconds,
                    category = item.category,
                    categoryWeight = if (moved) targetCategory?.categoryWeight ?: 5 else previous.categoryWeight,
                    isCategoryExcluded = if (moved) targetCategory?.isCategoryExcluded ?: false else previous.isCategoryExcluded
                ))
            }
        }
    }

    fun updateCategoryExclusion(category: String, isExcluded: Boolean) {
        updateSettings {
            dao.updateCategoryExclusion(category, isExcluded)
        }
    }

    fun updateCategoryWeight(category: String, weight: Int) {
        updateSettings {
            dao.updateCategoryWeight(category, weight.coerceIn(1, 10))
        }
    }

    fun updateItemWeight(id: Int, weight: Int) {
        updateSettings {
            dao.updateItemWeight(id, weight.coerceIn(1, 10))
        }
    }

    fun updateItemExclusion(id: Int, isExcluded: Boolean) {
        updateSettings {
            dao.updateItemExclusion(id, isExcluded)
        }
    }

    private fun invalidatePlaylist() {
        settingsRevision.incrementAndGet()
        _generatedPlaylist.value = emptyList()
        _generationMessage.value = null
    }

    private fun updateSettings(action: suspend () -> Unit) {
        viewModelScope.launch {
            mutationMutex.withLock {
                withContext(Dispatchers.IO) { action() }
                invalidatePlaylist()
            }
        }
    }

    fun selectPreset(id: Long) {
        selectedPresetId.value = id
        presetMessage.value = null
    }

    fun savePreset(name: String) = runPresetOperation {
        selectedPresetId.value = presetRepository.save(name)
        "‘${name.trim()}’ 프리셋을 저장했습니다."
    }

    fun overwritePreset(id: Long) = runPresetOperation {
        val name = presetRepository.overwrite(id)
        "‘$name’ 프리셋을 현재 설정으로 덮어썼습니다."
    }

    fun applyPreset(id: Long) = runPresetOperation {
        val result = presetRepository.apply(id)
        invalidatePlaylist()
        "‘${result.name}’ 적용: 분류 ${result.categoriesApplied}개 · 곡 ${result.songsApplied}개" +
            if (result.songsSkipped > 0) " (일치하지 않는 곡 ${result.songsSkipped}개는 건너뛰었습니다.)" else ""
    }

    fun deletePreset(id: Long) = runPresetOperation {
        val name = presetRepository.delete(id)
        if (selectedPresetId.value == id) selectedPresetId.value = null
        "‘$name’ 프리셋을 삭제했습니다."
    }

    private fun runPresetOperation(action: suspend () -> String) {
        if (!isPresetBusy.compareAndSet(false, true)) return
        presetMessage.value = null
        viewModelScope.launch {
            try {
                presetMessage.value = mutationMutex.withLock { withContext(Dispatchers.IO) { action() } }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                presetMessage.value = e.message ?: "프리셋 처리 중 오류가 발생했습니다."
            } finally {
                isPresetBusy.value = false
            }
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
                mutationMutex.withLock {
                    database.withTransaction {
                        dao.deleteAllItems()
                        dao.insertItems(items)
                    }
                    invalidatePlaylist()
                }
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
        updateSettings {
            dao.deleteAllItems()
        }
    }
}
