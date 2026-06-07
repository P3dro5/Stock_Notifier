package com.stockwidget.data.api

import com.stockwidget.data.model.FinnhubQuoteDto
import com.stockwidget.data.model.FinnhubSearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface FinnhubApiService {

    // Real-time quote: c=price, h=high, l=low, o=open, pc=prevClose, d=change, dp=changePct
    @GET("quote")
    suspend fun getQuote(
        @Query("symbol") symbol: String,
        @Query("token") token: String
    ): Response<FinnhubQuoteDto>

    // Symbol search — works on free tier
    @GET("search")
    suspend fun searchSymbol(
        @Query("q") query: String,
        @Query("token") token: String
    ): Response<FinnhubSearchResponse>
}