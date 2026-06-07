package com.stockwidget.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.stockwidget.domain.model.StockQuote
import com.stockwidget.domain.model.WatchedSymbol
import com.stockwidget.domain.model.Result
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.util.Locale

// ── Receiver ──────────────────────────────────────────────────────────────────

class StockWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StockGlanceWidget()
}

// ── GlanceAppWidget ───────────────────────────────────────────────────────────

class StockGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val getQuotes      = entryPoint.getStockQuotesUseCase()
        val observeWatched = entryPoint.observeWatchedSymbolsUseCase()

        val watched = try {
            observeWatched().first()
        } catch (_: Exception) {
            emptyList()
        }

        val symbols  = watched.map { it.symbol }
        val nameMap  = watched.associate { it.symbol to it.displayName }

        val quotes: List<StockQuote> = if (symbols.isNotEmpty()) {
            try {
                when (val result = getQuotes(symbols)) {
                    is Result.Success -> result.data
                    else              -> emptyList()
                }
            } catch (_: Exception) {
                emptyList()
            }
        } else emptyList()

        provideContent {
            WidgetContent(watched = watched, quotes = quotes, nameMap = nameMap)
        }
    }
}

// ── Widget UI (Glance Composable) ─────────────────────────────────────────────

@SuppressLint("RestrictedApi")
@Composable
private fun WidgetContent(
    watched: List<WatchedSymbol>,
    quotes: List<StockQuote>,
    nameMap: Map<String, String>
) {
    val quotesMap = quotes.associateBy { it.symbol }

    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF0D1117))
                .padding(12.dp)
        ) {
            // Header
            Row(
                modifier              = GlanceModifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = "📈 Stocks",
                    style = TextStyle(
                        color      = ColorProvider(Color.White),
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Image(
                    provider            = ImageProvider(android.R.drawable.ic_menu_rotate),
                    contentDescription  = "Refresh",
                    modifier            = GlanceModifier
                        .size(18.dp)
                        .clickable(actionRunCallback<RefreshWidgetAction>())
                )
            }

            Spacer(GlanceModifier.height(8.dp))

            if (watched.isEmpty()) {
                Text(
                    text  = "Add stocks in the app",
                    style = TextStyle(color = ColorProvider(Color(0xFF8B949E)), fontSize = 12.sp)
                )
            } else {
                watched.forEach { ws ->
                    val quote = quotesMap[ws.symbol]
                    WidgetStockRow(
                        symbol      = ws.symbol,
                        name        = nameMap[ws.symbol] ?: ws.displayName,
                        quote       = quote
                    )
                    Spacer(GlanceModifier.height(6.dp))
                }
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun WidgetStockRow(symbol: String, name: String, quote: StockQuote?) {
    val priceText  = quote?.let { formatPrice(it.price) } ?: "—"
    val changeText = quote?.let {
        val sign = if (it.isPositive) "+" else ""
        "$sign${"%.2f".format(it.changePercent)}%"
    } ?: "—"
    val changeColor = when {
        quote == null       -> Color(0xFF8B949E)
        quote.isPositive    -> Color(0xFF00C853)
        else                -> Color(0xFFFF1744)
    }

    Row(
        modifier          = GlanceModifier
            .fillMaxWidth()
            .background(Color(0xFF161B22))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text  = symbol,
                style = TextStyle(
                    color      = ColorProvider(Color.White),
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text  = name,
                style = TextStyle(color = ColorProvider(Color(0xFF8B949E)), fontSize = 10.sp)
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text  = priceText,
                style = TextStyle(
                    color      = ColorProvider(Color.White),
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text  = changeText,
                style = TextStyle(color = ColorProvider(changeColor), fontSize = 11.sp)
            )
        }
    }
}

private fun formatPrice(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 2 }.format(value)

// ── Refresh action ────────────────────────────────────────────────────────────

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        StockGlanceWidget().update(context, glanceId)
    }
}
