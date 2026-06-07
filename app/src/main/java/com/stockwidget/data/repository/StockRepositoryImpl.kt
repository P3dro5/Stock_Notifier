package com.stockwidget.data.repository

import com.stockwidget.data.api.FinnhubApiService
import com.stockwidget.data.api.YahooSearchService
import com.stockwidget.domain.model.Result
import com.stockwidget.domain.model.StockQuote
import com.stockwidget.domain.model.Ticker
import com.stockwidget.domain.model.WatchedSymbol
import com.stockwidget.domain.repository.StockRepository
import com.stockwidget.security.Secrets
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockRepositoryImpl @Inject constructor(
    private val finnhubApi: FinnhubApiService,
    private val yahooApi: YahooSearchService,
    private val prefs: StockPreferencesDataSource
) : StockRepository {

    override suspend fun fetchQuotes(symbols: List<String>): Result<List<StockQuote>> {
        if (symbols.isEmpty()) return Result.Success(emptyList())
        return try {
            val watched = prefs.observeWatchedSymbols().first()
            val nameMap = watched.associate { it.symbol.uppercase() to it.displayName }

            // Fetch all symbols concurrently — one call per symbol
            val quotes = coroutineScope {
                symbols.map { symbol ->
                    async {
                        try {
                            val response = finnhubApi.getQuote(
                                symbol = symbol.uppercase(),
                                token  = Secrets.getFinnhubApiKey()
                            )
                            if (response.isSuccessful) {
                                val dto = response.body()
                                // Finnhub returns all zeros when symbol is not found
                                if (dto == null || (dto.currentPrice == 0.0 && dto.open == 0.0)) {
                                    return@async null
                                }
                                val price     = dto.currentPrice ?: 0.0
                                val open      = dto.open ?: price
                                val change    = dto.change ?: (price - open)
                                val changePct = dto.changePercent ?: 0.0

                                StockQuote(
                                    symbol        = symbol.uppercase(),
                                    name          = nameMap[symbol.uppercase()] ?: symbol,
                                    price         = price,
                                    open          = open,
                                    high          = dto.high ?: price,
                                    low           = dto.low ?: price,
                                    volume        = 0.0,
                                    change        = change,
                                    changePercent = changePct,
                                    date          = dto.timestamp?.let {
                                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
                                            .apply { timeZone = java.util.TimeZone.getDefault() }
                                            .format(java.util.Date(it * 1000))
                                    } ?: java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                        .format(java.util.Date()),
                                    exchange      = ""
                                )
                            } else null
                        } catch (_: Exception) {
                            null
                        }
                    }
                }.awaitAll().filterNotNull()
            }

            if (quotes.isEmpty() && symbols.isNotEmpty()) {
                Result.Error("No data returned. Market may be closed or symbols invalid.")
            } else {
                Result.Success(quotes)
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Unknown network error")
        }
    }

    override suspend fun searchTickers(query: String): Result<List<Ticker>> {
        return try {
            // Try Finnhub search first
            val response = finnhubApi.searchSymbol(
                query = query,
                token = Secrets.getFinnhubApiKey()
            )
            if (response.isSuccessful) {
                val tickers = response.body()?.result
                    ?.filter { it.type == "Common Stock" && it.symbol != null }
                    ?.map {
                        Ticker(
                            symbol   = it.symbol!!,
                            name     = it.description ?: it.symbol,
                            exchange = "",
                            country  = ""
                        )
                    } ?: emptyList()
                Result.Success(tickers)
            } else {
                // Fall back to Yahoo search
                searchViaYahoo(query)
            }
        } catch (_: Exception) {
            searchViaYahoo(query)
        }
    }

    private suspend fun searchViaYahoo(query: String): Result<List<Ticker>> {
        return try {
            val response = yahooApi.searchTickers(query = query)
            if (response.isSuccessful) {
                val tickers = response.body()?.quotes
                    ?.filter { it.symbol != null && it.type == "Equity" }
                    ?.map {
                        Ticker(
                            symbol   = it.symbol!!,
                            name     = it.longName ?: it.shortName ?: it.symbol,
                            exchange = it.exchange ?: "",
                            country  = ""
                        )
                    } ?: emptyList()
                Result.Success(tickers)
            } else {
                Result.Error("Search unavailable")
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Search failed")
        }
    }

    override fun observeWatchedSymbols(): Flow<List<WatchedSymbol>> =
        prefs.observeWatchedSymbols()

    override suspend fun addWatchedSymbol(symbol: WatchedSymbol) {
        val current = prefs.observeWatchedSymbols().first().toMutableList()
        if (current.none { it.symbol == symbol.symbol }) {
            current.add(symbol)
            prefs.saveWatchedSymbols(current)
        }
    }

    override suspend fun removeWatchedSymbol(symbolId: String) {
        val current = prefs.observeWatchedSymbols().first().toMutableList()
        current.removeAll { it.symbol == symbolId }
        prefs.saveWatchedSymbols(current)
    }

    override suspend fun setWatchedSymbols(symbols: List<WatchedSymbol>) {
        prefs.saveWatchedSymbols(symbols)
    }
}