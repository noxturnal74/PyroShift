package com.example.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.theme.PyroAmber
import com.example.ui.theme.PyroOrange

@Composable
fun AiChatScreen(
  onOpenWebUrl: (String) -> Unit,
  viewModel: AiChatViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val listState = rememberLazyListState()
  var inputText by rememberSaveable { mutableStateOf("") }

  LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.text?.length) {
    if (uiState.messages.isNotEmpty()) {
      listState.animateScrollToItem(uiState.messages.lastIndex)
    }
  }

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      shadowElevation = 2.dp,
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Rounded.TravelExplore,
            contentDescription = null,
            tint = PyroOrange,
            modifier = Modifier.size(22.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.ai_header_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
          )
          IconButton(
            onClick = viewModel::clearConversation,
            enabled = uiState.messages.isNotEmpty() && !uiState.isGenerating,
            modifier = Modifier.size(48.dp).testTag("ai_clear_chat_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.DeleteOutline,
              contentDescription = stringResource(R.string.cd_clear_chat),
            )
          }
        }

        Surface(
          color = PyroOrange.copy(alpha = 0.14f),
          shape = RoundedCornerShape(999.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.VerifiedUser,
              contentDescription = null,
              tint = PyroOrange,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.ai_scope_badge),
              style = MaterialTheme.typography.labelSmall,
              color = PyroOrange,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }

    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      if (uiState.messages.isEmpty()) {
        AiEmptyState(onSelectPrompt = viewModel::sendMessage)
      } else {
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize().testTag("ai_chat_list"),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(uiState.messages, key = { it.id }) { msg ->
            ChatBubble(
              message = msg,
              onOpenSource = { source -> onOpenWebUrl(source.uri) },
            )
          }
        }
      }
    }

    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 4.dp,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = inputText,
          onValueChange = { inputText = it },
          placeholder = { Text(stringResource(R.string.ai_input_hint)) },
          modifier = Modifier.weight(1f).testTag("ai_chat_input"),
          maxLines = 4,
          shape = RoundedCornerShape(24.dp),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
          keyboardActions =
            KeyboardActions(
              onSend = {
                if (inputText.isNotBlank() && !uiState.isGenerating) {
                  val text = inputText
                  inputText = ""
                  viewModel.sendMessage(text)
                }
              }
            ),
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
          onClick = {
            if (inputText.isNotBlank() && !uiState.isGenerating) {
              val text = inputText
              inputText = ""
              viewModel.sendMessage(text)
            }
          },
          enabled = inputText.isNotBlank() && !uiState.isGenerating,
          modifier = Modifier.size(48.dp).clip(CircleShape).testTag("ai_send_button"),
          colors =
            IconButtonDefaults.iconButtonColors(
              containerColor = PyroOrange,
              contentColor = Color.White,
              disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
          if (uiState.isGenerating) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.dp,
              color = Color.White,
            )
          } else {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.Send,
              contentDescription = stringResource(R.string.cd_send_message),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AiEmptyState(
  onSelectPrompt: (String) -> Unit,
) {
  val suggestions =
    listOf(
      "Apa itu projek PyroShift di pyroshift.vercel.app?",
      "Apa saja fitur navigasi dan layar penuh di APK PyroShift ini?",
      "Bagaimana cara mengatasi kendala koneksi saat membuka pyroshift.vercel.app?",
    )

  Column(
    modifier = Modifier.fillMaxSize().padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = Icons.Rounded.TravelExplore,
      contentDescription = null,
      tint = PyroOrange,
      modifier = Modifier.size(48.dp),
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
      text = stringResource(R.string.ai_empty_title),
      style = MaterialTheme.typography.titleLarge,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = stringResource(R.string.ai_empty_subtitle),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(20.dp))
    suggestions.forEach { prompt ->
      AssistChip(
        onClick = { onSelectPrompt(prompt) },
        label = { Text(prompt, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        modifier = Modifier.padding(vertical = 4.dp),
      )
    }
  }
}

@Composable
private fun ChatBubble(
  message: ChatMessageItem,
  onOpenSource: (GroundedSource) -> Unit,
) {
  val alignment = if (message.isUser) Alignment.End else Alignment.Start
  val bubbleColor =
    when {
      message.isError -> MaterialTheme.colorScheme.errorContainer
      message.isUser -> PyroOrange
      else -> MaterialTheme.colorScheme.surfaceVariant
    }
  val textColor =
    when {
      message.isError -> MaterialTheme.colorScheme.onErrorContainer
      message.isUser -> Color.White
      else -> MaterialTheme.colorScheme.onSurface
    }

  Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
    Surface(
      color = bubbleColor,
      shape =
        RoundedCornerShape(
          topStart = 18.dp,
          topEnd = 18.dp,
          bottomStart = if (message.isUser) 18.dp else 4.dp,
          bottomEnd = if (message.isUser) 4.dp else 18.dp,
        ),
      modifier = Modifier.widthIn(max = 520.dp),
    ) {
      Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        if (message.text.isEmpty() && message.isStreaming) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp,
              color = PyroOrange,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Searching & thinking…",
              style = MaterialTheme.typography.bodyMedium,
              color = textColor,
            )
          }
        } else {
          Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
          )
        }

        if (message.searchQueries.isNotEmpty()) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = stringResource(R.string.ai_queries_label),
            style = MaterialTheme.typography.labelSmall,
            color = PyroAmber,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            message.searchQueries.forEach { query ->
              Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(999.dp),
              ) {
                Text(
                  text = query,
                  style = MaterialTheme.typography.labelSmall,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }
          }
        }

        if (message.sources.isNotEmpty()) {
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = stringResource(R.string.ai_sources_label),
            style = MaterialTheme.typography.labelSmall,
            color = PyroOrange,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            message.sources.take(6).forEach { source ->
              Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenSource(source) },
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Public,
                    contentDescription = null,
                    tint = PyroOrange,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = source.title,
                      style = MaterialTheme.typography.labelLarge,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = source.domain,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
