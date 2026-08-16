package com.airi.odslm.viewmodel

import android.net.Uri

/**
 * Represents a single message in the chat UI.
 *
 * This is a UI model (not a database entity). Mapping from [ChatEntity] happens in [ChatViewModel].
 * [imageUri] is transient — the URI is available while the user is composing.
 * Once saved, only [imagePath] (from the persisted entity) is relevant.
 */
data class ChatMessage(
    val id: Long = 0,
    val role: String,           // MessageRole.USER or MessageRole.ASSISTANT
    val content: String,
    val imagePath: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * UI state for the chat screen.
 *
 * Immutable data class — each state transition produces a new copy.
 * [pendingImageUri] holds the user-selected image URI before the message is sent.
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val pendingImageUri: Uri? = null,
    val error: String? = null
)
