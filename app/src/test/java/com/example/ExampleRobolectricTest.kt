package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.model.PortalLoadState
import com.example.util.PortalRouteDecision
import com.example.util.PortalUrlRouter
import com.example.util.StaticConnectivityObserver
import com.example.viewmodel.CashPanetViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun appName_matchesCashPanetBranding() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Cash Panet", appName)
  }

  @Test
  fun portalUrlRouter_routesCashPanetAndSocialLinksAccurately() {
    assertEquals(
      PortalRouteDecision.AllowInWebView,
      PortalUrlRouter.evaluateNavigation("https://cashpanet.app/dashboard", openSocialExternally = true),
    )

    assertEquals(
      PortalRouteDecision.AllowInWebView,
      PortalUrlRouter.evaluateNavigation("https://checkout.paychangu.com/pay/123", openSocialExternally = true),
    )

    val tiktokDecision = PortalUrlRouter.evaluateNavigation(
      "https://www.tiktok.com/@malawibusiness",
      openSocialExternally = true,
    )
    assertTrue(tiktokDecision is PortalRouteDecision.OpenExternalSocialTask)

    val whatsappDecision = PortalUrlRouter.evaluateNavigation(
      "whatsapp://send?text=https://cashpanet.app",
      openSocialExternally = true,
    )
    assertTrue(whatsappDecision is PortalRouteDecision.LaunchCustomScheme)
  }

  @Test
  fun deepLinkExtractor_acceptsOnlyCashPanetDomain() {
    val validIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://cashpanet.app/tasks"))
    assertEquals("https://cashpanet.app/tasks", MainActivity.extractValidatedDeepLink(validIntent))

    val untrustedIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.org/phish"))
    assertNull(MainActivity.extractValidatedDeepLink(untrustedIntent))
  }

  @Test
  fun splashScreen_showsLogoOnLaunchThenTransitionsToChromelessPortal() {
    val viewModel = CashPanetViewModel(
      connectivityObserver = StaticConnectivityObserver(online = false),
    )

    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.setContent {
      CashPanetAppRoot(
        viewModel = viewModel,
        includeLiveWebView = false,
      )
    }
    composeTestRule.mainClock.advanceTimeBy(350L)

    // Splash screen and uploaded Cash Panet logo are displayed on launch
    composeTestRule.onNodeWithTag("cashpanet_splash_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("splash_logo_image").assertIsDisplayed()

    // Simulate page load completion and advance past minimum splash duration
    viewModel.onWebPageFinished("https://cashpanet.app/", canGoBack = false)
    composeTestRule.mainClock.advanceTimeBy(2_000L)
    composeTestRule.mainClock.autoAdvance = true
    composeTestRule.waitForIdle()

    // Splash screen has faded out; chromeless portal is visible with no nav bars
    composeTestRule.onNodeWithTag("cashpanet_splash_screen").assertDoesNotExist()
    composeTestRule.onNodeWithTag("cashpanet_portal_container").assertIsDisplayed()
    composeTestRule.onNodeWithTag("portal_top_header").assertDoesNotExist()
    composeTestRule.onNodeWithTag("bottom_navigation_bar").assertDoesNotExist()

    // Simulate main-frame connection error and verify Retry transitions back to Loading
    viewModel.onWebMainFrameError("ERR_INTERNET_DISCONNECTED")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("portal_error_overlay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("portal_retry_button").performClick()
    composeTestRule.waitForIdle()

    assertEquals(PortalLoadState.Loading, viewModel.uiState.value.portalWebState.loadState)
  }
}
