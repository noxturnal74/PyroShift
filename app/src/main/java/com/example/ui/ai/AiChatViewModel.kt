package com.example.ui.ai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

private const val TAG = "AiChatViewModel"

data class GroundedSource(
  val title: String,
  val uri: String,
  val domain: String,
)

data class ChatMessageItem(
  val id: String = UUID.randomUUID().toString(),
  val isUser: Boolean,
  val text: String,
  val isStreaming: Boolean = false,
  val isError: Boolean = false,
  val searchQueries: List<String> = emptyList(),
  val sources: List<GroundedSource> = emptyList(),
)

data class AiChatUiState(
  val messages: List<ChatMessageItem> = emptyList(),
  val isGenerating: Boolean = false,
)

class AiChatViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(AiChatUiState())
  val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

  private val projectSystemInstruction =
    """
    You are the official, strictly-scoped AI Assistant for the PyroShift project.

    PROJECT CONTEXT:
    - Web Application: PyroShift hosted at https://pyroshift.vercel.app/
    - Android Application: PyroShift APK built with Kotlin, Jetpack Compose, Material 3, Android WebView (JavaScript, DOM Storage, persistent Cookies, file upload/download support, fullscreen mode, share/open in browser, and offline error recovery), and Firebase AI Logic with Google Search Grounding.
    - Purpose of this Assistant: Help users understand, navigate, use, and troubleshoot the PyroShift web application (https://pyroshift.vercel.app/) and this PyroShift Android APK.

    STRICT SCOPE BOUNDARY (CRITICAL — NEVER VIOLATE):
    1. You MUST ONLY answer questions that are directly about the PyroShift project (https://pyroshift.vercel.app/, its features, pages, usage, troubleshooting, architecture, or this PyroShift Android app).
    2. When factual details about https://pyroshift.vercel.app/ or PyroShift are needed, use the Google Search grounding tool focused on "pyroshift.vercel.app" / "PyroShift".
    3. If the user asks about ANY topic outside the PyroShift project — including general knowledge, trivia, news, weather, recipes, math homework, politics, entertainment, other websites/apps, travel/places, or unrelated coding — you MUST REFUSE politely and concisely in the user's language (Indonesian or English), explaining that you can ONLY answer questions about the PyroShift project (https://pyroshift.vercel.app/ and this APK).
    4. Do NOT use Google Search for off-topic questions, and do NOT give even a partial answer to off-topic questions.
    """.trimIndent()

  private val model: GenerativeModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      tools = listOf(Tool.googleSearch()),
      systemInstruction = content { text(projectSystemInstruction) },
    )
  }

  private var activeChat: Chat? = null

  private fun getOrStartChat(): Chat {
    val existing = activeChat
    if (existing != null) return existing
    val newChat = model.startChat()
    activeChat = newChat
    return newChat
  }

  fun clearConversation() {
    if (_uiState.value.isGenerating) return
    activeChat = null
    _uiState.update { it.copy(messages = emptyList()) }
  }

  fun sendMessage(rawPrompt: String) {
    val prompt = rawPrompt.trim()
    if (prompt.isEmpty() || _uiState.value.isGenerating) return

    val userMessage = ChatMessageItem(isUser = true, text = prompt)
    val assistantId = UUID.randomUUID().toString()
    val placeholderMessage =
      ChatMessageItem(id = assistantId, isUser = false, text = "", isStreaming = true)

    _uiState.update {
      it.copy(
        messages = it.messages + userMessage + placeholderMessage,
        isGenerating = true,
      )
    }

    viewModelScope.launch {
      val accumulatedText = StringBuilder()
      val collectedQueries = linkedSetOf<String>()
      val collectedSources = linkedMapOf<String, GroundedSource>()

      try {
        val chat = getOrStartChat()
        withTimeout(60_000L) {
          chat.sendMessageStream(prompt).collect { chunk ->
            val piece = chunk.text.orEmpty()
            if (piece.isNotEmpty()) {
              accumulatedText.append(piece)
            }
            chunk.candidates.forEach { candidate ->
              val gm = candidate.groundingMetadata
              gm?.webSearchQueries?.forEach { q ->
                if (q.isNotBlank()) collectedQueries.add(q)
              }
              gm?.groundingChunks?.forEach { gc ->
                gc.web?.let { web ->
                  val uri = web.uri ?: return@let
                  val title = web.title?.takeIf { it.isNotBlank() } ?: web.domain ?: uri
                  val domain = web.domain?.takeIf { it.isNotBlank() } ?: "Web"
                  collectedSources[uri] =
                    GroundedSource(
                      title = title,
                      uri = uri,
                      domain = domain,
                    )
                }
              }
            }

            _uiState.update { state ->
              state.copy(
                messages =
                  state.messages.map { item ->
                    if (item.id == assistantId) {
                      item.copy(
                        text = accumulatedText.toString(),
                        searchQueries = collectedQueries.toList(),
                        sources = collectedSources.values.toList(),
                      )
                    } else {
                      item
                    }
                  }
              )
            }
          }
        }

        val finalText = accumulatedText.toString().trim()
        _uiState.update { state ->
          state.copy(
            isGenerating = false,
            messages =
              state.messages.map { item ->
                if (item.id == assistantId) {
                  if (finalText.isNotEmpty()) {
                    item.copy(
                      text = finalText,
                      isStreaming = false,
                      searchQueries = collectedQueries.toList(),
                      sources = collectedSources.values.toList(),
                    )
                  } else {
                    item.copy(
                      text = "Model returned an empty response.",
                      isStreaming = false,
                      isError = true,
                    )
                  }
                } else {
                  item
                }
              },
          )
        }
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Gemini request timed out", e)
        updateError(assistantId, "Request timed out after 60 seconds. Please try again.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Gemini request failed", e)
        updateError(assistantId, e.localizedMessage ?: "Failed to generate response.")
      }
    }
  }

  private fun updateError(assistantId: String, errorMessage: String) {
    _uiState.update { state ->
      state.copy(
        isGenerating = false,
        messages =
          state.messages.map { item ->
            if (item.id == assistantId) {
              item.copy(text = errorMessage, isStreaming = false, isError = true)
            } else {
              item
            }
          },
      )
    }
  }
}
