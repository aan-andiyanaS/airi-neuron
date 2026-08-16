package com.airi.odslm.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository for chat message persistence.
 *
 * Single responsibility: abstract Room DAO from the rest of the app.
 * ViewModels must use this class, never access [ChatDao] directly.
 *
 * No business logic here — mapping, filtering, and formatting belong in ViewModel.
 */
class ChatRepository(private val chatDao: ChatDao) {

    /** Emits the full message list, re-emitting automatically on every insert. */
    val allMessages: Flow<List<ChatEntity>> = chatDao.getAllMessages()

    /**
     * Saves a new message to the database.
     * Returns [Result] so callers can handle storage errors without crashing.
     */
    suspend fun saveMessage(message: ChatEntity): Result<Long> = runCatching {
        chatDao.insertMessage(message)
    }

    /** Deletes all stored messages. For testing and future "clear chat" feature. */
    suspend fun clearHistory(): Result<Unit> = runCatching {
        chatDao.clearAll()
    }
}
