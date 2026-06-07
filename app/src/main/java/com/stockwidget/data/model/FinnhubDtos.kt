package com.stockwidget.data.model

import com.google.gson.annotations.SerializedName
data class FinnhubQuoteDto(
    @SerializedName("c")  val currentPrice: Double?,   // Current price
    @SerializedName("d")  val change: Double?,          // Change (absolute, vs prev close)
    @SerializedName("dp") val changePercent: Double?,   // Change percent
    @SerializedName("h")  val high: Double?,            // High of the day
    @SerializedName("l")  val low: Double?,             // Low of the day
    @SerializedName("o")  val open: Double?,            // Open price
    @SerializedName("pc") val previousClose: Double?,   // Previous close
    @SerializedName("t")  val timestamp: Long?          // Unix timestamp
)

data class FinnhubSearchResponse(
    @SerializedName("count")  val count: Int?,
    @SerializedName("result") val result: List<FinnhubSymbolDto>?
)

data class FinnhubSymbolDto(
    @SerializedName("symbol")        val symbol: String?,
    @SerializedName("description")   val description: String?,
    @SerializedName("type")          val type: String?,
    @SerializedName("displaySymbol") val displaySymbol: String?
)

data class FinnhubTradeMessage(
    val symbol: String,
    val price:  Double,
    val volume: Double,
    val time:   Long
)