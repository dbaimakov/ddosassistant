package com.ddosassistant.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private fun okHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // Use BASIC by default; change to BODY during dev only.
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    fun graphService(): Pair<GraphApiService, OkHttpClient> {
        val client = okHttpClient()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://graph.microsoft.com/v1.0/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(GraphApiService::class.java) to client
    }
}
