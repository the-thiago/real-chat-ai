package com.thiago.realchat.data

import android.content.Context
import android.media.MediaRecorder
import android.os.Environment
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.*

class VoiceChatRepository(private val service: OpenAIService = OpenAIService.create()) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(context: Context) {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.cacheDir
        if (!dir.exists()) dir.mkdirs()
        outputFile = File(dir, "record_${System.currentTimeMillis()}.m4a")

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile?.absolutePath)
            prepare()
            start()
        }
    }

    fun stopRecording(): File? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Pipeline: audio file -> transcription -> chat -> speech audio file path
     * Returns Triple<userText, aiText, aiAudioFilePath>
     */
    suspend fun processUserAudio(context: Context, audioFile: File): Triple<String, String, String?> {
        return withContext(Dispatchers.IO) {
            // 1. Transcribe user audio (Whisper)
            val transcription = transcribe(audioFile)
            // 2. Chat completion
            val aiText = chat(transcription)
            // 3. Convert AI text to speech
            val audioPath = textToSpeech(context, aiText)
            Triple(transcription, aiText, audioPath)
        }
    }

    private suspend fun transcribe(audioFile: File): String {
        val mediaType = "audio/mp4".toMediaType()
        val body = audioFile.asRequestBody(mediaType)
        val part = MultipartBody.Part.createFormData("file", audioFile.name, body)
        val modelBody = "whisper-1".toRequestBody("text/plain".toMediaType())
        val langBody = "en".toRequestBody("text/plain".toMediaType())
        val response = service.transcribeAudio(part, modelBody, langBody)
        return response.body()?.text ?: ""
    }

    private suspend fun chat(userText: String): String {
        val response = service.chatCompletion(ChatRequest(messages = listOf(ChatMessageDto("user", userText))))
        return response.body()?.choices?.firstOrNull()?.message?.content ?: ""
    }

    private suspend fun textToSpeech(context: Context, text: String): String? {
        if (text.isBlank()) return null
        val request = SpeechRequest(input = text, model = "tts-1", voice = "alloy", format = "mp3")
        val responseBody = service.createSpeech(request).body() ?: return null
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.cacheDir
        if (!dir.exists()) dir.mkdirs()
        val audioFile = File(dir, "ai_${UUID.randomUUID()}.mp3")
        audioFile.outputStream().use { output ->
            responseBody.byteStream().copyTo(output)
        }
        return audioFile.absolutePath
    }
} 