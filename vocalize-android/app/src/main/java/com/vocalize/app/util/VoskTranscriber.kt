package com.vocalize.app.util

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoskTranscriber @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var model: Model? = null
    private val modelsDir get() = File(context.filesDir, Constants.MODELS_DIR)
    private val modelDir get() = File(modelsDir, Constants.VOSK_MODEL_DIR)

    val isModelAvailable: Boolean get() = modelDir.exists() && modelDir.listFiles()?.isNotEmpty() == true

    fun initModel(): Boolean {
        return try {
            if (!isModelAvailable) return false
            model = Model(modelDir.absolutePath)
            true
        } catch (e: Exception) {
            Log.e("VoskTranscriber", "Failed to init model", e)
            false
        }
    }

    fun transcribeFile(filePath: String): String {
        val currentModel = model ?: run {
            if (!initModel()) return ""
            model ?: return ""
        }

        return try {
            val recognizer = Recognizer(currentModel, 44100.0f)
            val buf = ByteArray(4096)
            val result = StringBuilder()

            FileInputStream(filePath).use { stream ->
                var nread: Int
                while (stream.read(buf).also { nread = it } >= 0) {
                    if (recognizer.acceptWaveForm(buf, nread)) {
                        val partial = recognizer.result
                        result.append(parseVoskResult(partial))
                    }
                }
                result.append(parseVoskResult(recognizer.finalResult))
            }
            recognizer.close()
            result.toString().trim()
        } catch (e: Exception) {
            Log.e("VoskTranscriber", "Transcription failed", e)
            ""
        }
    }

    fun downloadModel(onProgress: (Int) -> Unit, onComplete: (Boolean) -> Unit) {
        modelsDir.mkdirs()
        StorageService.unpack(
            context,
            "vosk-model-small-en-us-0.15.zip",
            Constants.MODELS_DIR,
            { model ->
                this.model = model
                onComplete(true)
            },
            { exception ->
                Log.e("VoskTranscriber", "Download failed", exception)
                onComplete(false)
            }
        )
    }

    fun deleteModel() {
        model?.close()
        model = null
        modelDir.deleteRecursively()
    }

    private fun parseVoskResult(json: String): String {
        return try {
            val match = Regex("\"text\"\\s*:\\s*\"([^\"]+)\"").find(json)
            match?.groupValues?.getOrNull(1)?.let { "$it " } ?: ""
        } catch (e: Exception) { "" }
    }
}
