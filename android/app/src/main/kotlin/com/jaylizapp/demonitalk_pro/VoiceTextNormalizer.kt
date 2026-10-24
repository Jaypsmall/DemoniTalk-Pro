package com.jaylizapp.demonitalk_pro

import java.text.Normalizer
import java.util.regex.Pattern

object VoiceTextNormalizer {
    private val diacriticsPattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
    private val whitespacePattern = Regex("\\s+")
    private val nonAlphanumericPattern = Regex("[^a-z0-9]+")

    private val demoniAliasList = listOf(
        "demonio",
        "de money",
        "de moni",
        "d money",
        "demoni",
        "demony",
        "demon y",
        "demon",
        "maquina",
        "hermano",
        "amigo"
    ).sortedByDescending { it.length }

    private val compiledAliasRegexes = demoniAliasList.map { alias ->
        Regex("(?<!\\S)${Regex.escape(alias)}(?!\\S)")
    }

    private val wakeWords = listOf("hermano", "amigo", "maquina", "demoni", "demonio")

    data class WakeWordMatch(
        val wakeWord: String,
        val remainingText: String
    )

    fun normalize(text: String): String {
        if (text.isEmpty()) return ""

        // Keep voice normalization in Kotlin; JNI normalization aborted on
        // affected Android devices during speech-result handling.
        val nfdNormalizedString = Normalizer.normalize(text, Normalizer.Form.NFD)
        val withoutDiacritics = diacriticsPattern.matcher(nfdNormalizedString).replaceAll("")
        var cleanText = withoutDiacritics
            .lowercase()
            .replace(nonAlphanumericPattern, " ")
            .trim()
            .replace(whitespacePattern, " ")

        var idx = 0
        val total = compiledAliasRegexes.size
        while (idx < total) {
            cleanText = cleanText.replace(compiledAliasRegexes[idx], "demoni")
            idx++
        }
        return cleanText.replace(whitespacePattern, " ").trim()
    }

    fun findWakeWord(text: String): WakeWordMatch? {
        if (text.isEmpty()) return null

        // Use the Kotlin wake-word parser to keep voice recognition off JNI.
        val normalizedText = normalize(text)
        if (normalizedText.isEmpty()) return null

        val bestMatch = wakeWords.mapNotNull { wakeWord ->
            val match = Regex("(?<!\\S)(${Regex.escape(wakeWord)})(?!\\S)")
                .find(normalizedText)
                ?: return@mapNotNull null

            val groupRange = match.groups[1]?.range ?: return@mapNotNull null
            WakeCandidate(wakeWord, groupRange.first, groupRange.last + 1)
        }.minWithOrNull(compareBy<WakeCandidate> { it.start }.thenByDescending { it.end })

        return bestMatch?.let {
            WakeWordMatch(
                wakeWord = it.word,
                remainingText = normalizedText.substring(it.end).trim()
            )
        }
    }

    private data class WakeCandidate(
        val word: String,
        val start: Int,
        val end: Int
    )
}
