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
import com.airi.odslm.util.InputValidator
import com.airi.odslm.util.OutputFilter
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
class ChatViewModel(
    private val repository: ChatRepository,
    private val inferenceManager: InferenceManager
) : ViewModel() {

    companion object {
        private const val MODEL_PATH = "/sdcard/minicpm-v-4.6.Q4_K_M.gguf"
        private const val MMPROJ_PATH = "/sdcard/mmproj-model-f16.gguf"
        private const val CONTEXT_SIZE = 1024
    }

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

        // Pre-load model on startup (Phase 1 hardcoded paths per Setup doc)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val success = inferenceManager.loadModel(
                modelPath = MODEL_PATH,
                mmProjPath = MMPROJ_PATH,
                contextSize = CONTEXT_SIZE
            )
            if (!success) {
                _uiState.update { it.copy(error = "Warning: Failed to load model from /sdcard. Check files.") }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
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
        if (!InputValidator.validateText(text)) {
            _uiState.update { it.copy(error = "Text exceeds maximum length.") }
            return
        }

        val imageUri = _uiState.value.pendingImageUri
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, pendingImageUri = null, error = null) }

            // Save user message to database
            val userMessage = ChatEntity(
                role = MessageRole.USER,
                content = text,
                imagePath = imageUri?.toString()
            )
            val userSaveResult = repository.saveMessage(userMessage)
            if (userSaveResult.isFailure) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to save message to storage.") }
                return@launch
            }

            // Inference
            val inferenceResult = inferenceManager.infer(text, imageUri)
            
            inferenceResult.onSuccess { rawResponse ->
                // Security Filter
                val safeResponse = if (OutputFilter.isSafe(rawResponse)) {
                    rawResponse
                } else {
                    "Maaf, saya tidak dapat merespons permintaan tersebut (Terfilter)."
                }

                val assistantMessage = ChatEntity(
                    role = MessageRole.ASSISTANT,
                    content = safeResponse,
                    imagePath = null
                )
                val assistantSaveResult = repository.saveMessage(assistantMessage)
                if (assistantSaveResult.isFailure) {
                    _uiState.update { it.copy(error = "Warning: Failed to save AI response to storage.") }
                }
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.message ?: "Unknown inference error occurred.") }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /** Clears the current error from UI state (e.g., after Snackbar is shown). */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        inferenceManager.unloadModel()
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
            val inferenceManager = InferenceManager(context.applicationContext)
            return ChatViewModel(repository, inferenceManager) as T
        }
    }
}
