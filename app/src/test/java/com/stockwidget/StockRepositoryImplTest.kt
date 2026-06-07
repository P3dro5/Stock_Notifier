package com.stockwidget

import com.stockwidget.data.api.FinnhubApiService
import com.stockwidget.data.api.YahooSearchService
import com.stockwidget.data.repository.StockPreferencesDataSource
import com.stockwidget.data.repository.StockRepositoryImpl
import com.stockwidget.domain.model.Result
import com.stockwidget.domain.model.WatchedSymbol
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class StockRepositoryImplTest {
    private val api   = mockk<FinnhubApiService>()

    private val yahooSearchApi = mockk<YahooSearchService>()
    private val prefs = mockk<StockPreferencesDataSource>()

    private lateinit var repository: StockRepositoryImpl

    @Before
    fun setUp() {
        every { prefs.observeWatchedSymbols() } returns flowOf(
            listOf(WatchedSymbol("AAPL", "Apple Inc."))
        )
        repository = StockRepositoryImpl(api, yahooSearchApi, prefs)
    }

    @Test
    fun `fetchQuotes returns empty list when symbols list is empty`() = runTest {
        val result = repository.fetchQuotes(emptyList())
        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data.isEmpty())
    }

    @Test
    fun `addWatchedSymbol delegates to prefs`() = runTest {
        val current = mutableListOf<WatchedSymbol>()
        every { prefs.observeWatchedSymbols() } returns flowOf(current)
        coJustRun { prefs.saveWatchedSymbols(any()) }

        repository.addWatchedSymbol(WatchedSymbol("MSFT", "Microsoft"))

        coVerify { prefs.saveWatchedSymbols(any()) }
    }

    @Test
    fun `removeWatchedSymbol removes correct symbol`() = runTest {
        every { prefs.observeWatchedSymbols() } returns flowOf(
            listOf(
                WatchedSymbol("AAPL", "Apple"),
                WatchedSymbol("MSFT", "Microsoft")
            )
        )
        val saved = slot<List<WatchedSymbol>>()
        coEvery { prefs.saveWatchedSymbols(capture(saved)) } just Runs

        repository.removeWatchedSymbol("AAPL")

        assertEquals(1, saved.captured.size)
        assertEquals("MSFT", saved.captured[0].symbol)
    }
}
