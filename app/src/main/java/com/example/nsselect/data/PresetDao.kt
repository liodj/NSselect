package com.example.nsselect.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM settings_presets ORDER BY updatedAt DESC, id DESC")
    fun getPresetsFlow(): Flow<List<SettingsPreset>>

    @Query("SELECT * FROM settings_presets ORDER BY id")
    suspend fun getPresets(): List<SettingsPreset>

    @Query("SELECT * FROM settings_presets WHERE id = :id")
    suspend fun getPreset(id: Long): SettingsPreset?

    @Query("SELECT * FROM settings_presets WHERE name = :name")
    suspend fun getPresetByName(name: String): SettingsPreset?

    @Insert
    suspend fun insertPreset(preset: SettingsPreset): Long

    @Update
    suspend fun updatePreset(preset: SettingsPreset)

    @Insert
    suspend fun insertCategories(categories: List<PresetCategory>)

    @Insert
    suspend fun insertSongs(songs: List<PresetSong>)

    @Query("SELECT * FROM preset_categories WHERE presetId = :id")
    suspend fun getCategories(id: Long): List<PresetCategory>

    @Query("SELECT * FROM preset_songs WHERE presetId = :id")
    suspend fun getSongs(id: Long): List<PresetSong>

    @Query("DELETE FROM preset_categories WHERE presetId = :id")
    suspend fun deleteCategories(id: Long)

    @Query("DELETE FROM preset_songs WHERE presetId = :id")
    suspend fun deleteSongs(id: Long)

    @Query("DELETE FROM settings_presets WHERE id = :id")
    suspend fun deletePreset(id: Long)
}
