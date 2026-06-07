package com.stockwidget.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.stockwidget.BuildConfig
import com.stockwidget.data.api.FinnhubApiService
import com.stockwidget.data.api.YahooSearchService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().setLenient().create()

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(logging: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    // ── Finnhub ───────────────────────────────────────────────────────────

    @Provides
    @Singleton
    @Named("finnhub")
    fun provideFinnhubRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://finnhub.io/api/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideFinnhubApi(@Named("finnhub") retrofit: Retrofit): FinnhubApiService =
        retrofit.create(FinnhubApiService::class.java)

    // ── Yahoo Finance (search fallback) ───────────────────────────────────

    @Provides
    @Singleton
    @Named("yahoo")
    fun provideYahooRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://query2.finance.yahoo.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideYahooSearchService(@Named("yahoo") retrofit: Retrofit): YahooSearchService =
        retrofit.create(YahooSearchService::class.java)
}