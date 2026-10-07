package com.example.nsselect.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "settings_presets", indices = [Index(value = ["name"], unique = true)])
data class SettingsPreset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val updatedAt: Long,
    val songCount: Int,
    val categoryCount: Int,
    @ColumnInfo(defaultValue = "900") val targetSeconds: Int = 900,
    @ColumnInfo(defaultValue = "30") val marginSeconds: Int = 30,
    @ColumnInfo(defaultValue = "15") val replacementMarginSeconds: Int = 15
)

@Entity(
    tableName = "preset_categories",
    primaryKeys = ["presetId", "category"],
    foreignKeys = [ForeignKey(entity = SettingsPreset::class, parentColumns = ["id"], childColumns = ["presetId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("presetId")]
)
data class PresetCategory(
    val presetId: Long,
    val category: String,
    val weight: Int,
    val isExcluded: Boolean
)

@Entity(
    tableName = "preset_songs",
    primaryKeys = ["presetId", "songId"],
    foreignKeys = [ForeignKey(entity = SettingsPreset::class, parentColumns = ["id"], childColumns = ["presetId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("presetId")]
)
data class PresetSong(
    val presetId: Long,
    val songId: Int,
    val title: String,
    val weight: Int,
    val isExcluded: Boolean
)
