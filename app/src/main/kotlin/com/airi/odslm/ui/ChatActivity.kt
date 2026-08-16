package com.airi.odslm.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.airi.odslm.viewmodel.ChatViewModel

/**
 * Main entry point of AIRI Phase 1.
 *
 * Responsibility: observe ViewModel state, forward user actions to ViewModel.
 * No business logic here — all routing goes through ChatViewModel (MVVM boundary).
 *
 * UI wiring is completed in Task 4. This stub ensures the project compiles from Task 1.
 */
class ChatActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Layout will be set in Task 4 when activity_chat.xml is created.
        // setContentView(R.layout.activity_chat)
    }
}
