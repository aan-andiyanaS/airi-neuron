package com.airi.odslm.data

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class ChatRepositoryTest {

    private lateinit var chatDao: ChatDao
    private lateinit var repository: ChatRepository

    @Before
    fun setUp() {
        chatDao = mockk(relaxed = true)
        repository = ChatRepository(chatDao)
    }

    @Test
    fun `saveMessage returns success when dao inserts correctly`() = runTest {
        val message = ChatEntity(content = "Test", role = MessageRole.USER, imagePath = null)
        coEvery { chatDao.insertMessage(message) } returns 1L

        val result = repository.saveMessage(message)

        assertTrue(result.isSuccess)
        assertEquals(1L, result.getOrNull())
        coVerify(exactly = 1) { chatDao.insertMessage(message) }
    }

    @Test
    fun `saveMessage returns failure when dao throws exception`() = runTest {
        val message = ChatEntity(content = "Test", role = MessageRole.USER, imagePath = null)
        val exception = RuntimeException("Database error")
        coEvery { chatDao.insertMessage(message) } throws exception

        val result = repository.saveMessage(message)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `clearHistory returns success when dao clears correctly`() = runTest {
        coEvery { chatDao.clearAll() } returns Unit

        val result = repository.clearHistory()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { chatDao.clearAll() }
    }

    @Test
    fun `clearHistory returns failure when dao throws exception`() = runTest {
        val exception = RuntimeException("Database error")
        coEvery { chatDao.clearAll() } throws exception

        val result = repository.clearHistory()

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
