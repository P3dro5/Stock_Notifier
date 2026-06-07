package com.stockwidget.domain.usecase

import com.stockwidget.domain.model.Result
import com.stockwidget.domain.model.StockQuote
import com.stockwidget.domain.model.Ticker
import com.stockwidget.domain.model.WatchedSymbol
import com.stockwidget.domain.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// ── GetStockQuotesUseCase ──────────────────────────────────────────────────

class GetStockQuotesUseCase @Inject constructor(
    private val repository: StockRepository
) {
    suspend operator fun invoke(symbols: List<String>): Result<List<StockQuote>> {
        if (symbols.isEmpty()) return Result.Success(emptyList())
        return repository.fetchQuotes(symbols)
    }
}

// ── SearchTickersUseCase ──────────────────────────────────────────────────

class SearchTickersUseCase @Inject constructor(
    private val repository: StockRepository
) {
    suspend operator fun invoke(query: String): Result<List<Ticker>> {
        if (query.isBlank()) return Result.Success(emptyList())
        return repository.searchTickers(query.trim())
    }
}

// ── ObserveWatchedSymbolsUseCase ─────────────────────────────────────────

class ObserveWatchedSymbolsUseCase @Inject constructor(
    private val repository: StockRepository
) {
    operator fun invoke(): Flow<List<WatchedSymbol>> =
        repository.observeWatchedSymbols()
}

// ── AddWatchedSymbolUseCase ───────────────────────────────────────────────

class AddWatchedSymbolUseCase @Inject constructor(
    private val repository: StockRepository
) {
    suspend operator fun invoke(symbol: WatchedSymbol) =
        repository.addWatchedSymbol(symbol)
}

// ── RemoveWatchedSymbolUseCase ────────────────────────────────────────────

class RemoveWatchedSymbolUseCase @Inject constructor(
    private val repository: StockRepository
) {
    suspend operator fun invoke(symbol: String) =
        repository.removeWatchedSymbol(symbol)
}
