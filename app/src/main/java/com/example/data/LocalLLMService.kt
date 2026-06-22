package com.example.data

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object LocalLLMService {
    private var llmInference: LlmInference? = null
    private var isInitialized = false

    const val MODEL_PATH = "qwen2.5-1.5b-q8.litertlm"
    const val MODEL_URL = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm"

    fun initialize(context: Context) {
        if (isInitialized) return

        // Check both internal and external storage
        val internalFile = File(context.filesDir, MODEL_PATH)
        val externalFile = File(context.getExternalFilesDir(null), MODEL_PATH)
        val oldInternalFile = File(context.filesDir, "qwen2.5-1.5b.litertlm")
        
        val modelFile = when {
            externalFile.exists() && externalFile.length() > 100 * 1024 * 1024 -> externalFile
            internalFile.exists() && internalFile.length() > 100 * 1024 * 1024 -> internalFile
            oldInternalFile.exists() && oldInternalFile.length() > 100 * 1024 * 1024 -> oldInternalFile
            else -> null
        }

        if (modelFile == null) {
            android.util.Log.e("RKAI", "Model file not found in internal or external storage")
            return
        }

        try {
            android.util.Log.d("RKAI", "Initializing LlmInference...")
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(1024)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            isInitialized = true
            android.util.Log.d("RKAI", "LlmInference initialized successfully")
        } catch (e: Throwable) {
            android.util.Log.e("RKAI", "LlmInference initialization failed: ${e.message}")
            isInitialized = false
            
            // If the model file is corrupted (invalid magic number), delete it so it can be re-downloaded
            if (e is Exception && e.message?.contains("Invalid magic number", ignoreCase = true) == true) {
                android.util.Log.e("RKAI", "Corrupted model detected. Deleting $modelFile")
                modelFile.delete()
            }
        }
    }

    suspend fun generateResponse(prompt: String, context: Context? = null): String = withContext(Dispatchers.IO) {
        if (!isInitialized || llmInference == null) {
            if (context != null) {
                try {
                    initialize(context)
                } catch (e: Exception) {
                    return@withContext "Model initialization failed: ${e.localizedMessage}"
                }
            }
            
            if (!isInitialized || llmInference == null) {
                return@withContext "Offline LLM Model not found. Please download the model file to use offline AI."
            }
        }

        try {
            val response = llmInference?.generateResponse(prompt)
            response ?: "No response from local model."
        } catch (e: Exception) {
            "Error in local AI: ${e.localizedMessage}"
        }
    }

    fun isModelAvailable(context: Context): Boolean {
        val internalFile = File(context.filesDir, MODEL_PATH)
        val externalFile = File(context.getExternalFilesDir(null), MODEL_PATH)
        val oldInternalFile = File(context.filesDir, "qwen2.5-1.5b.litertlm")
        
        return (externalFile.exists() && externalFile.length() > 100 * 1024 * 1024) ||
               (internalFile.exists() && internalFile.length() > 100 * 1024 * 1024) ||
               (oldInternalFile.exists() && oldInternalFile.length() > 100 * 1024 * 1024)
    }

    fun reset() {
        isInitialized = false
        llmInference?.close()
        llmInference = null
    }
}
