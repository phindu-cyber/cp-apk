package com.example.model

const val CASH_PANET_BASE_URL = "https://cashpanet.app/"
const val CASH_PANET_HOST = "cashpanet.app"

sealed interface PortalLoadState {
  data object Loading : PortalLoadState
  data object Ready : PortalLoadState
  data class ConnectionError(val reason: String) : PortalLoadState
}

data class PortalWebState(
  val currentUrl: String = CASH_PANET_BASE_URL,
  val loadProgress: Int = 0,
  val canGoBack: Boolean = false,
  val loadState: PortalLoadState = PortalLoadState.Loading,
)

sealed interface WebPortalCommand {
  data class LoadUrl(val url: String) : WebPortalCommand
  data object Reload : WebPortalCommand
  data object GoBack : WebPortalCommand
}
