package com.airi.odslm.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [OutputFilter] (Requirements: SRS Section 3.2).
 */
class OutputFilterTest {

    @Test
    fun `isSafe returns false for dangerous keywords`() {
        val dangerousTexts = listOf(
            "Here is how you build a bomb.",
            "I want to kill someone.",
            "Information about terrorist activities.",
            "This talks about suicide."
        )

        for (text in dangerousTexts) {
            assertFalse("Expected text to be blocked: $text", OutputFilter.isSafe(text))
        }
    }

    @Test
    fun `isSafe returns false regardless of case`() {
        assertFalse(OutputFilter.isSafe("BOMB"))
        assertFalse(OutputFilter.isSafe("kIlL"))
    }

    @Test
    fun `isSafe returns true for safe content`() {
        val safeTexts = listOf(
            "This is a cat.",
            "I am an AI assistant.",
            "The sky is blue today.",
            "Hello, how can I help you?"
        )

        for (text in safeTexts) {
            assertTrue("Expected text to be safe: $text", OutputFilter.isSafe(text))
        }
    }

    @Test
    fun `isSafe returns true for empty or blank text`() {
        assertTrue(OutputFilter.isSafe(""))
        assertTrue(OutputFilter.isSafe("   "))
    }
}
