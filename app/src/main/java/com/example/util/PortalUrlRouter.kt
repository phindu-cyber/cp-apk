package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri

sealed interface PortalRouteDecision {
  data object AllowInWebView : PortalRouteDecision
  data class OpenExternalSocialTask(val uri: Uri) : PortalRouteDecision
  data class LaunchCustomScheme(val rawUrl: String) : PortalRouteDecision
}

object PortalUrlRouter {

  private val socialTaskDomains = listOf(
    "youtube.com",
    "youtu.be",
    "tiktok.com",
    "instagram.com",
    "facebook.com",
    "fb.com",
    "fb.watch",
    "wa.me",
    "whatsapp.com",
    "t.me",
    "telegram.me",
    "x.com",
    "twitter.com",
  )

  fun evaluateNavigation(
    rawUrl: String,
    openSocialExternally: Boolean,
  ): PortalRouteDecision {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return PortalRouteDecision.AllowInWebView

    val parsed = runCatching { Uri.parse(trimmed) }.getOrNull()
      ?: return PortalRouteDecision.AllowInWebView

    val scheme = parsed.scheme?.lowercase().orEmpty()
    if (scheme !in listOf("http", "https", "about", "blob", "data", "javascript")) {
      return PortalRouteDecision.LaunchCustomScheme(trimmed)
    }

    if (scheme != "http" && scheme != "https") {
      return PortalRouteDecision.AllowInWebView
    }

    val host = parsed.host?.lowercase().orEmpty()
    if (host == "cashpanet.app" || host.endsWith(".cashpanet.app")) {
      return PortalRouteDecision.AllowInWebView
    }

    // Keep OAuth and payment callback URLs inside the WebView session
    val fullLower = trimmed.lowercase()
    if (
      fullLower.contains("/oauth") ||
      fullLower.contains("/dialog/") ||
      fullLower.contains("redirect_uri=") ||
      host.contains("paychangu") ||
      host.contains("accounts.google.com")
    ) {
      return PortalRouteDecision.AllowInWebView
    }

    if (openSocialExternally && matchesSocialDomain(host)) {
      return PortalRouteDecision.OpenExternalSocialTask(parsed)
    }

    return PortalRouteDecision.AllowInWebView
  }

  private fun matchesSocialDomain(host: String): Boolean {
    return socialTaskDomains.any { domain ->
      host == domain || host.endsWith(".$domain")
    }
  }

  fun launchExternalUri(context: Context, uri: Uri): Boolean {
    return runCatching {
      val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      true
    }.getOrDefault(false)
  }

  fun launchCustomSchemeUrl(
    context: Context,
    rawUrl: String,
    onFallbackWebUrl: (String) -> Unit,
  ): Boolean {
    return runCatching {
      if (rawUrl.startsWith("intent:", ignoreCase = true)) {
        val intent = Intent.parseUri(rawUrl, Intent.URI_INTENT_SCHEME).apply {
          addCategory(Intent.CATEGORY_BROWSABLE)
          component = null
          selector = null
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val resolved = context.packageManager.resolveActivity(intent, 0)
        if (resolved != null) {
          context.startActivity(intent)
          return true
        }
        val fallbackUrl = intent.getStringExtra("browser_fallback_url")
        if (!fallbackUrl.isNullOrBlank()) {
          onFallbackWebUrl(fallbackUrl)
          return true
        }
        false
      } else {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl)).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
      }
    }.getOrDefault(false)
  }

  fun shareTextLink(context: Context, title: String, url: String, chooserLabel: String) {
    runCatching {
      val shareBody = if (title.isNotBlank() && !title.equals(url, ignoreCase = true)) {
        "$title\n$url"
      } else {
        url
      }
      val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, shareBody)
      }
      val chooser = Intent.createChooser(sendIntent, chooserLabel).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(chooser)
    }
  }

  fun copyToClipboard(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, value))
  }
}
