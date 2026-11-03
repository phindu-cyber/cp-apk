package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.util.Consumer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.CASH_PANET_BASE_URL
import com.example.model.CASH_PANET_HOST
import com.example.model.PortalLoadState
import com.example.ui.components.CashPanetPortalSection
import com.example.ui.components.CashPanetSplashScreen
import com.example.ui.components.rememberRetainedPortalWebView
import com.example.ui.theme.CashPanetTheme
import com.example.util.AndroidConnectivityObserver
import com.example.viewmodel.CashPanetViewModel
import kotlinx.coroutines.delay

private const val MIN_SPLASH_DURATION_MS = 1_300L
private const val MAX_SPLASH_DURATION_MS = 3_200L

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val launchUrl = extractValidatedDeepLink(intent) ?: CASH_PANET_BASE_URL

    setContent {
      val appContext = applicationContext
      val connectivityObserver = remember(appContext) { AndroidConnectivityObserver(appContext) }
      val viewModel: CashPanetViewModel = viewModel(
        factory = CashPanetViewModel.provideFactory(
          connectivityObserver = connectivityObserver,
          initialUrl = launchUrl,
        )
      )

      DisposableEffect(viewModel) {
        val listener = Consumer<Intent> { incomingIntent ->
          val deepLink = extractValidatedDeepLink(incomingIntent)
          if (deepLink != null) {
            viewModel.openUrlInPortal(deepLink)
          }
        }
        addOnNewIntentListener(listener)
        onDispose { removeOnNewIntentListener(listener) }
      }

      CashPanetAppRoot(viewModel = viewModel)
    }
  }

  companion object {
    fun extractValidatedDeepLink(intent: Intent?): String? {
      if (intent?.action != Intent.ACTION_VIEW) return null
      val data = intent.data ?: return null
      val host = data.host?.lowercase() ?: return null
      return if (host == CASH_PANET_HOST || host.endsWith(".$CASH_PANET_HOST")) {
        data.toString()
      } else {
        null
      }
    }
  }
}

@Composable
fun CashPanetAppRoot(
  viewModel: CashPanetViewModel,
  includeLiveWebView: Boolean = true,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  var minSplashElapsed by rememberSaveable { mutableStateOf(false) }
  var showSplash by rememberSaveable { mutableStateOf(true) }

  // Minimum splash visibility timer + maximum safety timeout
  LaunchedEffect(Unit) {
    if (showSplash) {
      delay(MIN_SPLASH_DURATION_MS)
      minSplashElapsed = true
      delay(MAX_SPLASH_DURATION_MS - MIN_SPLASH_DURATION_MS)
      showSplash = false
    }
  }

  // Dismiss splash once minimum brand display has elapsed and initial load completes or errors
  LaunchedEffect(minSplashElapsed, uiState.portalWebState.loadState) {
    if (minSplashElapsed && uiState.portalWebState.loadState !is PortalLoadState.Loading) {
      showSplash = false
    }
  }

  val retainedWebView = if (includeLiveWebView) {
    rememberRetainedPortalWebView(
      initialUrl = uiState.portalWebState.currentUrl,
      commands = viewModel.commands,
      onPageStarted = viewModel::onWebPageStarted,
      onHistoryUpdated = viewModel::onWebHistoryUpdated,
      onProgressChanged = viewModel::onWebProgressChanged,
      onPageFinished = viewModel::onWebPageFinished,
      onMainFrameError = viewModel::onWebMainFrameError,
    )
  } else {
    null
  }

  CashPanetTheme {
    Box(modifier = Modifier.fillMaxSize()) {
      Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
      ) { innerPadding ->
        CashPanetPortalSection(
          retainedWebView = retainedWebView,
          webState = uiState.portalWebState,
          isOnline = uiState.isOnline,
          onReloadClick = viewModel::reloadPortal,
          onBackClick = viewModel::navigatePortalBack,
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        )
      }

      // Full-bleed edge-to-edge Splash Screen overlay during initial app launch
      AnimatedVisibility(
        visible = showSplash,
        exit = fadeOut(animationSpec = tween(durationMillis = 320)),
      ) {
        CashPanetSplashScreen()
      }
    }
  }
}
