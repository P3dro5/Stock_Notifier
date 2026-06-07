cmake_minimum_required(VERSION 3.22.1)
project("stockwidget")

if(NOT DEFINED FINNHUB_API_KEY)
    set(FINNHUB_API_KEY "")
endif()

add_definitions(-DFINNHUB_API_KEY="${FINNHUB_API_KEY}")

add_library(
        stockwidget
        SHARED
        secrets.cpp
)

find_library(log-lib log)
target_link_libraries(stockwidget ${log-lib})

# 16 KB page alignment — required for Android 15+ (API 35+) devices
target_link_options(stockwidget PRIVATE "-Wl,-z,max-page-size=16384")