package com.airi.odslm.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.airi.odslm.data.AppDatabase
import com.airi.odslm.data.ChatEntity
import com.airi.odslm.data.ChatRepository
import com.airi.odslm.data.MessageRole
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the chat screen.
 *
 * Responsibilities:
 * - Expose [uiState] as a single source of truth for [ChatActivity].
 * - Accept user actions ([sendPrompt], [attachImage], [clearError]) and update state.
 * - Persist messages via [ChatRepository].
 * - (Task 7) Delegate inference to InferenceManager — stub returns placeholder now.
 *
 * MVVM contract:
 * - [ChatActivity] may NOT access [ChatRepository] or [ImageProcessor] directly.
 * - [ImageProcessor] is called here (Task 7), not in the Activity.
 * - [InferenceManager] is owned here and cancelled in [onCleared].
 */
class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Observe database messages and sync them into UI state
        repository.allMessages
            .onEach { entities ->
                val uiMessages = entities.map { entity ->
                    ChatMessage(
                        id = entity.id,
                        role = entity.role,
                        content = entity.content,
                        imagePath = entity.imagePath,
                        timestamp = entity.timestamp
                    )
                }
                _uiState.update { it.copy(messages = uiMessages) }
            }
            .catch { error ->
                // Storage read error — surface to UI but don't crash
                _uiState.update { it.copy(error = "Failed to load history: ${error.message}") }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Stores the user-selected image URI in state.
     * ImageProcessor (resize + encode) will be called in Task 7 when inference is integrated.
     *
     * Called by [ChatActivity] when the file picker returns a result.
     * Activity must NOT process the image itself (MVVM boundary).
     */
    fun attachImage(uri: Uri) {
        _uiState.update { it.copy(pendingImageUri = uri) }
    }

    /** Removes the pending image (user cancelled attachment). */
    fun clearPendingImage() {
        _uiState.update { it.copy(pendingImageUri = null) }
    }

    /**
     * Sends a prompt to the model and saves the exchange to the database.
     *
     * Phase 1 stub: inference is not yet wired (that's Task 7).
     * This validates the state flow and persistence pipeline independently.
     */
    fun sendPrompt(text: String) {
        if (text.isBlank()) return

        val imageUri = _uiState.value.pendingImageUri

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, pendingImageUri = null, error = null) }

            // Save user message to database
            val userMessage = ChatEntity(
                role = MessageRole.USER,
                content = text,
                imagePath = imageUri?.toString()
            )
            repository.saveMessage(userMessage)

            // TODO (Task 7): call InferenceManager.infer(text, imageBytes) here.
            // For now, emit a placeholder to confirm the UI pipeline works end-to-end.
            val placeholderResponse = ChatEntity(
                role = MessageRole.ASSISTANT,
                content = "[Inference not yet wired — Task 7]",
                imagePath = null
            )
            repository.saveMessage(placeholderResponse)

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /** Clears the current error from UI state (e.g., after Snackbar is shown). */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        // TODO (Task 7): cancel InferenceManager coroutine scope here.
    }

    /**
     * Factory for creating [ChatViewModel] with its repository dependency.
     * Avoids Hilt/Dagger for Phase 1 single-activity scope. (YAGNI)
     */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val dao = AppDatabase.getInstance(context).chatDao()
            val repository = ChatRepository(dao)
            return ChatViewModel(repository) as T
        }
    }
}
