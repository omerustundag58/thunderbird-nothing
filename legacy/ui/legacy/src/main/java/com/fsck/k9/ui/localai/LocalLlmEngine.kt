package com.fsck.k9.ui.localai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Thin wrapper around the MediaPipe LLM Inference task.
 *
 * Creating the engine and generating text both happen on [Dispatchers.IO]. The model lives in the app's files
 * directory, so no asset extraction is needed.
 *
 * MediaPipe fixes the model configuration when the engine is created. Sampling parameters (temperature / top-k) live
 * on a per-request session, which is why [generate] gets a fresh session for every prompt.
 */
class LocalLlmEngine(private val context: Context) {
    private val mutex = Mutex()

    @Volatile
    private var inference: LlmInference? = null

    @Volatile
    private var loadedModelPath: String? = null

    val isReady: Boolean
        get() = inference != null

    /**
     * Load [modelFile] if it isn't already loaded. Returns `true` when the engine is ready to generate.
     */
    suspend fun initialize(modelFile: File): Boolean = mutex.withLock {
        val absolutePath = modelFile.absolutePath
        if (inference != null && loadedModelPath == absolutePath) {
            return@withLock true
        }

        closeInternal()

        withContext(Dispatchers.IO) {
            try {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(absolutePath)
                    .setMaxTokens(MAX_TOKENS)
                    .setMaxTopK(1)
                    .build()

                inference = LlmInference.createFromOptions(context, options)
                loadedModelPath = absolutePath
                true
            } catch (e: Throwable) {
                inference = null
                loadedModelPath = null
                false
            }
        }
    }

    /**
     * Generate a completion. Returns `null` when no model is loaded or generation fails.
     *
     * Never call this from the main thread; this function switches to [Dispatchers.IO] itself.
     */
    suspend fun generate(
        prompt: String,
        maxTokens: Int = MAX_TOKENS,
        temperature: Float = DEFAULT_TEMPERATURE,
    ): String? = mutex.withLock {
        if (maxTokens <= 0) return@withLock null

        val engine = inference ?: return@withLock null
        withContext(Dispatchers.IO) {
            var session: LlmInferenceSession? = null
            try {
                val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTopK(1)
                    .setTopP(1.0f)
                    .setTemperature(temperature.coerceIn(0f, 1f))
                    .setRandomSeed(RANDOM_SEED)
                    .build()

                session = LlmInferenceSession.createFromOptions(engine, sessionOptions)
                session.addQueryChunk(prompt)
                session.generateResponse().trim().ifEmpty { null }
            } catch (e: Throwable) {
                null
            } finally {
                runCatching { session?.close() }
            }
        }
    }

    suspend fun close() {
        mutex.withLock {
            closeInternal()
        }
    }

    private fun closeInternal() {
        val engine = inference
        inference = null
        loadedModelPath = null
        runCatching { engine?.close() }
    }

    private companion object {
        // MediaPipe treats this as the total input+output budget. The prompt asks for a single word, so this is
        // comfortably above the prompt size while still capping runaway generations.
        const val MAX_TOKENS = 256
        const val DEFAULT_TEMPERATURE = 0.1f
        const val RANDOM_SEED = 101
    }
}
