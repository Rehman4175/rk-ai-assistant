package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLException

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
    ): retrofit2.Response<GeminiResponse>  // ✅ FIX: Using Response wrapper for proper error handling
}

interface WeatherApi {
    @retrofit2.http.GET("data/2.5/weather")
    suspend fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): retrofit2.Response<okhttp3.ResponseBody>
}

// ===== SERVICE OBJECTS =====

object GeminiService {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // ✅ FIX: Added more robust OkHttpClient with retry mechanism
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)  // Reduced from 60 to 30
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)  // ✅ FIX: Auto retry on connection failure
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val geminiApi = retrofit.create(GeminiApi::class.java)
    private val weatherRetrofit = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/")
        .client(okHttpClient)
        .build()
    private val weatherApi = weatherRetrofit.create(WeatherApi::class.java)

    // ✅ FIX: Better API key check
    fun isApiKeyConfigured(): Boolean {
        val key = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY" && key != "YOUR_GEMINI_API_KEY"
    }

    suspend fun chat(
        prompt: String,
        systemInstruction: String = "You are RK, a smart and friendly personal AI assistant. Be helpful, concise and conversational.",
        chatHistory: List<ChatMessage> = emptyList()
    ): String? {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (!isApiKeyConfigured()) {
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
            val response = geminiApi.generateContent(apiKey, request)

            // ✅ FIX: Proper response handling
            when {
                response.isSuccessful -> {
                    response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "No response from AI."
                }
                response.code() == 429 -> {
                    // ✅ FIX: Better 429 error message with retry suggestion
                    "Maaf kijiyega Boss, API limit khatam ho gayi hai. Thodi der baad try karein (429 Error)."
                }
                response.code() == 401 || response.code() == 403 -> {
                    "API Key sahi nahi hai ya expire ho gayi hai (401/403 Error)."
                }
                response.code() == 500 || response.code() == 503 -> {
                    "Google ke servers mein kuch takleef hai, thodi der mein theek ho jayega (500/503 Error)."
                }
                else -> {
                    "Kuch anjaan error aaya hai (Code: ${response.code()})."
                }
            }
        } catch (e: UnknownHostException) {
            null
        } catch (e: SocketTimeoutException) {
            null
        } catch (e: SSLException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * ✅ FIX: Improved OCR with better error handling
     */
    suspend fun performOcr(
        base64Image: String,
        mimeType: String = "image/jpeg"
    ): String {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (!isApiKeyConfigured()) {
            return "⚠️ OCR unavailable — configure GEMINI_API_KEY first."
        }

        // ✅ FIX: Validate base64 image
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
            val response = geminiApi.generateContent(apiKey, request)
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

    /**
     * ✅ FIX: Improved audio transcription
     */
    suspend fun transcribeAudio(
        base64Audio: String,
        mimeType: String = "audio/mp4"
    ): String {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
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
            val response = geminiApi.generateContent(apiKey, request)
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

    /**
     * ✅ FIX: Improved smart parsing
     */
    /**
     * ✅ FIX: Real-time Weather Integration
     */
    suspend fun fetchWeather(lat: Double, lon: Double, apiKey: String): String {
        return try {
            val response = weatherApi.getWeather(lat, lon, apiKey)
            if (response.isSuccessful) {
                val body = response.body()?.string() ?: ""
                val json = org.json.JSONObject(body)
                val main = json.getJSONObject("main")
                val temp = main.getDouble("temp").toInt()
                val weatherArray = json.getJSONArray("weather")
                val desc = if (weatherArray.length() > 0) weatherArray.getJSONObject(0).getString("description") else "Clear"
                "$temp°C - ${desc.replaceFirstChar { it.uppercase() }}"
            } else {
                "Weather unavailable"
            }
        } catch (e: Exception) {
            "Weather Error"
        }
    }

    suspend fun parseNaturalLanguage(
        userInput: String,
        parseType: String // "task", "reminder", "expense", "event"
    ): String {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
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
            val response = geminiApi.generateContent(apiKey, request)
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
            try {
                val mediaType = "application/json".toMediaType()
                val body = jsonPayload.toRequestBody(mediaType)
                
                // Google Apps Script usually requires following redirects correctly.
                val request = okhttp3.Request.Builder()
                    .url(scriptUrl)
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:100.0) Gecko/100.0 Firefox/100.0")
                    .header("Accept", "application/json")
                    .post(body)
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    println("Sync Response Code: ${response.code}")
                    println("Sync Response Body: $responseBody")
                    
                    // Google Apps Script success could be 200 or 302/301/307 (redirects)
                    // With followRedirects(true), we expect 200 OK.
                    // We also check if the body contains "Success" as defined in our GAS code.
                    val isSuccess = response.isSuccessful || 
                                    response.code in 300..308 || 
                                    responseBody.contains("Success", ignoreCase = true)
                    
                    isSuccess
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("Sync Error: ${e.message}")
                false
            }
        }
    }
}