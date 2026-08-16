package com.airi.odslm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single chat message stored in the local Room database.
 *
 * [role] uses string constants from [MessageRole] to avoid magic strings.
 * [imagePath] is nullable — only set for messages that include an image attachment.
 * [timestamp] is Unix epoch milliseconds for sorting and display.
 */
@Entity(tableName = "chat_messages")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String,       // MessageRole.USER or MessageRole.ASSISTANT
    val content: String,
    val imagePath: String?,  // Nullable: only for user messages with attached image
    val timestamp: Long = System.currentTimeMillis()
)

/** String constants for message roles — avoids magic strings across the codebase. */
object MessageRole {
    const val USER = "user"
    const val ASSISTANT = "assistant"
}
