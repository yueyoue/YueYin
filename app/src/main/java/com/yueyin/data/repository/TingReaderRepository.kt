package com.yueyin.data.repository

import com.yueyin.data.api.TingReaderApiClient
import com.yueyin.data.api.TingReaderApi
import com.yueyin.data.model.*

class TingReaderRepository {
    private var api: TingReaderApi? = null
    private var serverUrl: String = ""
    private var username: String = ""
    private var password: String = ""
    private var token: String = ""

    fun configure(baseUrl: String, user: String, pass: String) {
        serverUrl = baseUrl; username = user; password = pass
        api = TingReaderApiClient.getApi(baseUrl)
    }

    fun getStreamUrl(chapterId: String): String = TingReaderApiClient.getStreamUrl(serverUrl, chapterId)
    fun getCoverUrl(coverUrl: String?): String? {
        if (coverUrl.isNullOrBlank()) return null
        return TingReaderApiClient.getCoverProxyUrl(serverUrl, coverUrl)
    }
    fun getAuthToken(): String = "Bearer $token"

    suspend fun ping(): Result<Unit> {
        return try {
            // Use OkHttp for reliable HTTP connectivity (handles cleartext HTTP better than HttpURLConnection)
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val url = "${TingReaderApiClient.normalizeUrl(serverUrl)}api/stats"
            val request = okhttp3.Request.Builder()
                .url(url)
                .header("User-Agent", "YueYin-Android/1.1.1")
                .get()
                .build()
            val response = client.newCall(request).execute()
            response.close()
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("服务器返回错误: ${response.code}"))
        } catch (e: Exception) {
            Result.failure(Exception(when {
                e is java.net.SocketTimeoutException -> "连接超时，请检查网络"
                e is okhttp3.internal.connection.RouteException ||
                e.message?.contains("Unable to resolve host", true) == true -> "无法解析服务器地址，请检查地址是否正确"
                e.message?.contains("Connection refused", true) == true -> "连接被拒绝，服务器可能未启动"
                e.message?.contains("SSL", true) == true -> "SSL连接错误"
                e.message?.contains("canceled", true) == true -> "连接被取消"
                e.message?.contains("connect", true) == true -> "无法连接到服务器，请检查网络和服务器地址"
                else -> "连接失败: ${e.message ?: "未知错误"}"
            }))
        }
    }

    suspend fun login(): Result<Unit> = try {
        val response = api!!.login(TingLoginRequest(username, password))
        if (response.token.isNotBlank()) { token = response.token; Result.success(Unit) }
        else Result.failure(Exception("登录失败"))
    } catch (e: Exception) { Result.failure(Exception(when {
        e.message?.contains("401") == true -> "用户名或密码错误"
        else -> e.message ?: "登录失败"
    })) }

    suspend fun getBooks(): Result<List<TingBook>> = try { Result.success(api!!.getBooks("Bearer $token")) } catch (e: Exception) { Result.failure(e) }
    suspend fun getBook(id: String): Result<TingBook> = try { Result.success(api!!.getBook("Bearer $token", id)) } catch (e: Exception) { Result.failure(e) }
    suspend fun getChapters(bookId: String): Result<List<TingChapter>> = try { Result.success(api!!.getChapters("Bearer $token", bookId)) } catch (e: Exception) { Result.failure(e) }
    suspend fun getRecentProgress(): Result<List<TingProgress>> = try { Result.success(api!!.getRecentProgress("Bearer $token")) } catch (e: Exception) { Result.failure(e) }
    suspend fun getBookProgress(bookId: String): Result<TingProgress?> = try { Result.success(api!!.getBookProgress("Bearer $token", bookId)) } catch (e: Exception) { Result.failure(e) }
    suspend fun saveProgress(bookId: String, chapterId: String, position: Double, duration: Double): Result<Unit> = try { api!!.saveProgress("Bearer $token", TingProgressRequest(bookId, chapterId, position, duration)); Result.success(Unit) } catch (e: Exception) { Result.failure(e) }
    suspend fun getFavorites(): Result<List<TingFavorite>> = try { Result.success(api!!.getFavorites("Bearer $token")) } catch (e: Exception) { Result.failure(e) }
    suspend fun addFavorite(bookId: String): Result<Unit> = try { api!!.addFavorite("Bearer $token", bookId); Result.success(Unit) } catch (e: Exception) { Result.failure(e) }
    suspend fun removeFavorite(bookId: String): Result<Unit> = try { api!!.removeFavorite("Bearer $token", bookId); Result.success(Unit) } catch (e: Exception) { Result.failure(e) }
    suspend fun searchBooks(query: String): Result<List<TingBook>> = try { Result.success(api!!.searchBooks("Bearer $token", query)) } catch (e: Exception) { Result.failure(e) }
    suspend fun getStats(): Result<TingStats> = try { Result.success(api!!.getStats("Bearer $token")) } catch (e: Exception) { Result.failure(e) }
}
