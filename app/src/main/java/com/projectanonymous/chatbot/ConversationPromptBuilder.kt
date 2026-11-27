package com.projectanonymous.chatbot

/** Builds a bounded query carrying recent thread context into a fresh AI Mode search. */
object ConversationPromptBuilder {
    const val MAX_QUERY_LENGTH = 9_000
    private const val MAX_LATEST_PROMPT_LENGTH = 6_800
    private const val MAX_HISTORY_TURNS = 8
    private const val MAX_TURN_TEXT_LENGTH = 220

    fun build(history: List<ChatMessage>, latestPrompt: String): String {
        val latest = latestPrompt.trim().take(MAX_LATEST_PROMPT_LENGTH)
        if (latest.isEmpty()) return ""

        val turns = history.takeLast(MAX_HISTORY_TURNS).mapNotNull { message ->
            val body = message.text.replace(Regex("\\s+"), " ").trim().take(MAX_TURN_TEXT_LENGTH)
            body.takeIf { it.isNotEmpty() }?.let {
                (if (message.fromUser) "User: " else "Assistant: ") + it
            }
        }

        val context = if (turns.isEmpty()) "" else
            "Recent conversation context (background only):\n" + turns.joinToString("\n") + "\n\n"
        val suffix = "\nAnswer the latest user request. Use previous turns only when relevant."
        val prefix = "Latest user request:\n"
        val availableContext = (MAX_QUERY_LENGTH - prefix.length - latest.length - suffix.length).coerceAtLeast(0)
        val boundedContext = context.takeLast(availableContext)
        return (boundedContext + prefix + latest + suffix).take(MAX_QUERY_LENGTH)
    }
}
