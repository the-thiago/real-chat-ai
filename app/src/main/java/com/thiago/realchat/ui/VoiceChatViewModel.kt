package com.thiago.realchat.ui

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thiago.realchat.data.VoiceChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VoiceChatViewModel(private val repository: VoiceChatRepository = VoiceChatRepository()) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceChatUiState())
    val uiState: StateFlow<VoiceChatUiState> = _uiState

    private var loopJob: kotlinx.coroutines.Job? = null
    private val RECORD_DURATION_MS = 5000L

    fun startConversation(context: Context) {
        if (loopJob != null) return // already running
        loopJob = viewModelScope.launch {
            while (true) {
                // 1. Listen
                _uiState.value = _uiState.value.copy(isRecording = true, isThinking = false)
                repository.startRecording(context)
                kotlinx.coroutines.delay(RECORD_DURATION_MS)

                _uiState.value = _uiState.value.copy(isRecording = false, isThinking = true)
                val audioFile = repository.stopRecording()

                if (audioFile != null) {
                    // 2. Send to OpenAI (transcribe, chat, tts)
                    val (userText, aiText, aiAudio) = repository.processUserAudio(context, audioFile)
                    addMessage(userText, Sender.USER)
                    addMessage(aiText, Sender.AI)

                    // 3. Speak
                    aiAudio?.let { playAudioSuspending(context, it) }
                }

                _uiState.value = _uiState.value.copy(isThinking = false)
            }
        }
    }

    private fun addMessage(text: String, sender: Sender) {
        _uiState.value = _uiState.value.copy(messages = listOf(ChatMessage(text, sender)) + _uiState.value.messages)
    }

    private suspend fun playAudioSuspending(context: Context, filePath: String) {
        return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            try {
                val player = MediaPlayer().apply {
                    setDataSource(filePath)
                    prepare()
                    start()
                }
                player.setOnCompletionListener {
                    it.release()
                    if (cont.isActive) cont.resume(Unit) {}
                }
                cont.invokeOnCancellation {
                    try { player.stop() } catch (_: Exception) {}
                    player.release()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (cont.isActive) cont.resume(Unit) {}
            }
        }
    }
}

data class VoiceChatUiState(
    val isRecording: Boolean = false,
    val isThinking: Boolean = false,
    val messages: List<ChatMessage> = emptyList()
)

data class ChatMessage(val text: String, val sender: Sender)

enum class Sender { USER, AI } 