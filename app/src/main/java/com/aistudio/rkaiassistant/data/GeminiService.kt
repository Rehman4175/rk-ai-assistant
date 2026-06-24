package com.aistudio.rkaiassistant.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import java.net.UnknownHostException
import java.net.SocketTimeoutException

// ===== DATA MODELS =====

@JsonClass(generateAdapter = true)
data class Part(val text: String? = null, val inlineData: InlineData? = null)

@JsonClass(generateAdapter = true)
data class InlineData(val mimeType: String, val data: String)

@JsonClass(generateAdapter = true)
data class Content(val role: String = "user", val parts: List<Part>)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = 0.7f,
    val maxOutputTokens: Int? = 2048,
    val topP: Float? = 0.95f
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(val content: Content)

@JsonClass(generateAdapter = true)
data class GeminiResponse(val candidates: List<Candidate>?)

// ===== API INTERFACES =====

interface GeminiApi {
    @POST("v1beta/models/gemini-2.0-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): retrofit2.Response<GeminiResponse>
}


// ===== SERVICE OBJECTS =====

object GeminiService {
    private var geminiApiKey: String = ""

    fun initialize(apiKey: String) {
        geminiApiKey = apiKey
    }

    private val moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    private val geminiApi by lazy {
        retrofit.create(GeminiApi::class.java)
    }
    

    fun isApiKeyConfigured(): Boolean {
        return geminiApiKey.isNotBlank() && geminiApiKey != "MY_GEMINI_API_KEY" && geminiApiKey != "YOUR_GEMINI_API_KEY"
    }

    suspend fun chat(
        prompt: String,
        systemInstruction: String = "You are RK, a smart and friendly personal AI assistant. Be helpful, concise and conversational.",
        chatHistory: List<ChatMessage> = emptyList(),
        onError: ((String) -> Unit)? = null
    ): String? {
        if (!isApiKeyConfigured()) {
            onError?.invoke("API Key not configured.")
            return null
        }

        val contentsList = mutableListOf<Content>()
        val historyToUse = chatHistory.takeLast(10)
        historyToUse.forEach { msg ->
            val role = if (msg.sender == "User") "user" else "model"
            contentsList.add(Content(role = role, parts = listOf(Part(text = msg.text))))
        }
        contentsList.add(Content(role = "user", parts = listOf(Part(text = prompt))))

        val request = GeminiRequest(
            contents = contentsList,
            systemInstruction = Content(role = "user", parts = listOf(Part(text = systemInstruction))),
            generationConfig = GenerationConfig()
        )

        return try {
            val response = geminiApi.generateContent(geminiApiKey, request)

            if (response.isSuccessful) {
                response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "No response from AI."
            } else {
                val errorMsg = when (response.code()) {
                    429 -> "Maaf kijiyega Boss, API limit khatam ho gayi hai. (429 Error)"
                    401, 403 -> "API Key sahi nahi hai ya expire ho gayi hai. (401/403 Error)"
                    500, 503 -> "Google servers mein takleef hai. (500/503 Error)"
                    else -> "Anjaan error aaya hai. (Code: ${response.code()})"
                }
                onError?.invoke(errorMsg)
                null
            }
        } catch (e: UnknownHostException) {
            onError?.invoke("Internet connection nahi hai.")
            null
        } catch (e: SocketTimeoutException) {
            onError?.invoke("Request timed out.")
            null
        } catch (e: Exception) {
            onError?.invoke("Kuch gadbad ho gayi: ${e.localizedMessage}")
            null
        }
    }

    suspend fun performOcr(
        base64Image: String,
        mimeType: String = "image/jpeg"
    ): String {
        if (!isApiKeyConfigured()) {
            return "⚠️ OCR unavailable — configure GEMINI_API_KEY first."
        }

        if (base64Image.isEmpty() || base64Image.length < 100) {
            return "❌ Invalid image data. Please capture a valid image."
        }

        val request = GeminiRequest(
            contents = listOf(
                Content(
                    role = "user",
                    parts = listOf(
                        Part(text = "Extract all text from this image accurately. Return only the transcribed text, no extra commentary."),
                        Part(inlineData = InlineData(mimeType = mimeType, data = base64Image))
                    )
                )
            ),
            generationConfig = GenerationConfig(temperature = 0.1f, maxOutputTokens = 2000)
        )

        return try {
            val response = geminiApi.generateContent(geminiApiKey, request)
            when {
                response.isSuccessful -> {
                    response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "No text detected in image."
                }
                response.code() == 429 -> "⏳ Rate limit exceeded. Please wait and try again."
                else -> "⚠️ OCR Error ${response.code()}: ${response.message()}"
            }
        } catch (e: Exception) {
            when {
                e is UnknownHostException -> "📶 No internet connection for OCR."
                e is SocketTimeoutException -> "⏱️ OCR request timed out."
                else -> "❌ OCR failed: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    suspend fun transcribeAudio(
        base64Audio: String,
        mimeType: String = "audio/mp4"
    ): String {
        if (!isApiKeyConfigured()) {
            return "⚠️ Transcription unavailable — configure GEMINI_API_KEY first."
        }

        if (base64Audio.isEmpty() || base64Audio.length < 100) {
            return "❌ Invalid audio data. Please record a valid audio clip."
        }

        val request = GeminiRequest(
            contents = listOf(
                Content(
                    role = "user",
                    parts = listOf(
                        Part(text = "Transcribe this audio clip accurately. Return only the spoken text, no extra commentary."),
                        Part(inlineData = InlineData(mimeType = mimeType, data = base64Audio))
                    )
                )
            ),
            generationConfig = GenerationConfig(temperature = 0.1f, maxOutputTokens = 1500)
        )

        return try {
            val response = geminiApi.generateContent(geminiApiKey, request)
            when {
                response.isSuccessful -> {
                    response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "No speech recognized."
                }
                response.code() == 429 -> "⏳ Rate limit exceeded. Please wait."
                else -> "⚠️ Transcription Error ${response.code()}"
            }
        } catch (e: Exception) {
            when {
                e is UnknownHostException -> "📶 No internet connection for transcription."
                e is SocketTimeoutException -> "⏱️ Transcription timed out."
                else -> "❌ Transcription failed: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }


    suspend fun parseNaturalLanguage(
        userInput: String,
        parseType: String // "task", "reminder", "expense", "event"
    ): String {
        if (!isApiKeyConfigured()) return ""

        val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val nowTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date())

        val systemPrompt = when (parseType) {
            "task" -> """Extract task details from this text. Return ONLY valid JSON:
{"title":"task title","priority":"High|Medium|Low","dueDate":"$todayDate or YYYY-MM-DD","notes":"any extra notes"}"""
            "reminder" -> """Extract reminder from text. Today is $todayDate, time is $nowTime. Return ONLY valid JSON:
{"title":"reminder title","delayMinutes":30,"recurrence":"None|Daily|Weekly"}"""
            "expense" -> """Extract expense from text. Return ONLY valid JSON:
{"amount":100.0,"title":"item name","category":"Food|Fuel|Bill|Salary|Rent|Shopping|Health|Other","isIncome":false}"""
            "event" -> """Extract calendar event from text. Today is $todayDate. Return ONLY valid JSON:
{"title":"event title","dateString":"$todayDate or YYYY-MM-DD","timeString":"$nowTime or HH:MM","location":"","type":"Meeting|Birthday|Holiday|Custom"}"""
            else -> return ""
        }

        return try {
            val request = GeminiRequest(
                contents = listOf(Content(role = "user", parts = listOf(Part(text = userInput)))),
                systemInstruction = Content(role = "user", parts = listOf(Part(text = systemPrompt))),
                generationConfig = GenerationConfig(temperature = 0.1f, maxOutputTokens = 200)
            )
            val response = geminiApi.generateContent(geminiApiKey, request)
            if (response.isSuccessful) {
                response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}



object GoogleSheetsService {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun sync(jsonPayload: String, scriptUrl: String): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val mediaType = "application/json".toMediaType()
            var currentUrl = scriptUrl
            var redirectCount = 0
            val maxRedirects = 5

            try {
                val client = okHttpClient.newBuilder()
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .build()

                var response: okhttp3.Response? = null

                while (redirectCount < maxRedirects) {
                    val request = okhttp3.Request.Builder()
                        .url(currentUrl)
                        .header("User-Agent", "RK-AI-Assistant-Android")
                        .post(jsonPayload.toRequestBody(mediaType))
                        .build()

                    val nextResponse = client.newCall(request).execute()
                    
                    if (nextResponse.code in 300..308) {
                        val location = nextResponse.header("Location")
                        nextResponse.close()
                        if (location != null) {
                            currentUrl = if (location.startsWith("http")) {
                                location
                            } else {
                                currentUrl.toHttpUrl().resolve(location)?.toString() ?: location
                            }
                            redirectCount++
                            continue
                        }
                    }
                    
                    response = nextResponse
                    break
                }

                if (response == null) return@withContext false

                val responseBody = response.body?.string() ?: ""
                val code = response.code
                response.close()

                code == 200 && responseBody.contains("Success", ignoreCase = true)
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
