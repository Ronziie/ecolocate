package com.ecolocate.network

import android.content.Context
import com.ecolocate.Config
import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.random.Random

/**
 * Provides Retrofit API with OkHttp client and retry policy with exponential backoff + jitter.
 */
class NetworkClient(context: Context) {
    private val config = Config(context)

    private val retryInterceptor = Interceptor { chain ->
        var attempt = 0
        val maxAttempts = 5
        var waitMs = 1000L
        var lastError: IOException? = null

        while (attempt < maxAttempts) {
            try {
                val response = chain.proceed(chain.request())
                if (response.isSuccessful || response.code in 400..499) {
                    return@Interceptor response
                }
                response.close()
            } catch (e: IOException) {
                lastError = e
            }
            attempt += 1
            val jitter = Random.nextLong(0, 400)
            Thread.sleep(waitMs + jitter)
            waitMs = min(waitMs * 2, 60_000L)
        }
        throw lastError ?: IOException("request failed")
    }

    private val httpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor(retryInterceptor)
            .build()
    }

    private val moshi: Moshi by lazy { Moshi.Builder().build() }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(config.serverBaseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(httpClient)
            .build()
    }

    val api: TelemetryApi by lazy { retrofit.create(TelemetryApi::class.java) }
}


