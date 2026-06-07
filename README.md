# StockWidget — Android Home Screen Stock Tracker

A fully-featured Android app that displays real-time stock data as a home screen widget,
built with modern Android architecture.

## Stack

| Layer        | Technology                                         |
|--------------|----------------------------------------------------|
| UI           | Jetpack Compose + Material 3                       |
| Widget       | Glance API (Compose-based App Widget)              |
| Architecture | MVVM + Clean Architecture (data / domain / ui)     |
| DI           | Hilt (Dagger)                                      |
| Async        | Kotlin Coroutines + Flow                           |
| Networking   | Retrofit 2 + OkHttp + Gson                        |
| Persistence  | DataStore Preferences                              |
| Background   | WorkManager (periodic widget refresh every 15 min) |
| Testing      | MockK + Turbine + JUnit 4 + kotlinx-coroutines-test|
| IDE          | Android Studio Panda 4 \| 2025.3.4 Patch 1        |
| Build        | AGP 9.0.0 · Kotlin 2.1.20 · Gradle 8.11.1        |
| Min SDK      | 26 (Android 8.0)                                   |
| Target SDK   | 36                                                 |

---

## Setup

### 1. Get a Marketstack API Key

1. Go to [https://marketstack.com/dashboard](https://marketstack.com/dashboard)
2. Sign up for a free account
3. Copy your **Access Key**

### 2. Open in Android Studio

- Open **Android Studio Panda 4 | 2025.3.4**
- File → Open → select the `StockWidget` folder
- Let Gradle sync complete
- Run on a device or emulator (API 26+)

---

## Project Structure

```
app/src/main/java/com/stockwidget/
├── data/
│   ├── api/
│   │   └── MarketstackApiService.kt      # Retrofit interface
│   ├── model/
│   │   └── MarketstackDtos.kt            # API response DTOs
│   └── repository/
│       ├── Mappers.kt                    # DTO → domain model
│       ├── StockPreferencesDataSource.kt # DataStore persistence
│       └── StockRepositoryImpl.kt        # Repository implementation
│
├── domain/
│   ├── model/
│   │   └── DomainModels.kt              # StockQuote, Ticker, WatchedSymbol, Result
│   ├── repository/
│   │   └── StockRepository.kt           # Repository interface
│   └── usecase/
│       └── StockUseCases.kt             # GetStockQuotes, SearchTickers, etc.
│
├── ui/
│   ├── screens/
│   │   ├── MainActivity.kt
│   │   └── StockDashboardScreen.kt      # Main Compose UI
│   ├── viewmodel/
│   │   └── StockDashboardViewModel.kt   # MVVM ViewModel
│   ├── widget/
│   │   ├── StockGlanceWidget.kt         # Glance home screen widget
│   │   ├── WidgetEntryPoint.kt          # Hilt entry point for widget
│   │   └── StockWidgetConfigActivity.kt # Widget config activity
│   └── theme/
│       └── Theme.kt                     # Material 3 dark theme
│
├── di/
│   ├── NetworkModule.kt                 # Retrofit, OkHttp
│   ├── RepositoryModule.kt              # Repository binding
│   └── DispatcherModule.kt              # Coroutine dispatchers
│
├── worker/
│   └── StockRefreshWorker.kt            # WorkManager periodic refresh
│
└── StockWidgetApp.kt                    # Application class (Hilt + WorkManager)
```

---

## Adding the Widget to Your Home Screen

1. Long-press any empty area on your Android home screen
2. Tap **Widgets**
3. Find **Stock Tracker Widget** in the list
4. Drag it to a spot on your home screen
5. The configuration screen will open — add your desired stocks and tap **Add Widget**
6. The widget refreshes every 15 minutes (WorkManager) or tap the refresh icon on the widget

---

## Notes on Free vs Paid Marketstack Plan

| Feature            | Free Plan      | Paid Plan     |
|--------------------|----------------|---------------|
| Requests/month     | 100            | 10,000+       |
| EOD (end-of-day)   | ✅ Yes         | ✅ Yes        |
| Latest prices      | EOD only       | Real-time     |
| Intraday data      | ❌ No          | ✅ Yes        |

The app uses the `/eod/latest` endpoint which returns the most recent end-of-day close on the free plan.

---

## Running Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```
