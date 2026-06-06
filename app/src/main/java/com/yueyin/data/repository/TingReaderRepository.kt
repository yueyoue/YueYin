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

    suspend fun ping(): Result<Unit> = try { api!!.getStats(""); Result.success(Unit) } catch (e: Exception) {
        Result.failure(Exception(when {
            e.message?.contains("timeout") == true -> "连接超时"
            e.message?.contains("Unable to resolve host") == true -> "无法解析服务器地址"
            e.message?.contains("Connection refused") == true -> "连接被拒绝"
            else -> e.message ?: "连接失败"
        }))
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
