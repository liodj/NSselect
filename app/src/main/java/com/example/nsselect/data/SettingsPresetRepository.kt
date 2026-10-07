package com.example.nsselect.data

import androidx.room.withTransaction

data class PresetApplyResult(val name: String, val categoriesApplied: Int, val songsApplied: Int, val songsSkipped: Int)

class SettingsPresetRepository(private val database: AppDatabase) {
    private val items = database.itemDao()
    private val presets = database.presetDao()
    val allPresets = presets.getPresetsFlow()

    suspend fun save(name: String): Long = database.withTransaction {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty() && trimmed.length <= 40) { "프리셋 이름은 1~40자로 입력하세요." }
        require(presets.getPresetByName(trimmed) == null) { "같은 이름의 프리셋이 있습니다. 덮어쓰기를 사용하세요." }
        val songs = items.getAllItems()
        require(songs.isNotEmpty()) { "저장할 노래 설정이 없습니다." }
        val time = database.generatorSettingsDao().get() ?: GeneratorSettings()
        val id = presets.insertPreset(SettingsPreset(
            name = trimmed, updatedAt = System.currentTimeMillis(), songCount = songs.size,
            categoryCount = songs.map { it.category }.distinct().size,
            targetSeconds = time.targetSeconds, marginSeconds = time.marginSeconds,
            replacementMarginSeconds = time.replacementMarginSeconds
        ))
        saveEntries(id, songs)
        id
    }

    suspend fun overwrite(id: Long): String = database.withTransaction {
        val preset = requireNotNull(presets.getPreset(id)) { "프리셋을 찾을 수 없습니다." }
        val songs = items.getAllItems()
        require(songs.isNotEmpty()) { "저장할 노래 설정이 없습니다." }
        val time = database.generatorSettingsDao().get() ?: GeneratorSettings()
        presets.updatePreset(preset.copy(
            updatedAt = System.currentTimeMillis(), songCount = songs.size,
            categoryCount = songs.map { it.category }.distinct().size,
            targetSeconds = time.targetSeconds, marginSeconds = time.marginSeconds,
            replacementMarginSeconds = time.replacementMarginSeconds
        ))
        presets.deleteCategories(id)
        presets.deleteSongs(id)
        saveEntries(id, songs)
        preset.name
    }

    private suspend fun saveEntries(id: Long, songs: List<ItemEntity>) {
        presets.insertCategories(songs.groupBy { it.category }.map { (category, members) ->
            PresetCategory(id, category, members.first().categoryWeight, members.first().isCategoryExcluded)
        })
        presets.insertSongs(songs.map { PresetSong(id, it.id, it.title, it.weight, it.isExcluded) })
    }

    suspend fun apply(id: Long): PresetApplyResult = database.withTransaction {
        val preset = requireNotNull(presets.getPreset(id)) { "프리셋을 찾을 수 없습니다." }
        val current = items.getAllItems()
        val currentCategories = current.map { it.category }.toSet()
        val currentSongs = current.associateBy { it.id }
        val savedCategories = presets.getCategories(id).filter { it.category in currentCategories }
        val savedSongs = presets.getSongs(id)
        val matchingSongs = savedSongs.filter { currentSongs[it.songId]?.title == it.title }
        database.generatorSettingsDao().save(GeneratorSettings(
            targetSeconds = preset.targetSeconds, marginSeconds = preset.marginSeconds,
            replacementMarginSeconds = preset.replacementMarginSeconds
        ))
        savedCategories.forEach { items.updateCategorySettings(it.category, it.weight, it.isExcluded) }
        matchingSongs.forEach { items.updateSongSettings(it.songId, it.weight, it.isExcluded) }
        PresetApplyResult(preset.name, savedCategories.size, matchingSongs.size, savedSongs.size - matchingSongs.size)
    }

    suspend fun delete(id: Long): String = database.withTransaction {
        val preset = requireNotNull(presets.getPreset(id)) { "프리셋을 찾을 수 없습니다." }
        presets.deletePreset(id)
        preset.name
    }
}
