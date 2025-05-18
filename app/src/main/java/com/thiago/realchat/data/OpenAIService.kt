package com.thiago.realchat.data

import com.thiago.realchat.BuildConfig
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface OpenAIService {
    @Multipart
    @POST("v1/audio/transcriptions")
    suspend fun transcribeAudio(
        @Part file: MultipartBody.Part,
        @Part("model") model: String,
        @Part("language") language: String = "en"
    ): Response<TranscriptionResponse>

    @POST("v1/chat/completions")
    suspend fun chatCompletion(@Body request: ChatRequest): Response<ChatResponse>

    @POST("v1/audio/speech")
    suspend fun createSpeech(@Body request: SpeechRequest): Response<okhttp3.ResponseBody>

    companion object {
        fun create(): OpenAIService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
                        .build()
                    chain.proceed(request)
                }
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.openai.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(OpenAIService::class.java)
        }
    }
}

// ------ DTOs ------

data class TranscriptionResponse(
    val text: String
)

data class ChatRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<ChatMessageDto>
)

data class ChatMessageDto(
    val role: String,
    val content: String
)

data class ChatResponse(
    val choices: List<ChatChoice>
)

data class ChatChoice(
    val message: ChatMessageDto
)

data class SpeechRequest(
    val model: String,
    val input: String,
    val voice: String = "alloy",
    val format: String = "mp3"
) 