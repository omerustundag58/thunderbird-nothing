package com.fsck.k9.ui.localai

import app.k9mail.legacy.mailstore.MessageListRepository
import app.k9mail.legacy.mailstore.MessageMapper
import app.k9mail.legacy.mailstore.MessageStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId

/**
 * Reads locally stored message headers, classifies them and keeps the result in [CategoryStore].
 *
 * Only cached (already downloaded) messages are touched. Nothing is moved on the server and no network call is made.
 */
class LocalAiRepository(
    private val accountManager: LegacyAccountManager,
    private val messageListRepository: MessageListRepository,
    private val messageStoreManager: MessageStoreManager,
    private val categoryStore: CategoryStore,
    private val preferences: LocalAiPreferences,
    private val modelManager: LocalModelManager,
    private val classifier: CategoryClassifier,
    private val engine: LocalLlmEngine,
) {
    fun loadOverview(): List<CategoryOverview> = buildOverview(categoryStore.getAll())

    fun messagesFor(category: EmailCategory): List<CachedCategory> {
        return categoryStore.getAll()
            .filter { it.emailCategory == category }
            .sortedByDescending { it.date }
    }

    fun clearCache() {
        categoryStore.clear()
    }

    suspend fun scan(onProgress: (ScanProgress) -> Unit = {}): ScanSummary = withContext(Dispatchers.IO) {
        val messages = loadCachedMessages()

        val useModel = preferences.isLlmEnabled && modelManager.hasModel()
        val modelReady = useModel && engine.initialize(modelManager.modelFile)
        var languageModelBudget = if (modelReady) MAX_LLM_MESSAGES_PER_SCAN else 0

        var ruleCount = 0
        var languageModelCount = 0
        var cacheHits = 0
        val updates = mutableListOf<CachedCategory>()

        try {
            messages.values.forEachIndexed { index, message ->
            val cached = categoryStore.get(message.accountId, message.messageId)
            val canReuse = cached != null &&
                cached.subject == message.subject &&
                cached.category.isNotBlank()

            if (canReuse) {
                cacheHits++
            } else {
                val preliminary = CategoryRules.classify(message.subject, message.sender)
                val hasUnsubscribe = preliminary.ambiguous && hasUnsubscribeHeader(message)

                val useModelForThisMessage = modelReady && languageModelBudget > 0
                val result = classifier.classify(
                    subject = message.subject,
                    sender = message.sender,
                    hasUnsubscribeHeader = hasUnsubscribe,
                    useLocalModel = useModelForThisMessage,
                )

                if (useModelForThisMessage) {
                    languageModelBudget--
                }
                if (result.source == ClassificationSource.LOCAL_LLM) {
                    languageModelCount++
                } else {
                    ruleCount++
                }

                updates += CachedCategory(
                    accountId = message.accountId,
                    messageId = message.messageId,
                    category = result.category.apiName,
                    subject = message.subject,
                    sender = message.sender,
                    date = message.date,
                    source = result.source.name,
                )
            }

            if (index % PROGRESS_INTERVAL == 0 || index == messages.size - 1) {
                onProgress(
                    ScanProgress(
                        processed = index + 1,
                        total = messages.size,
                        ruleCount = ruleCount,
                        languageModelCount = languageModelCount,
                    ),
                )
            }
            }
        } finally {
            if (modelReady) {
                engine.close()
            }
        }

        categoryStore.putAll(updates)
        categoryStore.pruneExcept(messages.keys)
        preferences.lastScanAt = System.currentTimeMillis()

        ScanSummary(
            scannedMessages = messages.size,
            classifiedByRules = ruleCount,
            classifiedByLanguageModel = languageModelCount,
            cacheHits = cacheHits,
            modelAvailable = modelReady,
            overview = buildOverview(categoryStore.getAll()),
        )
    }

    private fun loadCachedMessages(): Map<String, ScannedMessage> {
        val result = linkedMapOf<String, ScannedMessage>()

        for (account in accountManager.findAll()) {
            val accountId = account.id
            val accountKey = accountId.toString()
            val mapper = MessageMapper<ScannedMessage> { accessor ->
                ScannedMessage(
                    accountIdValue = accountId,
                    accountId = accountKey,
                    messageId = accessor.id,
                    folderId = accessor.folderId,
                    messageServerId = accessor.messageServerId,
                    subject = accessor.subject.orEmpty(),
                    sender = accessor.fromAddresses.firstOrNull()?.address.orEmpty(),
                    date = accessor.messageDate,
                )
            }

            runCatching {
                messageListRepository.getMessages(
                    accountId = accountId,
                    selection = "1",
                    selectionArgs = emptyArray(),
                    sortOrder = "date DESC",
                    messageMapper = mapper,
                )
            }.getOrNull()
                .orEmpty()
                .asSequence()
                .filter { it.subject.isNotBlank() || it.sender.isNotBlank() }
                .take(MAX_MESSAGES_PER_ACCOUNT)
                .forEach { message ->
                    result[message.key] = message
                }
        }

        return result
    }

    private fun hasUnsubscribeHeader(message: ScannedMessage): Boolean {
        return runCatching {
            messageStoreManager.getMessageStore(message.accountIdValue)
                .getHeaders(
                    folderId = message.folderId,
                    messageServerId = message.messageServerId,
                    headerNames = setOf("List-Unsubscribe"),
                )
                .any { header -> header.name.equals("List-Unsubscribe", ignoreCase = true) }
        }.getOrDefault(false)
    }

    private fun buildOverview(entries: List<CachedCategory>): List<CategoryOverview> {
        val grouped = entries.groupBy { it.emailCategory }
        return EmailCategory.entries.map { category ->
            val categoryEntries = grouped[category].orEmpty().sortedByDescending { it.date }
            CategoryOverview(
                category = category,
                count = categoryEntries.size,
                samples = categoryEntries
                    .asSequence()
                    .filter { it.subject.isNotBlank() }
                    .take(SAMPLE_COUNT)
                    .toList(),
            )
        }
    }

    private companion object {
        const val MAX_MESSAGES_PER_ACCOUNT = 400
        const val MAX_LLM_MESSAGES_PER_SCAN = 12
        const val PROGRESS_INTERVAL = 5
        const val SAMPLE_COUNT = 3
    }
}

private data class ScannedMessage(
    val accountIdValue: AccountId,
    val accountId: String,
    val messageId: Long,
    val folderId: Long,
    val messageServerId: String,
    val subject: String,
    val sender: String,
    val date: Long,
) {
    val key: String
        get() = "$accountId:$messageId"
}

data class ScanProgress(
    val processed: Int,
    val total: Int,
    val ruleCount: Int,
    val languageModelCount: Int,
)

data class ScanSummary(
    val scannedMessages: Int,
    val classifiedByRules: Int,
    val classifiedByLanguageModel: Int,
    val cacheHits: Int,
    val modelAvailable: Boolean,
    val overview: List<CategoryOverview>,
)

data class CategoryOverview(
    val category: EmailCategory,
    val count: Int,
    val samples: List<CachedCategory>,
)
