package com.example.ui.components

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PyroWebView(
  initialUrl: String,
  modifier: Modifier = Modifier,
  isVisible: Boolean = true,
  onWebViewCreated: (WebView) -> Unit,
  onPageStarted: (url: String?, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
  onProgressChanged: (Int) -> Unit,
  onPageFinished: (url: String?, title: String?, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
  onNavigationStateChanged: (canGoBack: Boolean, canGoForward: Boolean) -> Unit,
  onMainFrameError: () -> Unit,
  onDownloadStarted: () -> Unit,
  onExternalAppMissing: () -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

  val fileChooserLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
      val callback = filePathCallback
      filePathCallback = null
      if (callback != null) {
        callback.onReceiveValue(if (uris.isNotEmpty()) uris.toTypedArray() else null)
      }
    }

  val webView = remember {
    WebView(context).apply {
      layoutParams =
        ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT,
        )
      setBackgroundColor(android.graphics.Color.parseColor("#0F111E"))

      val webViewInstance = this
      CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(webViewInstance, true)
      }

      settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        loadsImagesAutomatically = true
        useWideViewPort = true
        loadWithOverviewMode = true
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
        allowFileAccess = false
        allowContentAccess = true
        mediaPlaybackRequiresUserGesture = false
        setSupportMultipleWindows(true)
        javaScriptCanOpenWindowsAutomatically = true
        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        cacheMode = WebSettings.LOAD_DEFAULT
      }

      webViewClient =
        object : WebViewClient() {
          override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?,
          ): Boolean {
            val uri = request?.url ?: return false
            val scheme = uri.scheme?.lowercase() ?: return false
            if (scheme == "http" || scheme == "https") {
              return false
            }
            openExternalUri(context, uri, onExternalAppMissing)
            return true
          }

          override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            onPageStarted(url, view?.canGoBack() == true, view?.canGoForward() == true)
          }

          override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            CookieManager.getInstance().flush()
            onPageFinished(
              url,
              view?.title,
              view?.canGoBack() == true,
              view?.canGoForward() == true,
            )
          }

          override fun doUpdateVisitedHistory(
            view: WebView?,
            url: String?,
            isReload: Boolean,
          ) {
            super.doUpdateVisitedHistory(view, url, isReload)
            onNavigationStateChanged(view?.canGoBack() == true, view?.canGoForward() == true)
          }

          override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?,
          ) {
            super.onReceivedError(view, request, error)
            if (request?.isForMainFrame == true) {
              onMainFrameError()
            }
          }
        }

      webChromeClient =
        object : WebChromeClient() {
          override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            onProgressChanged(newProgress)
          }

          override fun onShowFileChooser(
            webView: WebView?,
            callback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?,
          ): Boolean {
            filePathCallback?.onReceiveValue(null)
            filePathCallback = callback
            val acceptTypes =
              fileChooserParams
                ?.acceptTypes
                ?.filter { it.isNotBlank() }
                ?.toTypedArray()
                ?.takeIf { it.isNotEmpty() } ?: arrayOf("*/*")
            return try {
              fileChooserLauncher.launch(acceptTypes)
              true
            } catch (_: ActivityNotFoundException) {
              filePathCallback?.onReceiveValue(null)
              filePathCallback = null
              false
            }
          }

          override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?,
          ): Boolean {
            val result = view?.hitTestResult
            val targetUrl = result?.extra
            if (!targetUrl.isNullOrBlank()) {
              view.loadUrl(targetUrl)
              return false
            }
            val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
            val tempWebView = WebView(context)
            tempWebView.webViewClient =
              object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                  popupView: WebView?,
                  request: WebResourceRequest?,
                ): Boolean {
                  val popupUrl = request?.url?.toString()
                  if (!popupUrl.isNullOrBlank()) {
                    view?.loadUrl(popupUrl)
                  }
                  popupView?.destroy()
                  return true
                }
              }
            transport.webView = tempWebView
            resultMsg.sendToTarget()
            return true
          }
        }

      setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
        handleDownload(
          context = context,
          url = url,
          userAgent = userAgent,
          contentDisposition = contentDisposition,
          mimeType = mimetype,
          onDownloadStarted = onDownloadStarted,
          onFallbackError = onExternalAppMissing,
        )
      }

      loadUrl(initialUrl)
    }
  }

  DisposableEffect(lifecycleOwner, webView) {
    onWebViewCreated(webView)
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> webView.onResume()
        Lifecycle.Event.ON_PAUSE -> {
          webView.onPause()
          CookieManager.getInstance().flush()
        }
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      filePathCallback?.onReceiveValue(null)
      filePathCallback = null
      webView.stopLoading()
      webView.destroy()
    }
  }

  AndroidView(
    factory = { webView },
    update = { view ->
      val targetVisibility =
        if (isVisible) android.view.View.VISIBLE else android.view.View.GONE
      if (view.visibility != targetVisibility) {
        view.visibility = targetVisibility
      }
    },
    modifier = modifier,
  )
}

private fun openExternalUri(
  context: Context,
  uri: Uri,
  onExternalAppMissing: () -> Unit,
) {
  try {
    val intent =
      if (uri.scheme.equals("intent", ignoreCase = true)) {
        Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
      } else {
        Intent(Intent.ACTION_VIEW, uri)
      }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
  } catch (_: Exception) {
    onExternalAppMissing()
  }
}

private fun handleDownload(
  context: Context,
  url: String,
  userAgent: String?,
  contentDisposition: String?,
  mimeType: String?,
  onDownloadStarted: () -> Unit,
  onFallbackError: () -> Unit,
) {
  if (!url.startsWith("http://") && !url.startsWith("https://")) {
    openExternalUri(context, Uri.parse(url), onFallbackError)
    return
  }
  try {
    val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
    val request =
      DownloadManager.Request(Uri.parse(url)).apply {
        setMimeType(mimeType)
        val cookies = CookieManager.getInstance().getCookie(url)
        if (!cookies.isNullOrBlank()) {
          addRequestHeader("Cookie", cookies)
        }
        if (!userAgent.isNullOrBlank()) {
          addRequestHeader("User-Agent", userAgent)
        }
        setTitle(fileName)
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
      }
    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
    dm?.enqueue(request)
    onDownloadStarted()
  } catch (_: Exception) {
    openExternalUri(context, Uri.parse(url), onFallbackError)
  }
}
