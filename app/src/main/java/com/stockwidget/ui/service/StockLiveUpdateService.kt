package com.stockwidget.ui.service

import com.stockwidget.data.manager.FinnhubWebSocketManager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.stockwidget.domain.usecase.GetStockQuotesUseCase
import com.stockwidget.domain.usecase.ObserveWatchedSymbolsUseCase
import com.stockwidget.ui.screens.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class StockLiveUpdateService : Service() {

    @Inject lateinit var webSocketManager: FinnhubWebSocketManager
    @Inject lateinit var observeWatchedSymbols: ObserveWatchedSymbolsUseCase
    @Inject lateinit var getStockQuotes: GetStockQuotesUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // symbol -> latest price
    private val latestPrices = mutableMapOf<String, Double>()
    private val latestChangePct = mutableMapOf<String, Double>()
    private val openPrices      = mutableMapOf<String, Double>()

    companion object {
        const val CHANNEL_ID   = "stock_live_channel"
        const val NOTIF_ID     = 1001
        const val ACTION_STOP  = "com.stockwidget.STOP_LIVE"

        fun start(context: Context) {
            val intent = Intent(context, StockLiveUpdateService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, StockLiveUpdateService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("Connecting…", emptyMap(), emptyMap()))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        scope.launch {
            // Initial REST fetch to populate prices immediately
            val watched = observeWatchedSymbols().first()
            val symbols = watched.map { it.symbol }

            if (symbols.isEmpty()) {
                stopSelf()
                return@launch
            }

            // Connect WebSocket and subscribe
            webSocketManager.connect()
            webSocketManager.subscribeAll(symbols)

            // Pre-populate from REST
            val result = getStockQuotes(symbols)
            if (result is com.stockwidget.domain.model.Result.Success) {
                result.data.forEach { quote ->
                    latestPrices[quote.symbol]    = quote.price
                    latestChangePct[quote.symbol] = quote.changePercent
                    openPrices[quote.symbol]      = quote.open  // add this
                }
                updateNotification()
            }

            // Listen to live WebSocket trades
            webSocketManager.trades.collect { trade ->
                latestPrices[trade.symbol] = trade.price
                // Recalculate change % from open price
                val open = openPrices[trade.symbol]
                if (open != null && open != 0.0) {
                    latestChangePct[trade.symbol] = ((trade.price - open) / open) * 100.0
                }
                updateNotification()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        webSocketManager.disconnect()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, buildNotification(null, latestPrices, latestChangePct))
    }

    private fun buildNotification(
        statusText: String?,
        prices: Map<String, Double>,
        changePcts: Map<String, Double>
    ): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, StockLiveUpdateService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentText = statusText
            ?: if (prices.isEmpty()) {
                "Waiting for data…"
            } else {
                prices.entries.joinToString(separator = "  •  ", prefix = "  •  ") { (sym, price) ->
                    val pct   = changePcts[sym] ?: 0.0
                    val sign  = if (pct >= 0) "\uD83D\uDFE9 " else "\uD83D\uDFE5 "
                    "$sym  $${"%.2f".format(price)}  $sign${"%.2f".format(abs(pct))}%\n"
                }
            }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_sort_by_size)
            .setContentTitle("📈 Stock Tracker · LIVE")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(tapIntent)
            .addAction(android.R.drawable.ic_delete, "Stop", stopIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Live Stock Prices",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description       = "Shows real-time stock prices in the notification bar"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}