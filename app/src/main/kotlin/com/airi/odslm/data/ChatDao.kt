package com.airi.odslm.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [ChatEntity].
 *
 * All methods are suspend or return Flow — no blocking calls allowed.
 * Query validation happens at compile time via Room's KSP processor.
 */
@Dao
interface ChatDao {

    /**
     * Inserts a new message. Returns the auto-generated row ID.
     * IGNORE conflict strategy: duplicate inserts are silently dropped (safe for retry).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: ChatEntity): Long

    /**
     * Returns all messages ordered by timestamp ascending (oldest first).
     * Returns a Flow so the UI automatically receives updates when new messages are inserted.
     */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatEntity>>

    /** Clears all chat history. Used for testing and future "clear chat" feature. */
    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
}
