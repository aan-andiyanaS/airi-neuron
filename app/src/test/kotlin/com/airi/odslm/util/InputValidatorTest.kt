package com.airi.odslm.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [InputValidator] (Requirements: SRS Section 3.1).
 */
class InputValidatorTest {

    @Test
    fun `validateText returns true for valid text`() {
        assertTrue(InputValidator.validateText("Hello, how are you?"))
        
        // Exact boundary (1000 chars)
        val boundaryText = "a".repeat(1000)
        assertTrue(InputValidator.validateText(boundaryText))
    }

    @Test
    fun `validateText returns false for blank text`() {
        assertFalse(InputValidator.validateText(""))
        assertFalse(InputValidator.validateText("   "))
        assertFalse(InputValidator.validateText("\n\t"))
    }

    @Test
    fun `validateText returns false for text exceeding max length`() {
        val longText = "a".repeat(1001) // Max is 1000
        assertFalse(InputValidator.validateText(longText))
        
        val veryLongText = "b".repeat(5000)
        assertFalse(InputValidator.validateText(veryLongText))
    }
    
    // Note: Image validation relies on Context and ContentResolver which requires Android framework mocking.
    // For pure JVM unit testing in Phase 1, we only test the text validation here.
    // Full image size testing should ideally be done in an Android instrumentation test or via MockK 
    // wrapping the Context and ContentResolver, but keeping it simple for ponytail.
}
