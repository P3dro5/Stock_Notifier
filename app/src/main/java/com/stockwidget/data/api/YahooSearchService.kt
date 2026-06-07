package com.stockwidget.data.api

import com.stockwidget.data.model.YahooSearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface YahooSearchService {

    @GET("v1/finance/search")
    suspend fun searchTickers(
        @Query("q") query: String,
        @Query("quotesCount") quotesCount: Int = 20,
        @Query("newsCount") newsCount: Int = 0,
        @Query("enableFuzzyQuery") enableFuzzyQuery: Boolean = true,
        @Query("quotesQueryId") quotesQueryId: String = "tss_match_phrase_query"
    ): Response<YahooSearchResponse>
}