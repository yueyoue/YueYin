package com.lechenmusic.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object TingReaderApiClient {
    private var retrofit: Retrofit? = null
    private var api: TingReaderApi? = null
    private var currentBaseUrl: String? = null

    fun getApi(baseUrl: String): TingReaderApi {
        val normalizedUrl = normalizeUrl(baseUrl)
        if (retrofit == null || currentBaseUrl != normalizedUrl) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            val gson = com.google.gson.GsonBuilder()
                .setLenient()
                .create()

            retrofit = Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(client)
                .addConverterFactory(SafeJsonConverterFactory(GsonConverterFactory.create(gson)))
                .build()
            currentBaseUrl = normalizedUrl
        }
        api = retrofit!!.create(TingReaderApi::class.java)
        return api!!
    }

    fun getStreamUrl(baseUrl: String, chapterId: String, token: String): String {
        val normalizedUrl = normalizeUrl(baseUrl)
        return "${normalizedUrl}api/stream/$chapterId"
    }

    fun getCoverProxyUrl(baseUrl: String, coverUrl: String, token: String): String {
        val normalizedUrl = normalizeUrl(baseUrl)
        val encodedUrl = java.net.URLEncoder.encode(coverUrl, "UTF-8")
        return "${normalizedUrl}api/proxy/cover?url=$encodedUrl"
    }

    private fun normalizeUrl(url: String): String {
        var normalized = url.trim()
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "http://$normalized"
        }
        if (!normalized.endsWith("/")) {
            normalized = "$normalized/"
        }
        return normalized
    }
}
