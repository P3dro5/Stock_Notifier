package com.stockwidget.domain.repository

import com.stockwidget.domain.model.Result
import com.stockwidget.domain.model.StockQuote
import com.stockwidget.domain.model.Ticker
import com.stockwidget.domain.model.WatchedSymbol
import kotlinx.coroutines.flow.Flow

interface StockRepository {

    /** Fetch latest quotes for all watched symbols from the network. */
    suspend fun fetchQuotes(symbols: List<String>): Result<List<StockQuote>>

    /** Search tickers by keyword. */
    suspend fun searchTickers(query: String): Result<List<Ticker>>

    // ── Watched symbols (persisted locally) ────────────────────────────────

    /** Observe the list of watched symbols as a Flow. */
    fun observeWatchedSymbols(): Flow<List<WatchedSymbol>>

    /** Add a symbol to the watch list. */
    suspend fun addWatchedSymbol(symbol: WatchedSymbol)

    /** Remove a symbol from the watch list. */
    suspend fun removeWatchedSymbol(symbolId: String)

    /** Replace the entire watch list (used by widget config). */
    suspend fun setWatchedSymbols(symbols: List<WatchedSymbol>)
}
