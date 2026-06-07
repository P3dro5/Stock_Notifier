package com.stockwidget.data.model

import com.google.gson.annotations.SerializedName

data class YahooSearchResponse(
    @SerializedName("quotes") val quotes: List<YahooQuoteDto>?
)

data class YahooQuoteDto(
    @SerializedName("symbol")       val symbol: String?,
    @SerializedName("shortname")    val shortName: String?,
    @SerializedName("longname")     val longName: String?,
    @SerializedName("exchDisp")     val exchange: String?,
    @SerializedName("typeDisp")     val type: String?
)