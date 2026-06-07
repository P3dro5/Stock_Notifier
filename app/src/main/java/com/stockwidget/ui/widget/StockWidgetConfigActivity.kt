package com.stockwidget.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stockwidget.ui.screens.StockDashboardScreen
import com.stockwidget.ui.theme.StockWidgetTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StockWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Must set RESULT_CANCELED first — if user backs out, widget won't be added
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            StockWidgetTheme {
                Scaffold { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        // Show dashboard so user can manage stocks before confirming
                        Box(modifier = Modifier.weight(1f)) {
                            StockDashboardScreen()
                        }

                        HorizontalDivider()

                        Box(
                            modifier         = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick  = { confirmWidget() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Add Widget to Home Screen")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun confirmWidget() {
        // Confirm the widget immediately — Glance will render asynchronously
        val result = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, result)
        finish()
    }
}