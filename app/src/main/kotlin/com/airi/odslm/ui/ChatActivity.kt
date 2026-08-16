package com.airi.odslm.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airi.odslm.R
import com.airi.odslm.viewmodel.ChatViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * Main entry point of AIRI Neuron Phase 1.
 *
 * Responsibility: observe ViewModel state, forward user actions to ViewModel.
 * No business logic here — all logic routes through [ChatViewModel] (MVVM boundary).
 *
 * MVVM rules enforced here:
 * - Image URI is forwarded to ViewModel via [ChatViewModel.attachImage], not processed here.
 * - Send delegates to [ChatViewModel.sendPrompt], not to Repository directly.
 */
class ChatActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModel.Factory(applicationContext)
    }

    private lateinit var chatAdapter: ChatAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextMessage: TextInputEditText
    private lateinit var buttonSend: MaterialButton
    private lateinit var buttonAttach: MaterialButton
    private lateinit var progressLoading: LinearProgressIndicator

    /** Launches the system image picker; result forwarded to ViewModel. */
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> viewModel.attachImage(uri) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        bindViews()
        setupRecyclerView()
        setupClickListeners()
        observeUiState()
    }

    private fun bindViews() {
        recyclerView = findViewById(R.id.recyclerViewChat)
        editTextMessage = findViewById(R.id.editTextMessage)
        buttonSend = findViewById(R.id.buttonSend)
        buttonAttach = findViewById(R.id.buttonAttach)
        progressLoading = findViewById(R.id.progressLoading)
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true  // New messages appear at the bottom
        }
        recyclerView.apply {
            adapter = chatAdapter
            this.layoutManager = layoutManager
        }
    }

    private fun setupClickListeners() {
        buttonSend.setOnClickListener {
            val text = editTextMessage.text?.toString() ?: return@setOnClickListener
            viewModel.sendPrompt(text)
            editTextMessage.text?.clear()
        }

        buttonAttach.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            imagePickerLauncher.launch(intent)
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            // repeatOnLifecycle ensures collection stops when activity is in background
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    chatAdapter.submitList(state.messages) {
                        // Scroll to bottom after list update
                        if (state.messages.isNotEmpty()) {
                            recyclerView.smoothScrollToPosition(state.messages.size - 1)
                        }
                    }

                    progressLoading.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE

                    state.error?.let { errorMsg ->
                        Toast.makeText(this@ChatActivity, errorMsg, Toast.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }
}
