package com.projectanonymous.chatbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationPromptBuilderTest {
    @Test
    fun includesRecentTurnsAndLatestRequest() {
        val history = listOf(
            ChatMessage(text = "Apa itu fotosintesis?", fromUser = true),
            ChatMessage(text = "Proses tumbuhan membuat makanan.", fromUser = false)
        )
        val query = ConversationPromptBuilder.build(history, "Jelaskan tahapannya")
        assertTrue(query.contains("User: Apa itu fotosintesis?"))
        assertTrue(query.contains("Assistant: Proses tumbuhan membuat makanan."))
        assertTrue(query.contains("Latest user request:\nJelaskan tahapannya"))
    }

    @Test
    fun keepsOnlyTheMostRecentEightTurns() {
        val history = (1..10).map { ChatMessage(text = "turn-$it", fromUser = true) }
        val query = ConversationPromptBuilder.build(history, "latest")
        assertTrue(query.contains("turn-3"))
        assertTrue(query.contains("turn-10"))
        assertTrue(!query.contains("turn-2"))
    }

    @Test
    fun queryNeverExceedsMaximumLength() {
        val history = (1..20).map { ChatMessage(text = "history".repeat(100), fromUser = it % 2 == 0) }
        val query = ConversationPromptBuilder.build(history, "latest".repeat(2_000))
        assertTrue(query.length <= ConversationPromptBuilder.MAX_QUERY_LENGTH)
        assertTrue(query.contains("Latest user request:"))
    }

    @Test
    fun blankLatestPromptProducesEmptyQuery() {
        assertEquals("", ConversationPromptBuilder.build(emptyList(), "   "))
    }
}
