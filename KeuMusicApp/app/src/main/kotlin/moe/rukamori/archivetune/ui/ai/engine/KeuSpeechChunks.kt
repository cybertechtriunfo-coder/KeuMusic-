/*
 * Adapted from Jarvis Android (MIT License), SpeechChunks.kt.
 * Copyright (c) Jarvis contributors. Original: https://github.com/judejulius/jarvis
 */
package moe.rukamori.archivetune.ui.ai.engine

/** Divide respostas longas em trechos naturais para o Keu começar a falar mais cedo. */
internal object KeuSpeechChunks {
    private val sentence = Regex(".+?(?:[.!?…]+[\"')\\]]*(?=\\s|$)|$)\\s*", RegexOption.DOT_MATCHES_ALL)

    fun split(text: String, first: Int = 72, max: Int = 150): List<String> {
        val result = mutableListOf<String>()
        var remaining = text.trim()
        while (remaining.isNotEmpty()) {
            val limit = if (result.isEmpty()) first else max
            val (part, rest) = take(remaining, limit)
            if (part.isBlank()) break
            result += part
            remaining = rest
        }
        return result
    }

    private fun take(text: String, limit: Int): Pair<String, String> {
        val value = text.trim()
        if (value.length <= limit) return value to ""
        var end = 0
        for (match in sentence.findAll(value)) {
            val stop = match.range.first + match.value.trimEnd().length
            if (stop > limit) break
            end = stop
        }
        if (end == 0) {
            val head = value.substring(0, minOf(value.length, limit + 1))
            val comma = maxOf(head.lastIndexOf(", "), head.lastIndexOf("; "), head.lastIndexOf(": "))
            end = if (comma >= limit / 3.0) comma + 1 else head.lastIndexOf(' ').takeIf { it > 0 } ?: limit
        }
        return value.substring(0, end).trim() to value.substring(end).trim()
    }
}
