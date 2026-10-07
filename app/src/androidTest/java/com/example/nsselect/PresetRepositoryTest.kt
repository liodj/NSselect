package com.example.nsselect

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nsselect.data.AppDatabase
import com.example.nsselect.data.ItemEntity
import com.example.nsselect.data.SettingsPresetRepository
import com.example.nsselect.data.GeneratorSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PresetRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun timeSettingsAreSavedOverwrittenAppliedAndPersistAcrossReopen() = runBlocking {
        val name = "generator-time-test.db"
        context.deleteDatabase(name)
        var database = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            database.itemDao().insertItems(listOf(ItemEntity(1, "곡", 120, "가요")))
            val time = GeneratorSettings(targetSeconds = 1250, marginSeconds = 45, replacementMarginSeconds = 20)
            database.generatorSettingsDao().save(time)
            val id = SettingsPresetRepository(database).save("시간 포함")
            database.generatorSettingsDao().save(GeneratorSettings())
            SettingsPresetRepository(database).apply(id)
            assertEquals(time, database.generatorSettingsDao().get())
            val changed = time.copy(targetSeconds = 600, replacementMarginSeconds = 0)
            database.generatorSettingsDao().save(changed)
            SettingsPresetRepository(database).overwrite(id)
            database.close()
            database = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            assertEquals(changed, database.generatorSettingsDao().get())
            database.generatorSettingsDao().save(GeneratorSettings())
            SettingsPresetRepository(database).apply(id)
            assertEquals(changed, database.generatorSettingsDao().get())
        } finally { database.close(); context.deleteDatabase(name) }
    }

    @Test
    fun versionFivePresetsRetainSongSettingsAndGainDefaultTimes() = runBlocking {
        val name = "time-migration-test.db"
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            old.execSQL("CREATE TABLE items (id INTEGER NOT NULL PRIMARY KEY, title TEXT NOT NULL, durationSeconds INTEGER NOT NULL, category TEXT NOT NULL, weight INTEGER NOT NULL, categoryWeight INTEGER NOT NULL, isExcluded INTEGER NOT NULL, isCategoryExcluded INTEGER NOT NULL DEFAULT 0)")
            old.execSQL("CREATE TABLE settings_presets (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, updatedAt INTEGER NOT NULL, songCount INTEGER NOT NULL, categoryCount INTEGER NOT NULL)")
            old.execSQL("CREATE UNIQUE INDEX index_settings_presets_name ON settings_presets (name)")
            old.execSQL("CREATE TABLE preset_categories (presetId INTEGER NOT NULL, category TEXT NOT NULL, weight INTEGER NOT NULL, isExcluded INTEGER NOT NULL, PRIMARY KEY(presetId, category), FOREIGN KEY(presetId) REFERENCES settings_presets(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            old.execSQL("CREATE INDEX index_preset_categories_presetId ON preset_categories (presetId)")
            old.execSQL("CREATE TABLE preset_songs (presetId INTEGER NOT NULL, songId INTEGER NOT NULL, title TEXT NOT NULL, weight INTEGER NOT NULL, isExcluded INTEGER NOT NULL, PRIMARY KEY(presetId, songId), FOREIGN KEY(presetId) REFERENCES settings_presets(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            old.execSQL("CREATE INDEX index_preset_songs_presetId ON preset_songs (presetId)")
            old.execSQL("INSERT INTO items VALUES (1, '곡', 120, '가요', 5, 5, 0, 0)")
            old.execSQL("INSERT INTO settings_presets VALUES (1, '기존 프리셋', 0, 1, 1)")
            old.execSQL("INSERT INTO preset_categories VALUES (1, '가요', 8, 0)")
            old.execSQL("INSERT INTO preset_songs VALUES (1, 1, '곡', 9, 1)")
            old.version = 5
        }
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(AppDatabase.MIGRATION_5_6).build()
        try {
            val preset = database.presetDao().getPreset(1)!!
            assertEquals(900, preset.targetSeconds)
            assertEquals(30, preset.marginSeconds)
            assertEquals(15, preset.replacementMarginSeconds)
            SettingsPresetRepository(database).apply(1)
            assertEquals(GeneratorSettings(), database.generatorSettingsDao().get())
            val song = database.itemDao().getAllItems().single()
            assertEquals(9, song.weight); assertEquals(8, song.categoryWeight); assertTrue(song.isExcluded)
        } finally { database.close(); context.deleteDatabase(name) }
    }

    @Test
    fun restoresAllFourSettingsWithoutChangingSongMetadata() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = database.itemDao()
            val original = listOf(
                ItemEntity(1, "첫 곡", 120, "가요", weight = 2, categoryWeight = 8, isExcluded = true, isCategoryExcluded = true),
                ItemEntity(2, "둘째 곡", 180, "가요", weight = 6, categoryWeight = 8, isCategoryExcluded = true),
                ItemEntity(3, "피아노", 200, "연주곡", weight = 9)
            )
            dao.insertItems(original)
            val repository = SettingsPresetRepository(database)
            val id = repository.save("공연")
            dao.updateCategorySettings("가요", 1, false)
            dao.updateSongSettings(1, 10, false)
            dao.updateSongSettings(3, 1, true)
            dao.updateItem(dao.getAllItems().first().copy(durationSeconds = 140))
            val result = repository.apply(id)

            assertEquals(2, result.categoriesApplied)
            assertEquals(3, result.songsApplied)
            assertEquals(original.map { if (it.id == 1) it.copy(durationSeconds = 140) else it }, dao.getAllItems())
        } finally { database.close() }
    }

    @Test
    fun skipsReusedNumbersAndPreservesNewSongSettings() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = database.itemDao()
            dao.insertItems(listOf(
                ItemEntity(1, "예전 곡", 120, "가요", weight = 9, categoryWeight = 8, isExcluded = true, isCategoryExcluded = true),
                ItemEntity(2, "유지된 곡", 180, "가요", weight = 2, categoryWeight = 8, isCategoryExcluded = true),
                ItemEntity(4, "삭제될 곡", 200, "옛 분류")
            ))
            val repository = SettingsPresetRepository(database)
            val id = repository.save("예전 설정")
            dao.deleteAllItems()
            dao.insertItems(listOf(
                ItemEntity(1, "번호가 같은 다른 곡", 130, "가요"),
                ItemEntity(2, "유지된 곡", 185, "가요"),
                ItemEntity(3, "새 곡", 200, "가요", weight = 4),
                ItemEntity(9, "새 분류의 곡", 160, "새 분류", categoryWeight = 3)
            ))
            val result = repository.apply(id)
            val songs = dao.getAllItems()
            assertEquals(1, result.categoriesApplied)
            assertEquals(1, result.songsApplied)
            assertEquals(2, result.songsSkipped)
            assertEquals(5, songs[0].weight)
            assertFalse(songs[0].isExcluded)
            assertEquals(2, songs[1].weight)
            assertEquals(4, songs[2].weight)
            assertTrue(songs.take(3).all { it.isCategoryExcluded && it.categoryWeight == 8 })
            assertEquals(3, songs[3].categoryWeight)
            assertFalse(songs[3].isCategoryExcluded)
        } finally { database.close() }
    }

    @Test
    fun duplicateNamesAreRejectedAndOverwriteAndDeleteReplaceSnapshots() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = database.itemDao()
            val presetDao = database.presetDao()
            dao.insertItems(listOf(ItemEntity(1, "첫 곡", 120, "가요"), ItemEntity(2, "둘째 곡", 180, "다른 분류")))
            val repository = SettingsPresetRepository(database)
            val id = repository.save("  평소  ")
            try {
                repository.save("평소")
                fail("Duplicate preset names must be rejected")
            } catch (_: IllegalArgumentException) { }
            assertEquals(1, presetDao.getPresets().size)
            assertEquals(2, presetDao.getSongs(id).size)
            dao.deleteAllItems()
            dao.insertItems(listOf(ItemEntity(1, "첫 곡", 120, "가요", weight = 9, isExcluded = true)))
            repository.overwrite(id)
            assertEquals(1, presetDao.getSongs(id).size)
            assertEquals(1, presetDao.getCategories(id).size)
            dao.updateSongSettings(1, 1, false)
            repository.apply(id)
            assertEquals(9, dao.getAllItems().single().weight)
            assertTrue(dao.getAllItems().single().isExcluded)
            val beforeDelete = dao.getAllItems()
            repository.delete(id)
            assertTrue(presetDao.getPresets().isEmpty())
            assertTrue(presetDao.getSongs(id).isEmpty())
            assertTrue(presetDao.getCategories(id).isEmpty())
            assertEquals(beforeDelete, dao.getAllItems())
        } finally { database.close() }
    }

    @Test
    fun presetsSurviveDatabaseReopenAndSongListReset() = runBlocking {
        val name = "preset-persistence-test.db"
        context.deleteDatabase(name)
        var database = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            database.itemDao().insertItems(listOf(ItemEntity(42, "저장된 곡", 185, "가요", weight = 8, categoryWeight = 7, isExcluded = true)))
            val id = SettingsPresetRepository(database).save("저장된 프리셋")
            database.close()
            database = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            assertEquals("저장된 프리셋", database.presetDao().getPreset(id)?.name)
            database.itemDao().deleteAllItems()
            assertEquals(1, database.presetDao().getPresets().size)
            database.itemDao().insertItems(listOf(ItemEntity(42, "저장된 곡", 190, "가요")))
            SettingsPresetRepository(database).apply(id)
            val restored = database.itemDao().getAllItems().single()
            assertEquals(8, restored.weight)
            assertEquals(7, restored.categoryWeight)
            assertTrue(restored.isExcluded)
            assertEquals(190, restored.durationSeconds)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun migrationFromVersionFourPreservesSettingsAndCreatesPresetStorage() = runBlocking {
        val name = "preset-migration-test.db"
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            old.execSQL("CREATE TABLE items (id INTEGER NOT NULL PRIMARY KEY, title TEXT NOT NULL, durationSeconds INTEGER NOT NULL, category TEXT NOT NULL, weight INTEGER NOT NULL, categoryWeight INTEGER NOT NULL, isExcluded INTEGER NOT NULL, isCategoryExcluded INTEGER NOT NULL DEFAULT 0)")
            old.execSQL("INSERT INTO items VALUES (42, '기존 곡', 185, '기존 분류', 8, 7, 1, 1)")
            old.version = 4
        }
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6).build()
        try {
            val song = database.itemDao().getAllItems().single()
            assertEquals(8, song.weight)
            assertEquals(7, song.categoryWeight)
            assertTrue(song.isExcluded)
            assertTrue(song.isCategoryExcluded)
            val id = SettingsPresetRepository(database).save("업그레이드 후 저장")
            assertEquals(1, database.presetDao().getSongs(id).size)
            assertEquals(1, database.presetDao().getCategories(id).size)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
