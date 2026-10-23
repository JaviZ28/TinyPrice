package com.apps.tinyprice.data.remote

import com.apps.tinyprice.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object TinyPriceApiFactory {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    fun create(): TinyPriceApi {
        val baseUrl = BuildConfig.EDGE_RUNTIME_BASE_URL.let {
            if (it.endsWith("/")) it else "$it/"
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TinyPriceApi::class.java)
    }
}