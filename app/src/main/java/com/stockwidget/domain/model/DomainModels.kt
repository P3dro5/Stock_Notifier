package com.stockwidget.domain.model

/**
 * Clean domain model for a stock quote – decoupled from the API DTO.
 */
data class StockQuote(
    val symbol: String,
    val name: String,           // resolved from ticker search or stored preference
    val price: Double,          // latest close price
    val open: Double,
    val high: Double,
    val low: Double,
    val volume: Double,
    val change: Double,         // price - open
    val changePercent: Double,  // ((price - open) / open) * 100
    val date: String,           // ISO date string
    val exchange: String
) {
    val isPositive: Boolean get() = change >= 0.0
}

/**
 * A ticker the user has searched for / wants to watch.
 */
data class Ticker(
    val symbol: String,
    val name: String,
    val exchange: String,
    val country: String
)

/**
 * Persisted user preference for which symbols to watch.
 */
data class WatchedSymbol(
    val symbol: String,
    val displayName: String,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Sealed result wrapper – keeps coroutine error handling clean.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
