package com.fsck.k9.ui.localai

import android.content.Context
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistent category cache.
 *
 * Deliberately not a database migration: this is a single JSON file in the app's private files directory keyed by
 * `accountId:messageId`. It stores just enough metadata to render the Categories screen without re-reading mail.
 */
class CategoryStore(context: Context) {
    private val file = File(context.filesDir, "local_ai/categories.json")
    private val lock = Any()

    private var entries: MutableMap<String, CachedCategory>? = null

    fun get(accountId: String, messageId: Long): CachedCategory? {
        return synchronized(lock) {
            loadLocked()[keyOf(accountId, messageId)]
        }
    }

    fun getAll(): List<CachedCategory> {
        return synchronized(lock) {
            loadLocked().values.toList()
        }
    }

    fun put(entry: CachedCategory) {
        synchronized(lock) {
            val loaded = loadLocked()
            loaded[entry.key] = entry
            saveLocked(loaded)
        }
    }

    fun putAll(newEntries: Collection<CachedCategory>) {
        if (newEntries.isEmpty()) return

        synchronized(lock) {
            val loaded = loadLocked()
            newEntries.forEach { entry ->
                loaded[entry.key] = entry
            }
            saveLocked(loaded)
        }
    }

    fun clear() {
        synchronized(lock) {
            entries = mutableMapOf()
            if (file.exists()) {
                file.delete()
            }
            if (file.exists()) {
                saveLocked(emptyMap())
            }
        }
    }

    /**
     * Drops cached entries that were not part of the current scan, so deleted
     * messages and removed accounts do not keep subjects around forever.
     */
    fun pruneExcept(keys: Set<String>) {
        synchronized(lock) {
            val loaded = loadLocked()
            if (loaded.keys.retainAll(keys)) {
                saveLocked(loaded)
            }
        }
    }

    private fun loadLocked(): MutableMap<String, CachedCategory> {
        val current = entries
        if (current != null) return current

        val loaded = mutableMapOf<String, CachedCategory>()
        if (file.isFile) {
            runCatching {
                val json = JSONArray(file.readText())
                for (index in 0 until json.length()) {
                    val item = json.getJSONObject(index)
                    val entry = CachedCategory(
                        accountId = item.getString("accountId"),
                        messageId = item.getLong("messageId"),
                        category = item.getString("category"),
                        subject = item.optString("subject"),
                        sender = item.optString("sender"),
                        date = item.optLong("date"),
                        source = item.optString("source", ClassificationSource.RULES.name),
                    )
                    loaded[entry.key] = entry
                }
            }
        }

        entries = loaded
        return loaded
    }

    private fun saveLocked(newEntries: Map<String, CachedCategory>) {
        runCatching {
            file.parentFile?.mkdirs()
            val json = JSONArray()
            newEntries.values.forEach { entry ->
                json.put(
                    JSONObject()
                        .put("accountId", entry.accountId)
                        .put("messageId", entry.messageId)
                        .put("category", entry.category)
                        .put("subject", entry.subject)
                        .put("sender", entry.sender)
                        .put("date", entry.date)
                        .put("source", entry.source),
                )
            }

            val temporaryFile = File(file.parentFile, "${file.name}.tmp")
            temporaryFile.writeText(json.toString())
            if (file.exists()) {
                file.delete()
            }
            if (!temporaryFile.renameTo(file)) {
                temporaryFile.delete()
            }
        }
    }

    private fun keyOf(accountId: String, messageId: Long): String = "$accountId:$messageId"
}

data class CachedCategory(
    val accountId: String,
    val messageId: Long,
    val category: String,
    val subject: String,
    val sender: String,
    val date: Long,
    val source: String = ClassificationSource.RULES.name,
) {
    val key: String
        get() = "$accountId:$messageId"

    val emailCategory: EmailCategory
        get() = EmailCategory.fromApiName(category) ?: EmailCategory.OTHER
}
