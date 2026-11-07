package com.fsck.k9.ui.localai

import java.util.Locale

enum class ClassificationSource {
    RULES,
    LOCAL_LLM,
    CACHE,
}

data class CategoryResult(
    val category: EmailCategory,
    val source: ClassificationSource,
)

/**
 * Rules first, then the optional on-device model for ambiguous messages only.
 *
 * The prompt is deliberately tiny and asks for exactly one label; the answer parser accepts only the nine known
 * category names. Any failure falls back to the rule result, so the feature degrades to rule-only automatically.
 */
class CategoryClassifier(private val engine: LocalLlmEngine) {
    suspend fun classify(
        subject: String?,
        sender: String?,
        hasUnsubscribeHeader: Boolean,
        useLocalModel: Boolean,
    ): CategoryResult {
        val ruleResult = CategoryRules.classify(subject, sender, hasUnsubscribeHeader)
        if (!ruleResult.ambiguous) {
            return CategoryResult(ruleResult.category, ClassificationSource.RULES)
        }

        if (!useLocalModel || !engine.isReady) {
            return CategoryResult(ruleResult.category, ClassificationSource.RULES)
        }

        val response = engine.generate(
            prompt = buildPrompt(subject, sender),
            maxTokens = MAX_OUTPUT_TOKENS,
            temperature = LOW_TEMPERATURE,
        )

        val category = parseCategory(response) ?: return CategoryResult(ruleResult.category, ClassificationSource.RULES)
        return CategoryResult(category, ClassificationSource.LOCAL_LLM)
    }

    private fun buildPrompt(subject: String?, sender: String?): String {
        val cleanSubject = subject.orEmpty()
            .replace(OTP_PATTERN, "[code]")
            .replace('\n', ' ')
            .take(MAX_SUBJECT_LENGTH)
        val cleanSender = sender.orEmpty().replace('\n', ' ').take(MAX_SENDER_LENGTH)
        return """
            |Classify this email into exactly one category:
            |finance, gaming, subscription, shopping, travel, verification, social, newsletter, other.
            |
            |Sender: $cleanSender
            |Subject: $cleanSubject
            |
            |Answer with the category word only. No punctuation.
            |Category:
        """.trimMargin()
    }

    private fun parseCategory(response: String?): EmailCategory? {
        if (response == null) return null

        val normalized = response.lowercase(Locale.ROOT).trim()

        // Accept an exact answer first.
        EmailCategory.fromApiName(normalized)?.let { return it }

        // Then look for the earliest known label in the response, not the
        // first enum entry: "not finance, it is travel" must mean travel.
        var bestCategory: EmailCategory? = null
        var bestIndex = Int.MAX_VALUE
        for (category in EmailCategory.entries) {
            val match = Regex("(^|[^a-z])${Regex.escape(category.apiName)}([^a-z]|$)")
                .find(normalized) ?: continue
            if (match.range.first < bestIndex) {
                bestIndex = match.range.first
                bestCategory = category
            }
        }
        return bestCategory
    }

    private companion object {
        val OTP_PATTERN = Regex("\\b\\d{4,8}\\b")
        const val MAX_OUTPUT_TOKENS = 32
        const val LOW_TEMPERATURE = 0.1f
        const val MAX_SUBJECT_LENGTH = 160
        const val MAX_SENDER_LENGTH = 120
    }
}
