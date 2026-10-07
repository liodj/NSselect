package com.example.nsselect.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "generator_settings")
data class GeneratorSettings(
    @PrimaryKey val id: Int = 1,
    val targetSeconds: Int = 900,
    val marginSeconds: Int = 30,
    val replacementMarginSeconds: Int = 15
) {
    fun isValid() = id == 1 && targetSeconds > 0 && marginSeconds >= 0 &&
        marginSeconds.toLong() + targetSeconds <= Int.MAX_VALUE && replacementMarginSeconds >= 0
}

@Dao
interface GeneratorSettingsDao {
    @Query("SELECT * FROM generator_settings WHERE id = 1")
    fun observe(): Flow<GeneratorSettings?>
    @Query("SELECT * FROM generator_settings WHERE id = 1")
    suspend fun get(): GeneratorSettings?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(settings: GeneratorSettings)
}
