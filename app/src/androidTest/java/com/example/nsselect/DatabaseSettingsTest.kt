package com.example.nsselect

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nsselect.data.AppDatabase
import com.example.nsselect.data.ItemEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseSettingsTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun categoryTogglePreservesIndividualSongChoices() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = database.itemDao()
            dao.insertItems(listOf(
                ItemEntity(1, "사용 곡", 120, "가요", weight = 7),
                ItemEntity(2, "제외 곡", 180, "가요", isExcluded = true)
            ))
            dao.updateCategoryExclusion("가요", true)
            assertTrue(dao.getIncludedItems().isEmpty())
            dao.updateCategoryWeight("가요", 9)
            dao.updateItemWeight(1, 3)
            dao.updateCategoryExclusion("가요", false)

            assertEquals(listOf(1), dao.getIncludedItems().map { it.id })
            val songs = dao.getAllItems()
            assertTrue(songs[1].isExcluded)
            assertEquals(3, songs[0].weight)
            assertTrue(songs.all { it.categoryWeight == 9 })
        } finally {
            database.close()
        }
    }

    @Test
    fun migrationPreservesVersionThreeSongsAndSettings() = runBlocking {
        val name = "migration-settings-test.db"
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { oldDatabase ->
            oldDatabase.execSQL("CREATE TABLE items (id INTEGER NOT NULL PRIMARY KEY, title TEXT NOT NULL, durationSeconds INTEGER NOT NULL, category TEXT NOT NULL, weight INTEGER NOT NULL, categoryWeight INTEGER NOT NULL, isExcluded INTEGER NOT NULL)")
            oldDatabase.execSQL("INSERT INTO items VALUES (42, '기존 곡', 185, '기존 분류', 8, 7, 1)")
            oldDatabase.version = 3
        }
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6).build()
        try {
            val song = database.itemDao().getAllItems().single()
            assertEquals(42, song.id)
            assertEquals("기존 곡", song.title)
            assertEquals(185, song.durationSeconds)
            assertEquals(8, song.weight)
            assertEquals(7, song.categoryWeight)
            assertTrue(song.isExcluded)
            assertFalse(song.isCategoryExcluded)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
