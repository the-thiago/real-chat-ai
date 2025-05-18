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

    fun onMicButtonClicked(context: Context) {
        val state = _uiState.value
        if (state.isRecording) {
            stopRecording(context)
        } else {
            startRecording(context)
        }
    }

    private fun startRecording(context: Context) {
        _uiState.value = _uiState.value.copy(isRecording = true)
        repository.startRecording(context)
    }

    private fun stopRecording(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRecording = false)
            val audioFile = repository.stopRecording()
            if (audioFile != null) {
                // Transcribe -> Chat -> Speech
                val (userText, aiText, aiAudio) = repository.processUserAudio(context, audioFile)
                addMessage(userText, Sender.USER)
                addMessage(aiText, Sender.AI)
                // Play audio reply
                aiAudio?.let {
                    playAudio(context, it)
                }
            }
        }
    }

    private fun addMessage(text: String, sender: Sender) {
        _uiState.value = _uiState.value.copy(messages = listOf(ChatMessage(text, sender)) + _uiState.value.messages)
    }

    private fun playAudio(context: Context, filePath: String) {
        try {
            val player = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                start()
            }
            player.setOnCompletionListener { player.release() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

data class VoiceChatUiState(
    val isRecording: Boolean = false,
    val messages: List<ChatMessage> = emptyList()
)

data class ChatMessage(val text: String, val sender: Sender)

enum class Sender { USER, AI } 