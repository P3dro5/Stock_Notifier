package com.stockwidget.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stockwidget.domain.model.StockQuote
import com.stockwidget.domain.model.Ticker
import com.stockwidget.domain.model.WatchedSymbol
import com.stockwidget.ui.theme.GainGreen
import com.stockwidget.ui.theme.LossRed
import com.stockwidget.ui.viewmodel.StockDashboardViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDashboardScreen(
    viewModel: StockDashboardViewModel = hiltViewModel()
) {
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val searchState    by viewModel.searchState.collectAsStateWithLifecycle()
    val watchedSymbols by viewModel.watchedSymbols.collectAsStateWithLifecycle()

    var showSearch by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "StockWidget",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    // Live indicator
                    if (dashboardState.isLive) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(GainGreen, shape = RoundedCornerShape(4.dp))
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text  = "LIVE",
                                color = GainGreen,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(
                            if (showSearch) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = if (showSearch) "Close search" else "Add symbol"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Search bar ─────────────────────────────────────────────
            if (showSearch) {
                SearchSection(
                    query         = searchState.query,
                    results       = searchState.results,
                    isSearching   = searchState.isSearching,
                    error         = searchState.searchError,
                    watchedList   = watchedSymbols,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    onSubmit      = viewModel::submitSearch,
                    onAddSymbol   = { ticker ->
                        viewModel.addSymbol(ticker)
                        keyboard?.hide()
                    },
                )
                HorizontalDivider()
            }

            // ── Dashboard content ───────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    dashboardState.isLoading && dashboardState.quotes.isEmpty() -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    watchedSymbols.isEmpty() -> {
                        EmptyState(onAddClick = { showSearch = true })
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Error banner
                            dashboardState.error?.let { err ->
                                item {
                                    ErrorBanner(message = err)
                                }
                            }
                            // Loading indicator when refreshing existing data
                            if (dashboardState.isLoading) {
                                item {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                            // Quote cards — fall back to watched symbols if quotes not yet loaded
                            watchedSymbols.map { it.symbol }.toSet()
                            val quotesMap = dashboardState.quotes.associateBy { it.symbol }

                            items(watchedSymbols, key = { it.symbol }) { watched ->
                                val quote = quotesMap[watched.symbol]
                                SwipeToDismissCard(
                                    onDismiss = { viewModel.removeSymbol(watched.symbol) }
                                ) {
                                    if (quote != null) {
                                        StockQuoteCard(quote = quote)
                                    } else {
                                        PlaceholderCard(
                                            symbol = watched.symbol,
                                            name   = watched.displayName
                                        )
                                    }
                                }
                            }

                            dashboardState.lastUpdated?.let { ts ->
                                item {
                                    Text(
                                        text  = if (dashboardState.isLive)
                                            "● Live · ${formatTime(ts)}"
                                        else
                                            "Last updated: ${formatTime(ts)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (dashboardState.isLive) GainGreen
                                        else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDismissCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart ||
                value == SwipeToDismissBoxValue.StartToEnd) {
                onDismiss()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state            = dismissState,
        modifier         = modifier,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.surfaceVariant
                    else                           -> LossRed
                },
                label = "swipe_color"
            )
            Box(
                modifier          = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment  = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector        = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint               = Color.White
                )
            }
        }
    ) {
        content()
    }
}

// ── Search Section ────────────────────────────────────────────────────────────

@Composable
private fun SearchSection(
    query: String,
    results: List<Ticker>,
    isSearching: Boolean,
    error: String?,
    watchedList: List<WatchedSymbol>,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onAddSymbol: (Ticker) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value         = query,
            onValueChange = onQueryChange,
            label         = { Text("Type exact symbol e.g. AAPL") },
            leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon  = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine      = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            modifier        = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick  = onSubmit,
            enabled  = query.isNotBlank() && !isSearching,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSearching) "Searching..." else "Search Symbol")
        }

        if (isSearching) {
            LinearProgressIndicator(modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp))
        }

        error?.let {
            Text(
                text  = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        val watchedSet = watchedList.map { it.symbol }.toSet()
        results.forEach { ticker ->
            val alreadyAdded = ticker.symbol in watchedSet
            ListItem(
                headlineContent   = { Text(ticker.symbol, fontWeight = FontWeight.SemiBold) },
                supportingContent = { Text(ticker.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                trailingContent   = {
                    if (alreadyAdded) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Already added",
                            tint = GainGreen
                        )
                    } else {
                        IconButton(onClick = { onAddSymbol(ticker) }) {
                            Icon(Icons.Default.Add, contentDescription = "Add ${ticker.symbol}")
                        }
                    }
                }
            )
            HorizontalDivider(thickness = 0.5.dp)
        }
    }
}

// ── Stock Quote Card ──────────────────────────────────────────────────────────

@Composable
fun StockQuoteCard(
    quote: StockQuote,
    modifier: Modifier = Modifier
) {
    val changeColor = if (quote.isPositive) GainGreen else LossRed
    val sign        = if (quote.isPositive) "+" else ""

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = quote.symbol,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text     = quote.name,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text  = quote.exchange,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Column(
                modifier            = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MiniStat("H", formatPrice(quote.high))
                MiniStat("L", formatPrice(quote.low))
                MiniStat("V", formatVolume(quote.volume))
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier            = Modifier.weight(1f)
            ) {
                Text(
                    text       = formatPrice(quote.price),
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text       = "$sign${formatPrice(quote.change)}",
                    color      = changeColor,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    color = changeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text       = "$sign${"%.2f".format(quote.changePercent)}%",
                        color      = changeColor,
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.labelSmall)
    }
}

// ── Placeholder card (while loading) ─────────────────────────────────────────

@Composable
private fun PlaceholderCard(symbol: String, name: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(symbol, fontWeight = FontWeight.Bold)
                Text(name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(onAddClick: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.TrendingUp,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint     = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text  = "No stocks yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text  = "Tap + to search and add stocks to\nyour dashboard and home-screen widget",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAddClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add Stock")
        }
    }
}

// ── Error banner ──────────────────────────────────────────────────────────────

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        color  = MaterialTheme.colorScheme.errorContainer,
        shape  = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(8.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

// ── Formatters ────────────────────────────────────────────────────────────────

private fun formatPrice(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 2 }.format(value)

private fun formatVolume(value: Double): String =
    when {
        value >= 1_000_000_000 -> "${"%.1f".format(value / 1_000_000_000)}B"
        value >= 1_000_000     -> "${"%.1f".format(value / 1_000_000)}M"
        value >= 1_000         -> "${"%.1f".format(value / 1_000)}K"
        else                   -> value.toLong().toString()
    }

private fun formatTime(ts: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(java.util.Date(ts))
}
