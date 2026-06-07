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
            // Lightweight connectivity check - just try to reach the server
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val url = "${TingReaderApiClient.normalizeUrl(serverUrl)}api/auth/login"
            val body = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("application/json"),
                "{}"
            )
            val request = okhttp3.Request.Builder().url(url).post(body).build()
            val response = client.newCall(request).execute()
            response.close()
            // Any response (even 400/401) means the server is reachable
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(mapConnectionError(e)))
        }
    }

    suspend fun login(): Result<Unit> = try {
        val response = api!!.login(TingLoginRequest(username, password))
        if (response.token.isNotBlank()) { token = response.token; Result.success(Unit) }
        else Result.failure(Exception("登录失败"))
    } catch (e: Exception) { Result.failure(Exception(mapConnectionError(e))) }

    private fun mapConnectionError(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            e is java.net.SocketTimeoutException -> "连接超时，请检查网络"
            e is java.net.ConnectException -> "无法连接到服务器，请确认地址和端口正确"
            msg.contains("Unable to resolve host", true) -> "无法解析服务器地址，请检查域名是否正确"
            msg.contains("Connection refused", true) -> "连接被拒绝，服务器可能未启动"
            msg.contains("timeout", true) -> "连接超时，请检查网络"
            msg.contains("SSL", true) -> "SSL连接错误"
            msg.contains("401") -> "用户名或密码错误"
            msg.contains("connect", true) -> "无法连接到服务器: $msg"
            else -> "连接失败: $msg"
        }
    }

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
