package com.fsck.k9.ui.localai

import android.content.Context
import android.net.Uri
import android.text.format.Formatter
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Owns the optional MediaPipe `.task` model file.
 *
 * The model is never bundled with the APK. The user imports it with the system document picker, then it is copied
 * into the app's private storage. Nothing is downloaded from the network.
 */
class LocalModelManager(private val context: Context) {
    val modelsDirectory: File
        get() = File(context.filesDir, MODELS_DIRECTORY_NAME)

    val modelFile: File
        get() = File(modelsDirectory, MODEL_FILE_NAME)

    fun hasModel(): Boolean = modelFile.isFile && modelFile.length() > 0L

    fun sizeBytes(): Long = modelFile.takeIf { it.isFile }?.length() ?: 0L

    fun formattedSize(): String = Formatter.formatShortFileSize(context, sizeBytes())

    fun describeStatus(): String {
        return if (hasModel()) {
            context.getString(
                com.fsck.k9.ui.R.string.local_ai_model_status_ready,
                modelFile.absolutePath,
                formattedSize(),
            )
        } else {
            context.getString(com.fsck.k9.ui.R.string.local_ai_model_status_none)
        }
    }

    /**
     * Copy the document selected by the user into private app storage.
     *
     * The copy is written to a temporary file first so an interrupted import never leaves a half written model behind.
     */
    suspend fun importModel(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        try {
            modelsDirectory.mkdirs()
            if (!modelsDirectory.isDirectory) {
                return@withContext ImportResult.Failure("Could not create the model directory")
            }

            val destination = modelFile
            val temporaryFile = File(modelsDirectory, "$MODEL_FILE_NAME.part")
            if (temporaryFile.exists()) {
                temporaryFile.delete()
            }

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext ImportResult.Failure("Could not open the selected file")
            inputStream.use { input ->
                temporaryFile.outputStream().buffered(BUFFER_SIZE).use { output ->
                    input.copyTo(output, BUFFER_SIZE)
                }
            }

            if (temporaryFile.length() <= 0L) {
                temporaryFile.delete()
                return@withContext ImportResult.Failure("The selected file is empty")
            }

            if (destination.exists()) {
                destination.delete()
            }
            if (!temporaryFile.renameTo(destination)) {
                temporaryFile.delete()
                return@withContext ImportResult.Failure("Could not move the model into place")
            }

            ImportResult.Success(destination.length())
        } catch (e: Exception) {
            ImportResult.Failure(e.message ?: "Could not import the model")
        }
    }

    private companion object {
        const val MODELS_DIRECTORY_NAME = "models"
        const val MODEL_FILE_NAME = "gemma3-1b-it-int4.task"
        const val BUFFER_SIZE = 1024 * 1024
    }
}

sealed interface ImportResult {
    data class Success(val sizeBytes: Long) : ImportResult
    data class Failure(val message: String) : ImportResult
}
