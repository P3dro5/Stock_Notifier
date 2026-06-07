package com.stockwidget.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.stockwidget.domain.model.WatchedSymbol
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "stock_prefs")

@Singleton
class StockPreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val watchedKey = stringPreferencesKey("watched_symbols")

    fun observeWatchedSymbols(): Flow<List<WatchedSymbol>> =
        context.dataStore.data.map { prefs ->
            val json = prefs[watchedKey] ?: return@map emptyList()
            val type = object : TypeToken<List<WatchedSymbol>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        }

    suspend fun saveWatchedSymbols(symbols: List<WatchedSymbol>) {
        context.dataStore.edit { prefs ->
            prefs[watchedKey] = gson.toJson(symbols)
        }
    }
}
