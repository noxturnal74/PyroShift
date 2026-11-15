package com.example.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class WebUiState(
  val homeUrl: String = DEFAULT_HOME_URL,
  val currentUrl: String = DEFAULT_HOME_URL,
  val pageTitle: String = "",
  val progress: Int = 0,
  val isLoading: Boolean = true,
  val isInitialLoad: Boolean = true,
  val canGoBack: Boolean = false,
  val canGoForward: Boolean = false,
  val hasError: Boolean = false,
  val isOfflineError: Boolean = false,
  val isFullscreen: Boolean = false,
) {
  companion object {
    const val DEFAULT_HOME_URL = "https://pyroshift.vercel.app/"
  }
}

class WebBrowserViewModel(application: Application) : AndroidViewModel(application) {
  private val prefs =
    application.getSharedPreferences("pyroshift_prefs", Context.MODE_PRIVATE)

  private val _uiState =
    MutableStateFlow(
      WebUiState(
        isFullscreen = prefs.getBoolean(KEY_FULLSCREEN, false),
        isOfflineError = !isNetworkAvailable(),
        hasError = !isNetworkAvailable(),
        isLoading = isNetworkAvailable(),
      )
    )
  val uiState: StateFlow<WebUiState> = _uiState.asStateFlow()

  fun onPageStarted(url: String?, canGoBack: Boolean, canGoForward: Boolean) {
    _uiState.update { state ->
      state.copy(
        currentUrl = url ?: state.currentUrl,
        isLoading = true,
        hasError = false,
        isOfflineError = false,
        canGoBack = canGoBack,
        canGoForward = canGoForward,
      )
    }
  }

  fun onProgressChanged(newProgress: Int) {
    _uiState.update { state ->
      val finishedInitial = state.isInitialLoad && newProgress >= 90
      state.copy(
        progress = newProgress.coerceIn(0, 100),
        isLoading = newProgress < 100,
        isInitialLoad = if (finishedInitial) false else state.isInitialLoad,
      )
    }
  }

  fun onPageFinished(
    url: String?,
    title: String?,
    canGoBack: Boolean,
    canGoForward: Boolean,
  ) {
    _uiState.update { state ->
      state.copy(
        currentUrl = url ?: state.currentUrl,
        pageTitle = title?.takeIf { it.isNotBlank() && !it.startsWith("http") } ?: state.pageTitle,
        progress = 100,
        isLoading = false,
        isInitialLoad = false,
        canGoBack = canGoBack,
        canGoForward = canGoForward,
      )
    }
  }

  fun onNavigationStateChanged(canGoBack: Boolean, canGoForward: Boolean) {
    _uiState.update { it.copy(canGoBack = canGoBack, canGoForward = canGoForward) }
  }

  fun onReceivedMainFrameError() {
    val offline = !isNetworkAvailable()
    _uiState.update {
      it.copy(
        isLoading = false,
        isInitialLoad = false,
        hasError = true,
        isOfflineError = offline,
      )
    }
  }

  fun onSplashTimeout() {
    _uiState.update { it.copy(isInitialLoad = false) }
  }

  fun clearErrorForRetry() {
    val offline = !isNetworkAvailable()
    _uiState.update {
      it.copy(
        hasError = offline,
        isOfflineError = offline,
        isLoading = !offline,
        progress = if (offline) 0 else 10,
      )
    }
  }

  fun toggleFullscreen() {
    setFullscreen(!_uiState.value.isFullscreen)
  }

  fun setFullscreen(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_FULLSCREEN, enabled).apply()
    _uiState.update { it.copy(isFullscreen = enabled) }
  }

  fun isNetworkAvailable(): Boolean {
    val cm =
      getApplication<Application>()
        .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return true
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
      capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
  }

  private companion object {
    const val KEY_FULLSCREEN = "fullscreen_mode"
  }
}
