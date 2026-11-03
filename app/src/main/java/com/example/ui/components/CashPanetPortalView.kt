package com.example.ui.components

import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.model.PortalLoadState
import com.example.model.PortalWebState

@Composable
fun CashPanetPortalSection(
  retainedWebView: WebView?,
  webState: PortalWebState,
  isOnline: Boolean,
  onReloadClick: () -> Unit,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(enabled = webState.canGoBack) {
    if (retainedWebView?.canGoBack() == true) {
      retainedWebView.goBack()
    } else {
      onBackClick()
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("cashpanet_portal_container")
  ) {
    if (retainedWebView != null) {
      AndroidView(
        factory = {
          (retainedWebView.parent as? ViewGroup)?.removeView(retainedWebView)
          retainedWebView
        },
        modifier = Modifier
          .fillMaxSize()
          .testTag("cashpanet_webview"),
      )
    }

    // Ultra-thin 2dp progress hairline at top edge only while actively loading
    if (
      webState.loadState is PortalLoadState.Loading &&
      webState.loadProgress in 1..94
    ) {
      LinearProgressIndicator(
        progress = { (webState.loadProgress.coerceAtLeast(8)) / 100f },
        modifier = Modifier
          .align(Alignment.TopCenter)
          .fillMaxWidth()
          .height(2.dp)
          .testTag("portal_progress_bar"),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
      )
    }

    val errorState = webState.loadState as? PortalLoadState.ConnectionError
    if (errorState != null) {
      PortalConnectionErrorOverlay(
        isOnline = isOnline,
        reason = errorState.reason,
        onRetry = onReloadClick,
      )
    }
  }
}

@Composable
private fun PortalConnectionErrorOverlay(
  isOnline: Boolean,
  reason: String,
  onRetry: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(24.dp)
      .testTag("portal_error_overlay"),
    contentAlignment = Alignment.Center,
  ) {
    Card(
      shape = MaterialTheme.shapes.extraLarge,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 420.dp),
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(28.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.errorContainer,
          modifier = Modifier.size(60.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = if (!isOnline) Icons.Filled.WifiOff else Icons.Filled.CloudOff,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.size(28.dp),
            )
          }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
          text = stringResource(
            if (!isOnline) R.string.error_title_offline else R.string.error_title_connection
          ),
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = if (!isOnline) {
            stringResource(R.string.error_body_offline)
          } else {
            stringResource(R.string.error_body_connection, reason)
          },
          style = MaterialTheme.typography.bodyMedium,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(22.dp))
        Button(
          onClick = onRetry,
          modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("portal_retry_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = stringResource(R.string.action_retry_connection))
        }
      }
    }
  }
}
