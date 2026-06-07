package com.stockwidget.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockwidget.data.manager.FinnhubWebSocketManager
import com.stockwidget.domain.model.Result
import com.stockwidget.domain.model.StockQuote
import com.stockwidget.domain.model.Ticker
import com.stockwidget.domain.model.WatchedSymbol
import com.stockwidget.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val quotes: List<StockQuote> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdated: Long? = null,
    val isLive: Boolean = false  // true when WebSocket is delivering prices
)

data class SearchUiState(
    val query: String = "",
    val results: List<Ticker> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null
)

@HiltViewModel
class StockDashboardViewModel @Inject constructor(
    private val getStockQuotes: GetStockQuotesUseCase,
    private val searchTickers: SearchTickersUseCase,
    observeWatchedSymbols: ObserveWatchedSymbolsUseCase,
    private val addWatchedSymbol: AddWatchedSymbolUseCase,
    private val removeWatchedSymbol: RemoveWatchedSymbolUseCase,
    private val webSocketManager: FinnhubWebSocketManager
) : ViewModel() {

    val watchedSymbols: StateFlow<List<WatchedSymbol>> =
        observeWatchedSymbols()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _dashboardState = MutableStateFlow(DashboardUiState())
    val dashboardState: StateFlow<DashboardUiState> = _dashboardState.asStateFlow()

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()
    private var searchJob: Job? = null

    private val openPrices = mutableMapOf<String, Double>()

    init {
        // Initial REST fetch + re-subscribe when symbols change
        viewModelScope.launch {
            watchedSymbols.collect { symbols ->
                if (symbols.isNotEmpty()) {
                    refreshQuotes(symbols.map { it.symbol })
                    // Service manages the WebSocket connection;
                    // just ensure new symbols are subscribed
                    webSocketManager.subscribeAll(symbols.map { it.symbol })
                }
            }
        }

        // Listen to WebSocket trades for in-app live UI updates
        viewModelScope.launch {
            webSocketManager.trades.collect { trade ->
                val currentQuotes = _dashboardState.value.quotes.toMutableList()
                val idx = currentQuotes.indexOfFirst { it.symbol == trade.symbol }
                if (idx >= 0) {
                    val open      = openPrices[trade.symbol]?.takeIf { it != 0.0 }
                        ?: currentQuotes[idx].open.takeIf { it != 0.0 }
                        ?: trade.price
                    val newChange = trade.price - open
                    val newPct    = ((newChange / open) * 100.0)
                    currentQuotes[idx] = currentQuotes[idx].copy(
                        price         = trade.price,
                        change        = newChange,
                        changePercent = newPct,
                        high          = maxOf(currentQuotes[idx].high, trade.price),
                        low           = if (currentQuotes[idx].low == 0.0) trade.price
                        else minOf(currentQuotes[idx].low, trade.price)
                    )
                    _dashboardState.update {
                        it.copy(
                            quotes      = currentQuotes,
                            lastUpdated = System.currentTimeMillis(),
                            isLive      = true
                        )
                    }
                }
            }
        }

        // REST fallback every 30 s (pre-market, after-hours, weekends)
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                val symbols = watchedSymbols.value.map { it.symbol }
                if (symbols.isNotEmpty()) refreshQuotes(symbols)
            }
        }
    }

    fun refresh() {
        val symbols = watchedSymbols.value.map { it.symbol }
        if (symbols.isNotEmpty()) refreshQuotes(symbols)
    }

    fun addSymbol(ticker: Ticker) {
        viewModelScope.launch {
            addWatchedSymbol(WatchedSymbol(symbol = ticker.symbol, displayName = ticker.name))
            webSocketManager.subscribe(ticker.symbol)
        }
    }

    fun removeSymbol(symbol: String) {
        viewModelScope.launch {
            removeWatchedSymbol(symbol)
            webSocketManager.unsubscribe(symbol)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchState.update { it.copy(query = query, searchError = null) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchState.update { it.copy(results = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            _searchState.update { it.copy(isSearching = true) }
            when (val result = searchTickers(query)) {
                is Result.Success -> _searchState.update {
                    it.copy(results = result.data, isSearching = false)
                }
                is Result.Error -> _searchState.update {
                    it.copy(searchError = result.message, isSearching = false)
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun submitSearch() {
        val query = _searchState.value.query.trim()
        if (query.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _searchState.update { it.copy(isSearching = true, searchError = null) }
            when (val result = searchTickers(query)) {
                is Result.Success -> _searchState.update {
                    it.copy(results = result.data, isSearching = false)
                }
                is Result.Error -> _searchState.update {
                    it.copy(searchError = result.message, isSearching = false)
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchState.update { SearchUiState() }
    }

    private fun refreshQuotes(symbols: List<String>) {
        viewModelScope.launch {
            _dashboardState.update { it.copy(isLoading = it.quotes.isEmpty(), error = null) }
            when (val result = getStockQuotes(symbols)) {
                is Result.Success -> {
                    result.data.forEach { quote ->
                        openPrices[quote.symbol] = quote.open
                    }
                    _dashboardState.update {
                        it.copy(
                            quotes      = result.data,
                            isLoading   = false,
                            lastUpdated = System.currentTimeMillis(),
                            error       = null
                        )
                    }
                }
                is Result.Error -> _dashboardState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is Result.Loading -> Unit
            }
        }
    }
}