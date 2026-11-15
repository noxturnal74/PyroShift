package com.example.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.components.PyroWebView
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PyroAmber
import com.example.ui.theme.PyroOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PyroShiftScreen(
  isTabActive: Boolean = true,
  pendingUrlToLoad: String? = null,
  onPendingUrlConsumed: () -> Unit = {},
  onFullscreenChanged: (Boolean) -> Unit = {},
  viewModel: WebBrowserViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  var webViewRef by remember { mutableStateOf<WebView?>(null) }

  LaunchedEffect(uiState.isFullscreen) {
    onFullscreenChanged(uiState.isFullscreen)
  }

  LaunchedEffect(pendingUrlToLoad, webViewRef) {
    val target = pendingUrlToLoad
    val wv = webViewRef
    if (!target.isNullOrBlank() && wv != null) {
      viewModel.clearErrorForRetry()
      wv.loadUrl(target)
      onPendingUrlConsumed()
    }
  }

  // Safety fallback so initial splash overlay never blocks the user if a page loads slowly
  LaunchedEffect(uiState.isInitialLoad) {
    if (uiState.isInitialLoad) {
      delay(4500L)
      viewModel.onSplashTimeout()
    }
  }

  BackHandler(enabled = isTabActive && uiState.canGoBack) {
    webViewRef?.goBack()
  }

  val cacheClearedMsg = stringResource(R.string.msg_cache_cleared)
  val noBrowserMsg = stringResource(R.string.msg_no_browser_found)
  val downloadStartedMsg = stringResource(R.string.msg_download_started)
  val fullscreenHintMsg = stringResource(R.string.msg_fullscreen_hint)
  val shareChooserTitle = stringResource(R.string.share_chooser_title)

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = {
      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
      )
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .let { mod ->
            if (uiState.isFullscreen) mod.windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)
            else mod
          }
          .imePadding()
    ) {
      AnimatedVisibility(
        visible = !uiState.isFullscreen,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
      ) {
        PyroTopBar(
          title = uiState.pageTitle.ifBlank { stringResource(R.string.app_name) },
          currentUrl = uiState.currentUrl,
          canGoBack = uiState.canGoBack,
          canGoForward = uiState.canGoForward,
          isLoading = uiState.isLoading,
          onBack = { webViewRef?.goBack() },
          onForward = { webViewRef?.goForward() },
          onRefresh = {
            viewModel.clearErrorForRetry()
            webViewRef?.reload()
          },
          onHome = {
            viewModel.clearErrorForRetry()
            webViewRef?.loadUrl(uiState.homeUrl)
          },
          onToggleFullscreen = {
            viewModel.toggleFullscreen()
            scope.launch { snackbarHostState.showSnackbar(fullscreenHintMsg) }
          },
          onShare = { shareUrl(context, uiState.currentUrl, shareChooserTitle) },
          onOpenInBrowser = {
            openInExternalBrowser(context, uiState.currentUrl) {
              scope.launch { snackbarHostState.showSnackbar(noBrowserMsg) }
            }
          },
          onClearCache = {
            webViewRef?.clearCache(true)
            viewModel.clearErrorForRetry()
            webViewRef?.reload()
            scope.launch { snackbarHostState.showSnackbar(cacheClearedMsg) }
          },
        )
      }

      val animatedProgress by
        animateFloatAsState(
          targetValue = uiState.progress / 100f,
          animationSpec = tween(durationMillis = 180),
          label = "web_progress",
        )

      if (uiState.isLoading && !uiState.hasError) {
        LinearProgressIndicator(
          progress = { animatedProgress.coerceIn(0.05f, 1f) },
          modifier = Modifier.fillMaxWidth().height(2.5.dp).testTag("loading_progress_bar"),
          color = PyroOrange,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
      }

      Box(modifier = Modifier.fillMaxSize().weight(1f)) {
        PyroWebView(
          initialUrl = uiState.homeUrl,
          modifier = Modifier.fillMaxSize().testTag("pyroshift_webview"),
          isVisible = isTabActive,
          onWebViewCreated = { webViewRef = it },
          onPageStarted = viewModel::onPageStarted,
          onProgressChanged = viewModel::onProgressChanged,
          onPageFinished = viewModel::onPageFinished,
          onNavigationStateChanged = viewModel::onNavigationStateChanged,
          onMainFrameError = viewModel::onReceivedMainFrameError,
          onDownloadStarted = {
            scope.launch { snackbarHostState.showSnackbar(downloadStartedMsg) }
          },
          onExternalAppMissing = {
            scope.launch { snackbarHostState.showSnackbar(noBrowserMsg) }
          },
        )

        if (uiState.isFullscreen && !uiState.hasError && !uiState.isInitialLoad) {
          FilledTonalIconButton(
            onClick = { viewModel.setFullscreen(false) },
            modifier =
              Modifier.align(Alignment.TopEnd)
                .padding(12.dp)
                .size(48.dp)
                .testTag("exit_fullscreen_button"),
            colors =
              IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = ObsidianSurface.copy(alpha = 0.78f),
                contentColor = Color.White,
              ),
          ) {
            Icon(
              imageVector = Icons.Rounded.FullscreenExit,
              contentDescription = stringResource(R.string.cd_exit_fullscreen),
            )
          }
        }

        androidx.compose.animation.AnimatedVisibility(
          visible = uiState.hasError,
          enter = fadeIn(tween(220)),
          exit = fadeOut(tween(180)),
        ) {
          ErrorRecoveryOverlay(
            isOffline = uiState.isOfflineError,
            onRetry = {
              viewModel.clearErrorForRetry()
              val target = webViewRef?.url?.takeIf { it.isNotBlank() } ?: uiState.homeUrl
              webViewRef?.loadUrl(target)
            },
            onOpenInBrowser = {
              openInExternalBrowser(context, uiState.currentUrl) {
                scope.launch { snackbarHostState.showSnackbar(noBrowserMsg) }
              }
            },
          )
        }

        androidx.compose.animation.AnimatedVisibility(
          visible = uiState.isInitialLoad && !uiState.hasError,
          enter = fadeIn(),
          exit = fadeOut(tween(300)),
        ) {
          BrandedSplashOverlay()
        }
      }
    }
  }
}

@Composable
private fun PyroTopBar(
  title: String,
  currentUrl: String,
  canGoBack: Boolean,
  canGoForward: Boolean,
  isLoading: Boolean,
  onBack: () -> Unit,
  onForward: () -> Unit,
  onRefresh: () -> Unit,
  onHome: () -> Unit,
  onToggleFullscreen: () -> Unit,
  onShare: () -> Unit,
  onOpenInBrowser: () -> Unit,
  onClearCache: () -> Unit,
) {
  var menuExpanded by remember { mutableStateOf(false) }
  val displayHost = remember(currentUrl) { Uri.parse(currentUrl).host ?: "pyroshift.vercel.app" }

  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    shadowElevation = 2.dp,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_pyroshift_icon_fg),
        contentDescription = stringResource(R.string.cd_app_logo),
        contentScale = ContentScale.Crop,
        modifier =
          Modifier.size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, PyroOrange.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
      )

      Spacer(modifier = Modifier.width(10.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = displayHost,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      IconButton(
        onClick = onBack,
        enabled = canGoBack,
        modifier = Modifier.size(48.dp).testTag("nav_back_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = stringResource(R.string.cd_back),
        )
      }

      IconButton(
        onClick = onRefresh,
        modifier = Modifier.size(48.dp).testTag("nav_refresh_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Refresh,
          contentDescription = stringResource(R.string.cd_refresh),
          tint = if (isLoading) PyroOrange else MaterialTheme.colorScheme.onSurface,
        )
      }

      Box {
        IconButton(
          onClick = { menuExpanded = true },
          modifier = Modifier.size(48.dp).testTag("nav_more_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = stringResource(R.string.cd_more_options),
          )
        }

        DropdownMenu(
          expanded = menuExpanded,
          onDismissRequest = { menuExpanded = false },
        ) {
          DropdownMenuItem(
            text = { Text(stringResource(R.string.cd_home)) },
            leadingIcon = { Icon(Icons.Rounded.Home, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onHome()
            },
            modifier = Modifier.testTag("menu_home_item"),
          )
          if (canGoForward) {
            DropdownMenuItem(
              text = { Text(stringResource(R.string.cd_forward)) },
              leadingIcon = {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
              },
              onClick = {
                menuExpanded = false
                onForward()
              },
              modifier = Modifier.testTag("menu_forward_item"),
            )
          }
          DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_fullscreen_mode)) },
            leadingIcon = { Icon(Icons.Rounded.Fullscreen, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onToggleFullscreen()
            },
            modifier = Modifier.testTag("menu_fullscreen_item"),
          )
          DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_share_link)) },
            leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onShare()
            },
            modifier = Modifier.testTag("menu_share_item"),
          )
          DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_open_in_browser)) },
            leadingIcon = {
              Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
            },
            onClick = {
              menuExpanded = false
              onOpenInBrowser()
            },
            modifier = Modifier.testTag("menu_browser_item"),
          )
          DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_clear_cache)) },
            leadingIcon = { Icon(Icons.Rounded.CleaningServices, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onClearCache()
            },
            modifier = Modifier.testTag("menu_clear_cache_item"),
          )
        }
      }
    }
  }
}

@Composable
private fun BrandedSplashOverlay() {
  val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
  val pulseScale by
    infiniteTransition.animateFloat(
      initialValue = 0.96f,
      targetValue = 1.04f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(950, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "logo_scale",
    )
  val glowAlpha by
    infiniteTransition.animateFloat(
      initialValue = 0.35f,
      targetValue = 0.75f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(950, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "glow_alpha",
    )

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(ObsidianBg, ObsidianSurface, ObsidianBg)
          )
        ),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(32.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Box(
          modifier =
            Modifier.size(136.dp)
              .alpha(glowAlpha)
              .background(
                Brush.radialGradient(
                  colors = listOf(PyroOrange.copy(alpha = 0.45f), Color.Transparent)
                ),
                shape = CircleShape,
              )
        )
        Image(
          painter = painterResource(id = R.drawable.img_pyroshift_icon_fg),
          contentDescription = stringResource(R.string.cd_app_logo),
          contentScale = ContentScale.Crop,
          modifier =
            Modifier.size(96.dp)
              .scale(pulseScale)
              .clip(RoundedCornerShape(26.dp))
              .border(1.5.dp, PyroAmber.copy(alpha = 0.4f), RoundedCornerShape(26.dp)),
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineSmall,
        color = Color.White,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = stringResource(R.string.splash_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFFA7AEC2),
      )

      Spacer(modifier = Modifier.height(20.dp))

      CircularProgressIndicator(
        modifier = Modifier.size(28.dp),
        color = PyroOrange,
        trackColor = ObsidianSurface,
        strokeWidth = 3.dp,
      )
    }
  }
}

@Composable
private fun ErrorRecoveryOverlay(
  isOffline: Boolean,
  onRetry: () -> Unit,
  onOpenInBrowser: () -> Unit,
) {
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_offline_state),
        contentDescription = stringResource(R.string.cd_offline_illustration),
        contentScale = ContentScale.Crop,
        modifier =
          Modifier.fillMaxWidth(0.75f)
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp)),
      )

      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text =
          stringResource(
            if (isOffline) R.string.error_title_offline else R.string.error_title_load_failed
          ),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text =
          stringResource(
            if (isOffline) R.string.error_desc_offline else R.string.error_desc_load_failed
          ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(28.dp))

      Button(
        onClick = onRetry,
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("retry_button"),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = PyroOrange,
            contentColor = Color.White,
          ),
      ) {
        Icon(
          imageVector = Icons.Rounded.Refresh,
          contentDescription = null,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(R.string.action_retry),
          style = MaterialTheme.typography.labelLarge,
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedButton(
        onClick = onOpenInBrowser,
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("open_browser_error_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(R.string.action_open_browser),
          style = MaterialTheme.typography.labelLarge,
        )
      }
    }
  }
}

private fun shareUrl(context: Context, url: String, chooserTitle: String) {
  val sendIntent =
    Intent(Intent.ACTION_SEND).apply {
      putExtra(Intent.EXTRA_TEXT, url)
      type = "text/plain"
    }
  val shareIntent = Intent.createChooser(sendIntent, chooserTitle)
  context.startActivity(shareIntent)
}

private fun openInExternalBrowser(
  context: Context,
  url: String,
  onNotFound: () -> Unit,
) {
  try {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
  } catch (_: ActivityNotFoundException) {
    onNotFound()
  }
}
