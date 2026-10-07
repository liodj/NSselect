package com.example.nsselect.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ItemEntity::class, SettingsPreset::class, PresetCategory::class, PresetSong::class, GeneratorSettings::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun presetDao(): PresetDao
    abstract fun generatorSettingsDao(): GeneratorSettingsDao

    companion object {
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE settings_presets ADD COLUMN targetSeconds INTEGER NOT NULL DEFAULT 900")
                db.execSQL("ALTER TABLE settings_presets ADD COLUMN marginSeconds INTEGER NOT NULL DEFAULT 30")
                db.execSQL("ALTER TABLE settings_presets ADD COLUMN replacementMarginSeconds INTEGER NOT NULL DEFAULT 15")
                db.execSQL("CREATE TABLE IF NOT EXISTS generator_settings (id INTEGER NOT NULL PRIMARY KEY, targetSeconds INTEGER NOT NULL, marginSeconds INTEGER NOT NULL, replacementMarginSeconds INTEGER NOT NULL)")
                db.execSQL("INSERT INTO generator_settings VALUES (1, 900, 30, 15)")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS settings_presets (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, updatedAt INTEGER NOT NULL, songCount INTEGER NOT NULL, categoryCount INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_settings_presets_name ON settings_presets (name)")
                db.execSQL("CREATE TABLE IF NOT EXISTS preset_categories (presetId INTEGER NOT NULL, category TEXT NOT NULL, weight INTEGER NOT NULL, isExcluded INTEGER NOT NULL, PRIMARY KEY(presetId, category), FOREIGN KEY(presetId) REFERENCES settings_presets(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_preset_categories_presetId ON preset_categories (presetId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS preset_songs (presetId INTEGER NOT NULL, songId INTEGER NOT NULL, title TEXT NOT NULL, weight INTEGER NOT NULL, isExcluded INTEGER NOT NULL, PRIMARY KEY(presetId, songId), FOREIGN KEY(presetId) REFERENCES settings_presets(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_preset_songs_presetId ON preset_songs (presetId)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN isCategoryExcluded INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nsselect_database"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
