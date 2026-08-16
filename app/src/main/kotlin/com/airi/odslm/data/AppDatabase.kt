package com.airi.odslm.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for AIRI Phase 1.
 *
 * Single table: [ChatEntity] (chat_messages).
 * No encryption in Phase 1 — Android internal storage protection is sufficient for a PoC.
 * SQLCipher deferred to Phase 2 (YAGNI).
 *
 * Version history:
 *   1 → Initial schema (chat_messages table).
 */
@Database(
    entities = [ChatEntity::class],
    version = 1,
    exportSchema = true // Export schema for version tracking — add to .gitignore if too noisy
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao

    companion object {
        private const val DATABASE_NAME = "chat_history.db"

        @Volatile
        private var instance: AppDatabase? = null

        /**
         * Returns the singleton database instance, creating it if necessary.
         * Thread-safe via double-checked locking + @Volatile.
         */
        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }

        private fun buildDatabase(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            ).build()
    }
}
