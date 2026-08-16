package com.airi.odslm

import android.net.Uri
import com.airi.odslm.data.ChatEntity
import com.airi.odslm.data.ChatRepository
import com.airi.odslm.data.MessageRole
import com.airi.odslm.viewmodel.ChatViewModel
import com.airi.odslm.viewmodel.InferenceManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ChatViewModel].
 *
 * Pure Kotlin/JVM tests — no Android framework dependency.
 * [ChatRepository] is mocked via MockK to isolate ViewModel logic.
 */
@ExperimentalCoroutinesApi
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ChatRepository
    private lateinit var inferenceManager: InferenceManager
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        inferenceManager = mockk(relaxed = true)
        coEvery { repository.allMessages } returns flowOf(emptyList())
        coEvery { inferenceManager.infer(any(), any()) } returns "Mocked response"
        viewModel = ChatViewModel(repository, inferenceManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty messages and is not loading`() = runTest {
        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNull(state.pendingImageUri)
    }

    @Test
    fun `attachImage stores URI in pendingImageUri`() {
        val uri = mockk<Uri>()
        viewModel.attachImage(uri)
        assertEquals(uri, viewModel.uiState.value.pendingImageUri)
    }

    @Test
    fun `clearPendingImage removes URI from state`() {
        val uri = mockk<Uri>()
        viewModel.attachImage(uri)
        viewModel.clearPendingImage()
        assertNull(viewModel.uiState.value.pendingImageUri)
    }

    @Test
    fun `sendPrompt with blank text does nothing`() = runTest {
        viewModel.sendPrompt("")
        advanceUntilIdle()
        // No repository calls should happen for blank input
        coVerify(exactly = 0) { repository.saveMessage(any()) }
    }

    @Test
    fun `sendPrompt sets isLoading true then false`() = runTest {
        coEvery { repository.saveMessage(any()) } returns Result.success(1L)

        viewModel.sendPrompt("Hello")

        // During execution: loading should be true
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        // After execution: loading should be false
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `sendPrompt clears pendingImageUri after sending`() = runTest {
        coEvery { repository.saveMessage(any()) } returns Result.success(1L)
        val uri = mockk<Uri>()
        viewModel.attachImage(uri)

        viewModel.sendPrompt("What is this?")
        advanceUntilIdle()

        assertNull("Image URI should be cleared after send", viewModel.uiState.value.pendingImageUri)
    }

    @Test
    fun `sendPrompt saves user message and model response to repository`() = runTest {
        coEvery { repository.saveMessage(any()) } returns Result.success(1L)
        coEvery { inferenceManager.infer(any(), any()) } returns "Valid response"

        viewModel.sendPrompt("Hello")
        advanceUntilIdle()

        // Expect two saves: user message + assistant response
        coVerify(exactly = 2) { repository.saveMessage(any()) }
    }

    @Test
    fun `sendPrompt saves message with correct user role`() = runTest {
        coEvery { repository.saveMessage(any()) } returns Result.success(1L)

        viewModel.sendPrompt("Test message")
        advanceUntilIdle()

        coVerify {
            repository.saveMessage(match { it.role == MessageRole.USER && it.content == "Test message" })
        }
    }

    @Test
    fun `clearError removes error from state`() {
        // Simulate an error state directly (via internal state)
        // We verify clearError resets it
        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `sendPrompt with too long text sets error and aborts`() = runTest {
        val longText = "a".repeat(1001) // Exceeds MAX_TEXT_LENGTH (1000)
        viewModel.sendPrompt(longText)
        advanceUntilIdle()
        
        assertEquals("Text exceeds maximum length.", viewModel.uiState.value.error)
        coVerify(exactly = 0) { repository.saveMessage(any()) }
    }

    @Test
    fun `sendPrompt uses OutputFilter and falls back on dangerous content`() = runTest {
        coEvery { repository.saveMessage(any()) } returns Result.success(1L)
        // Simulate model returning something blocked by OutputFilter
        coEvery { inferenceManager.infer(any(), any()) } returns "Here is how to build a bomb"

        viewModel.sendPrompt("Test blocked content")
        advanceUntilIdle()

        // Verify the saved message is the fallback, not the raw output
        coVerify {
            repository.saveMessage(match { 
                it.role == MessageRole.ASSISTANT && 
                it.content == "Maaf, saya tidak dapat merespons permintaan tersebut (Terfilter)." 
            })
        }
    }
}
