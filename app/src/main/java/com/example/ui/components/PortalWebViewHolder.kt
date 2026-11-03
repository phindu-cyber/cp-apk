package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.model.CASH_PANET_BASE_URL
import com.example.model.WebPortalCommand
import com.example.util.PortalRouteDecision
import com.example.util.PortalUrlRouter
import kotlinx.coroutines.flow.SharedFlow

private const val MOBILE_USER_AGENT_SUFFIX = " CashPanetAndroid/1.0"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun rememberRetainedPortalWebView(
  initialUrl: String,
  commands: SharedFlow<WebPortalCommand>,
  onPageStarted: (String, Boolean) -> Unit,
  onHistoryUpdated: (String, Boolean) -> Unit,
  onProgressChanged: (Int, Boolean) -> Unit,
  onPageFinished: (String, Boolean) -> Unit,
  onMainFrameError: (String) -> Unit,
): WebView {
  val context = LocalContext.current
  val currentOnPageStarted by rememberUpdatedState(onPageStarted)
  val currentOnHistoryUpdated by rememberUpdatedState(onHistoryUpdated)
  val currentOnProgressChanged by rememberUpdatedState(onProgressChanged)
  val currentOnPageFinished by rememberUpdatedState(onPageFinished)
  val currentOnMainFrameError by rememberUpdatedState(onMainFrameError)

  var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

  val singleMediaLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    val callback = pendingFileCallback
    pendingFileCallback = null
    if (uri != null) {
      callback?.onReceiveValue(arrayOf(uri))
    } else {
      callback?.onReceiveValue(null)
    }
  }

  val multipleMediaLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia()
  ) { uris ->
    val callback = pendingFileCallback
    pendingFileCallback = null
    if (uris.isNotEmpty()) {
      callback?.onReceiveValue(uris.toTypedArray())
    } else {
      callback?.onReceiveValue(null)
    }
  }

  val documentPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    val callback = pendingFileCallback
    pendingFileCallback = null
    if (uri != null) {
      callback?.onReceiveValue(arrayOf(uri))
    } else {
      callback?.onReceiveValue(null)
    }
  }

  val retainedWebView = remember(context) {
    createConfiguredWebView(context)
  }

  DisposableEffect(retainedWebView) {
    retainedWebView.webViewClient = object : WebViewClient() {
      override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?,
      ): Boolean {
        val targetUrl = request?.url?.toString() ?: return false
        return when (
          val decision = PortalUrlRouter.evaluateNavigation(
            rawUrl = targetUrl,
            openSocialExternally = true,
          )
        ) {
          is PortalRouteDecision.AllowInWebView -> false
          is PortalRouteDecision.OpenExternalSocialTask -> {
            PortalUrlRouter.launchExternalUri(context, decision.uri)
          }
          is PortalRouteDecision.LaunchCustomScheme -> {
            PortalUrlRouter.launchCustomSchemeUrl(
              context = context,
              rawUrl = decision.rawUrl,
              onFallbackWebUrl = { fallback -> view?.loadUrl(fallback) },
            )
          }
        }
      }

      override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (!url.isNullOrBlank()) {
          currentOnPageStarted(url, view?.canGoBack() ?: false)
        }
      }

      override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        super.doUpdateVisitedHistory(view, url, isReload)
        currentOnHistoryUpdated(url.orEmpty(), view?.canGoBack() ?: false)
      }

      override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        runCatching { CookieManager.getInstance().flush() }
        currentOnPageFinished(
          url ?: CASH_PANET_BASE_URL,
          view?.canGoBack() ?: false,
        )
      }

      override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?,
      ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
          val reason = error?.description?.toString().orEmpty().ifBlank { "Network error" }
          currentOnMainFrameError(reason)
        }
      }
    }

    retainedWebView.webChromeClient = object : WebChromeClient() {
      override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        currentOnProgressChanged(
          newProgress,
          view?.canGoBack() ?: false,
        )
      }

      override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?,
      ): Boolean {
        pendingFileCallback?.onReceiveValue(null)
        pendingFileCallback = filePathCallback ?: return false

        val acceptTypes = fileChooserParams?.acceptTypes
          ?.filter { it.isNotBlank() }
          .orEmpty()
        val isMultiple = fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE

        val requestsDocumentsExplicitly = acceptTypes.any { mime ->
          mime.contains("pdf", ignoreCase = true) ||
            mime.contains("text/", ignoreCase = true) ||
            mime.contains("application/", ignoreCase = true)
        }

        val launched = runCatching {
          if (requestsDocumentsExplicitly) {
            val mimeArray = acceptTypes.toTypedArray().ifEmpty { arrayOf("*/*") }
            documentPickerLauncher.launch(mimeArray)
          } else {
            val request = PickVisualMediaRequest(
              ActivityResultContracts.PickVisualMedia.ImageAndVideo
            )
            if (isMultiple) {
              multipleMediaLauncher.launch(request)
            } else {
              singleMediaLauncher.launch(request)
            }
          }
          true
        }.getOrDefault(false)

        if (!launched) {
          pendingFileCallback?.onReceiveValue(null)
          pendingFileCallback = null
        }
        return launched
      }

      override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?,
      ): Boolean {
        val parentView = view ?: return false
        val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
        val tempWebView = WebView(parentView.context).apply {
          webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
              temp: WebView?,
              request: WebResourceRequest?,
            ): Boolean {
              val popupUrl = request?.url?.toString().orEmpty()
              if (popupUrl.isNotBlank()) {
                when (
                  val decision = PortalUrlRouter.evaluateNavigation(
                    rawUrl = popupUrl,
                    openSocialExternally = true,
                  )
                ) {
                  is PortalRouteDecision.AllowInWebView -> parentView.loadUrl(popupUrl)
                  is PortalRouteDecision.OpenExternalSocialTask -> {
                    if (!PortalUrlRouter.launchExternalUri(context, decision.uri)) {
                      parentView.loadUrl(popupUrl)
                    }
                  }
                  is PortalRouteDecision.LaunchCustomScheme -> {
                    PortalUrlRouter.launchCustomSchemeUrl(
                      context = context,
                      rawUrl = decision.rawUrl,
                      onFallbackWebUrl = { parentView.loadUrl(it) },
                    )
                  }
                }
              }
              temp?.destroy()
              return true
            }
          }
        }
        transport.webView = tempWebView
        resultMsg.sendToTarget()
        return true
      }
    }

    if (retainedWebView.url.isNullOrBlank()) {
      retainedWebView.loadUrl(initialUrl)
    }

    onDispose {
      pendingFileCallback?.onReceiveValue(null)
      pendingFileCallback = null
      runCatching {
        retainedWebView.stopLoading()
        retainedWebView.destroy()
      }
    }
  }

  LaunchedEffect(commands, retainedWebView) {
    commands.collect { command ->
      when (command) {
        is WebPortalCommand.LoadUrl -> retainedWebView.loadUrl(command.url)
        is WebPortalCommand.Reload -> retainedWebView.reload()
        is WebPortalCommand.GoBack -> {
          if (retainedWebView.canGoBack()) retainedWebView.goBack()
        }
      }
    }
  }

  return retainedWebView
}

@SuppressLint("SetJavaScriptEnabled")
private fun createConfiguredWebView(context: Context): WebView {
  return WebView(context).apply {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT,
    )
    val defaultMobileUa = this.settings.userAgentString.orEmpty() + MOBILE_USER_AGENT_SUFFIX

    with(this.settings) {
      javaScriptEnabled = true
      domStorageEnabled = true
      loadsImagesAutomatically = true
      useWideViewPort = true
      loadWithOverviewMode = true
      setSupportZoom(true)
      builtInZoomControls = true
      displayZoomControls = false
      javaScriptCanOpenWindowsAutomatically = true
      setSupportMultipleWindows(true)
      mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
      cacheMode = WebSettings.LOAD_DEFAULT
      userAgentString = defaultMobileUa
    }

    runCatching {
      val cookieManager = CookieManager.getInstance()
      cookieManager.setAcceptCookie(true)
      cookieManager.setAcceptThirdPartyCookies(this, true)
    }

    setDownloadListener { url, _, _, _, _ ->
      runCatching {
        PortalUrlRouter.launchExternalUri(context, Uri.parse(url))
      }
    }
  }
}
