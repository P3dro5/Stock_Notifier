package com.stockwidget.data.manager

import com.stockwidget.data.model.FinnhubTradeMessage
import com.stockwidget.security.Secrets
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.*
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinnhubWebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private var webSocket: WebSocket? = null
    private val subscribedSymbols = mutableSetOf<String>()

    private val _trades = MutableSharedFlow<FinnhubTradeMessage>(extraBufferCapacity = 64)
    val trades: SharedFlow<FinnhubTradeMessage> = _trades

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            // Re-subscribe to all symbols on reconnect
            subscribedSymbols.forEach { symbol ->
                webSocket.send("""{"type":"subscribe","symbol":"$symbol"}""")
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val json = JSONObject(text)
                if (json.getString("type") == "trade") {
                    val data = json.getJSONArray("data")
                    for (i in 0 until data.length()) {
                        val trade = data.getJSONObject(i)
                        val msg = FinnhubTradeMessage(
                            symbol = trade.getString("s"),
                            price  = trade.getDouble("p"),
                            volume = trade.optDouble("v", 0.0),
                            time   = trade.optLong("t", 0L)
                        )
                        _trades.tryEmit(msg)
                    }
                }
            } catch (_: Exception) {}
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            // Reconnect after failure
            this@FinnhubWebSocketManager.webSocket = null
            connect()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            this@FinnhubWebSocketManager.webSocket = null
        }
    }

    fun connect() {
        if (webSocket != null) return
        val request = Request.Builder()
            .url("wss://ws.finnhub.io?token=${Secrets.getFinnhubApiKey()}")
            .build()
        webSocket = okHttpClient.newWebSocket(request, listener)
    }

    fun subscribe(symbol: String) {
        subscribedSymbols.add(symbol)
        webSocket?.send("""{"type":"subscribe","symbol":"$symbol"}""") ?: connect()
    }

    fun unsubscribe(symbol: String) {
        subscribedSymbols.remove(symbol)
        webSocket?.send("""{"type":"unsubscribe","symbol":"$symbol"}""")
    }

    fun subscribeAll(symbols: List<String>) {
        symbols.forEach { subscribe(it) }
    }

    fun disconnect() {
        webSocket?.close(1000, "Disconnected")
        webSocket = null
        subscribedSymbols.clear()
    }
}