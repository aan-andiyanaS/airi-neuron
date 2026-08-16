package com.airi.odslm

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.airi.odslm.data.AppDatabase
import com.airi.odslm.data.ChatDao
import com.airi.odslm.data.ChatEntity
import com.airi.odslm.data.ChatRepository
import com.airi.odslm.data.MessageRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for [ChatRepository] using Room in-memory database.
 * These tests run on the JVM (no device needed) via Robolectric.
 */
@RunWith(AndroidJUnit4::class)
class ChatRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ChatRepository
    private lateinit var dao: ChatDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.chatDao()
        repository = ChatRepository(dao)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `saveMessage inserts message and getAllMessages emits it`() = runTest {
        val message = ChatEntity(
            role = MessageRole.USER,
            content = "Hello!",
            imagePath = null
        )

        val saveResult = repository.saveMessage(message)
        assertTrue("Save should succeed", saveResult.isSuccess)

        val messages = repository.allMessages.first()
        assertEquals(1, messages.size)
        assertEquals("Hello!", messages[0].content)
        assertEquals(MessageRole.USER, messages[0].role)
    }

    @Test
    fun `allMessages emits empty list when database is empty`() = runTest {
        val messages = repository.allMessages.first()
        assertTrue("Empty database should return empty list", messages.isEmpty())
    }

    @Test
    fun `saveMessage with assistant role stores correct role`() = runTest {
        val message = ChatEntity(
            role = MessageRole.ASSISTANT,
            content = "I am AIRI.",
            imagePath = null
        )

        repository.saveMessage(message)

        val messages = repository.allMessages.first()
        assertEquals(MessageRole.ASSISTANT, messages[0].role)
    }

    @Test
    fun `saveMessage with imagePath stores path correctly`() = runTest {
        val imagePath = "/data/data/com.airi.odslm/cache/img_001.jpg"
        val message = ChatEntity(
            role = MessageRole.USER,
            content = "What is this?",
            imagePath = imagePath
        )

        repository.saveMessage(message)

        val messages = repository.allMessages.first()
        assertEquals(imagePath, messages[0].imagePath)
    }

    @Test
    fun `messages are ordered by timestamp ascending`() = runTest {
        val first = ChatEntity(role = MessageRole.USER, content = "First", imagePath = null, timestamp = 1000L)
        val second = ChatEntity(role = MessageRole.ASSISTANT, content = "Second", imagePath = null, timestamp = 2000L)

        // Insert in reverse order to verify sorting
        repository.saveMessage(second)
        repository.saveMessage(first)

        val messages = repository.allMessages.first()
        assertEquals(2, messages.size)
        assertEquals("First", messages[0].content)
        assertEquals("Second", messages[1].content)
    }

    @Test
    fun `clearHistory removes all messages`() = runTest {
        repository.saveMessage(ChatEntity(role = MessageRole.USER, content = "msg1", imagePath = null))
        repository.saveMessage(ChatEntity(role = MessageRole.USER, content = "msg2", imagePath = null))

        val clearResult = repository.clearHistory()
        assertTrue("Clear should succeed", clearResult.isSuccess)

        val messages = repository.allMessages.first()
        assertTrue("All messages should be deleted", messages.isEmpty())
    }

    @Test
    fun `saveMessage with empty content returns success`() = runTest {
        // Edge case: empty string content (not null) — Room should accept it
        val message = ChatEntity(role = MessageRole.USER, content = "", imagePath = null)
        val result = repository.saveMessage(message)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `saveMessage with very long content stores correctly`() = runTest {
        val longContent = "a".repeat(5_000) // 5000 chars — beyond UI limit but storage should handle it
        val message = ChatEntity(role = MessageRole.USER, content = longContent, imagePath = null)
        val result = repository.saveMessage(message)
        assertTrue(result.isSuccess)

        val messages = repository.allMessages.first()
        assertEquals(5_000, messages[0].content.length)
    }
}
