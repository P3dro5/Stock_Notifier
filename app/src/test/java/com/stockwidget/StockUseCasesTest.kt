package com.stockwidget

import com.stockwidget.domain.model.Result
import com.stockwidget.domain.model.StockQuote
import com.stockwidget.domain.model.Ticker
import com.stockwidget.domain.model.WatchedSymbol
import com.stockwidget.domain.repository.StockRepository
import com.stockwidget.domain.usecase.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class StockUseCasesTest {

    private val repository = mockk<StockRepository>()

    private lateinit var getStockQuotes: GetStockQuotesUseCase
    private lateinit var searchTickers: SearchTickersUseCase
    private lateinit var addWatchedSymbol: AddWatchedSymbolUseCase
    private lateinit var removeWatchedSymbol: RemoveWatchedSymbolUseCase

    @Before
    fun setUp() {
        getStockQuotes    = GetStockQuotesUseCase(repository)
        searchTickers     = SearchTickersUseCase(repository)
        addWatchedSymbol  = AddWatchedSymbolUseCase(repository)
        removeWatchedSymbol = RemoveWatchedSymbolUseCase(repository)
    }

    // ── GetStockQuotesUseCase ─────────────────────────────────────────────────

    @Test
    fun `getStockQuotes returns empty list for empty symbols`() = runTest {
        val result = getStockQuotes(emptyList())
        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data.isEmpty())
        coVerify(exactly = 0) { repository.fetchQuotes(any()) }
    }

    @Test
    fun `getStockQuotes delegates to repository`() = runTest {
        val expected = listOf(mockk<StockQuote>())
        coEvery { repository.fetchQuotes(listOf("AAPL")) } returns Result.Success(expected)

        val result = getStockQuotes(listOf("AAPL"))

        assertTrue(result is Result.Success)
        assertEquals(expected, (result as Result.Success).data)
    }

    // ── SearchTickersUseCase ──────────────────────────────────────────────────

    @Test
    fun `searchTickers returns empty list for blank query`() = runTest {
        val result = searchTickers("")
        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data.isEmpty())
        coVerify(exactly = 0) { repository.searchTickers(any()) }
    }

    @Test
    fun `searchTickers trims query before delegating`() = runTest {
        val expected = listOf(Ticker("AAPL", "Apple", "XNGS", "US"))
        coEvery { repository.searchTickers("AAPL") } returns Result.Success(expected)

        val result = searchTickers("  AAPL  ")

        coVerify { repository.searchTickers("AAPL") }
        assertEquals(expected, (result as Result.Success).data)
    }

    // ── AddWatchedSymbolUseCase ───────────────────────────────────────────────

    @Test
    fun `addWatchedSymbol delegates to repository`() = runTest {
        val symbol = WatchedSymbol("TSLA", "Tesla")
        coJustRun { repository.addWatchedSymbol(symbol) }

        addWatchedSymbol(symbol)

        coVerify(exactly = 1) { repository.addWatchedSymbol(symbol) }
    }

    // ── RemoveWatchedSymbolUseCase ────────────────────────────────────────────

    @Test
    fun `removeWatchedSymbol delegates to repository`() = runTest {
        coJustRun { repository.removeWatchedSymbol("TSLA") }

        removeWatchedSymbol("TSLA")

        coVerify(exactly = 1) { repository.removeWatchedSymbol("TSLA") }
    }
}
