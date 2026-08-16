package com.airi.odslm.util

/**
 * Filters the output from the model to prevent inappropriate content.
 * Requirements: NF9 (Task 8).
 * 
 * Ponytail: Keep it simple for PoC. Just a static list of regex patterns.
 */
object OutputFilter {
    // Simple blocklist for Phase 1
    private val blockedPatterns = listOf(
        Regex("(?i)\\b(bomb|terrorist|kill|suicide)\\b")
    )

    /**
     * Returns true if the output is safe, false if it contains blocked content.
     */
    fun isSafe(text: String): Boolean {
        if (text.isBlank()) return true
        
        for (pattern in blockedPatterns) {
            if (pattern.containsMatchIn(text)) {
                return false
            }
        }
        return true
    }
}
