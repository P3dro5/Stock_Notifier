package com.stockwidget.ui.widget

import com.stockwidget.domain.usecase.GetStockQuotesUseCase
import com.stockwidget.domain.usecase.ObserveWatchedSymbolsUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun getStockQuotesUseCase(): GetStockQuotesUseCase
    fun observeWatchedSymbolsUseCase(): ObserveWatchedSymbolsUseCase
}
