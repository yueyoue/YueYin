package com.lechenmusic.data.repository

import com.lechenmusic.data.api.TingReaderApiClient
import com.lechenmusic.data.api.TingReaderApi
import com.lechenmusic.data.model.*

class TingReaderRepository {
    private var api: TingReaderApi? = null
    private var serverUrl: String = ""
    private var username: String = ""
    private var password: String = ""
    private var token: String = ""

    fun configure(baseUrl: String, user: String, pass: String) {
        serverUrl = baseUrl
        username = user
        password = pass
        api = TingReaderApiClient.getApi(baseUrl)
    }

    fun getStreamUrl(chapterId: String): String {
        return TingReaderApiClient.getStreamUrl(serverUrl, chapterId, token)
    }

    fun getCoverUrl(coverUrl: String?): String? {
        if (coverUrl.isNullOrBlank()) return null
        // If it's already a full URL, proxy it
        return TingReaderApiClient.getCoverProxyUrl(serverUrl, coverUrl, token)
    }

    fun getAuthToken(): String = "Bearer $token"

    suspend fun ping(): Result<Unit> {
        return try {
            // Try stats endpoint (public, no auth needed)
            api!!.getStats()
            Result.success(Unit)
        } catch (e: Exception) {
            val msg = when {
                e.message?.contains("timeout") == true -> "连接超时，请检查服务器地址"
                e.message?.contains("Unable to resolve host") == true -> "无法解析服务器地址"
                e.message?.contains("Connection refused") == true -> "连接被拒绝，请检查端口"
                else -> e.message ?: "连接失败"
            }
            Result.failure(Exception(msg))
        }
    }

    suspend fun login(): Result<Unit> {
        return try {
            val response = api!!.login(TingLoginRequest(username, password))
            if (response.token.isNotBlank()) {
                token = response.token
                Result.success(Unit)
            } else {
                Result.failure(Exception("登录失败：未获取到token"))
            }
        } catch (e: Exception) {
            val msg = when {
                e.message?.contains("401") == true -> "用户名或密码错误"
                e.message?.contains("timeout") == true -> "连接超时"
                else -> e.message ?: "登录失败"
            }
            Result.failure(Exception(msg))
        }
    }

    suspend fun getBooks(): Result<List<TingBook>> {
        return try {
            val books = api!!.getBooks("Bearer $token")
            Result.success(books)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBook(id: String): Result<TingBook> {
        return try {
            val book = api!!.getBook("Bearer $token", id)
            Result.success(book)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChapters(bookId: String): Result<List<TingChapter>> {
        return try {
            val chapters = api!!.getChapters("Bearer $token", bookId)
            Result.success(chapters)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecentProgress(): Result<List<TingProgress>> {
        return try {
            val progress = api!!.getRecentProgress("Bearer $token")
            Result.success(progress)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBookProgress(bookId: String): Result<TingProgress?> {
        return try {
            val progress = api!!.getBookProgress("Bearer $token", bookId)
            Result.success(progress)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveProgress(bookId: String, chapterId: String, position: Double, duration: Double): Result<Unit> {
        return try {
            api!!.saveProgress("Bearer $token", TingProgressRequest(bookId, chapterId, position, duration))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFavorites(): Result<List<TingFavorite>> {
        return try {
            val favorites = api!!.getFavorites("Bearer $token")
            Result.success(favorites)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addFavorite(bookId: String): Result<Unit> {
        return try {
            api!!.addFavorite("Bearer $token", bookId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFavorite(bookId: String): Result<Unit> {
        return try {
            api!!.removeFavorite("Bearer $token", bookId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchBooks(query: String): Result<List<TingBook>> {
        return try {
            val books = api!!.searchBooks("Bearer $token", query)
            Result.success(books)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
