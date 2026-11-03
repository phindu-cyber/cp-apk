package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.model.CASH_PANET_BASE_URL
import com.example.model.PortalLoadState
import com.example.model.PortalWebState
import com.example.model.WebPortalCommand
import com.example.util.ConnectivityObserver
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CashPanetUiState(
  val portalWebState: PortalWebState = PortalWebState(),
  val isOnline: Boolean = true,
)

class CashPanetViewModel(
  connectivityObserver: ConnectivityObserver,
  initialUrl: String = CASH_PANET_BASE_URL,
) : ViewModel() {

  private val _portalWebState = MutableStateFlow(PortalWebState(currentUrl = initialUrl))

  private val _commands = MutableSharedFlow<WebPortalCommand>(extraBufferCapacity = 8)
  val commands: SharedFlow<WebPortalCommand> = _commands.asSharedFlow()

  val uiState: StateFlow<CashPanetUiState> = combine(
    _portalWebState,
    connectivityObserver.isOnline,
  ) { webState, online ->
    CashPanetUiState(
      portalWebState = webState,
      isOnline = online,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = CashPanetUiState(
      portalWebState = PortalWebState(currentUrl = initialUrl)
    ),
  )

  init {
    // Automatically reload if network comes back online while on connection error state
    viewModelScope.launch {
      var wasOffline = false
      connectivityObserver.isOnline.collect { online ->
        if (online && wasOffline && _portalWebState.value.loadState is PortalLoadState.ConnectionError) {
          reloadPortal()
        }
        wasOffline = !online
      }
    }
  }

  fun openUrlInPortal(url: String) {
    val target = url.trim().ifEmpty { CASH_PANET_BASE_URL }
    _portalWebState.update {
      it.copy(
        currentUrl = target,
        loadState = PortalLoadState.Loading,
      )
    }
    _commands.tryEmit(WebPortalCommand.LoadUrl(target))
  }

  fun reloadPortal() {
    _portalWebState.update {
      it.copy(loadState = PortalLoadState.Loading)
    }
    _commands.tryEmit(WebPortalCommand.Reload)
  }

  fun navigatePortalBack() {
    _commands.tryEmit(WebPortalCommand.GoBack)
  }

  fun onWebPageStarted(url: String, canGoBack: Boolean) {
    _portalWebState.update {
      it.copy(
        currentUrl = url,
        canGoBack = canGoBack,
        loadState = PortalLoadState.Loading,
      )
    }
  }

  fun onWebHistoryUpdated(url: String, canGoBack: Boolean) {
    _portalWebState.update {
      it.copy(
        currentUrl = url.ifBlank { it.currentUrl },
        canGoBack = canGoBack,
      )
    }
  }

  fun onWebProgressChanged(progress: Int, canGoBack: Boolean) {
    _portalWebState.update { state ->
      val nextLoad = if (progress >= 95 && state.loadState !is PortalLoadState.ConnectionError) {
        PortalLoadState.Ready
      } else {
        state.loadState
      }
      state.copy(
        loadProgress = progress.coerceIn(0, 100),
        canGoBack = canGoBack,
        loadState = nextLoad,
      )
    }
  }

  fun onWebPageFinished(url: String, canGoBack: Boolean) {
    _portalWebState.update { state ->
      val nextLoad = if (state.loadState is PortalLoadState.ConnectionError) {
        state.loadState
      } else {
        PortalLoadState.Ready
      }
      state.copy(
        currentUrl = url,
        loadProgress = 100,
        canGoBack = canGoBack,
        loadState = nextLoad,
      )
    }
  }

  fun onWebMainFrameError(description: String) {
    _portalWebState.update { state ->
      state.copy(
        loadProgress = 0,
        loadState = PortalLoadState.ConnectionError(description),
      )
    }
  }

  companion object {
    fun provideFactory(
      connectivityObserver: ConnectivityObserver,
      initialUrl: String = CASH_PANET_BASE_URL,
    ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CashPanetViewModel(
          connectivityObserver = connectivityObserver,
          initialUrl = initialUrl,
        ) as T
      }
    }
  }
}
