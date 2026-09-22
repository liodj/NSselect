package com.example.nsselect.viewmodel

import android.app.Application
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

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).itemDao()
    
    val allItems = dao.getAllItemsFlow()

    private val _generatedPlaylist = MutableStateFlow<List<ItemEntity>>(emptyList())
    val generatedPlaylist: StateFlow<List<ItemEntity>> = _generatedPlaylist.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    fun generate(targetSeconds: Int, marginSeconds: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _isGenerating.value = true
            val validItems = dao.getIncludedItems()
            val result = PlaylistGenerator.generatePlaylist(validItems, targetSeconds, marginSeconds)
            _generatedPlaylist.value = result
            _isGenerating.value = false
        }
    }

    fun updateItem(item: ItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateItem(item)
        }
    }

    fun updateCategoryExclusion(category: String, isExcluded: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateCategoryExclusion(category, isExcluded)
        }
    }

    fun updateCategoryWeight(category: String, weight: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateCategoryWeight(category, weight)
        }
    }

    fun importCsv(csvContent: String, onResult: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val items = CsvParser.parseCsv(csvContent)
            if (items.isNotEmpty()) {
                dao.insertItems(items)
            }
            launch(Dispatchers.Main) {
                onResult(items.size)
            }
        }
    }

    fun clearDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAllItems()
        }
    }
}
