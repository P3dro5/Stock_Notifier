package com.stockwidget.worker

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.stockwidget.ui.widget.StockGlanceWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
@HiltWorker
class StockRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Force a fresh data fetch then update all widget instances
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                com.stockwidget.ui.widget.WidgetEntryPoint::class.java
            )
            val getQuotes      = entryPoint.getStockQuotesUseCase()
            val observeWatched = entryPoint.observeWatchedSymbolsUseCase()

            val watched = observeWatched().first()
            if (watched.isNotEmpty()) {
                getQuotes(watched.map { it.symbol }) // pre-warm cache
            }

            StockGlanceWidget().updateAll(applicationContext)
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
    companion object {
        private const val PERIODIC_WORK_NAME = "stock_widget_refresh_periodic"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Immediate one-time run on app start
            val oneTimeRequest = OneTimeWorkRequestBuilder<StockRefreshWorker>()
                .setConstraints(constraints)
                .build()

            // Periodic every 15 minutes (Android minimum)
            val periodicRequest = PeriodicWorkRequestBuilder<StockRefreshWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).apply {
                enqueue(oneTimeRequest)
                enqueueUniquePeriodicWork(
                    PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    periodicRequest
                )
            }
        }
    }
}