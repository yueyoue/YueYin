package com.yueyin.data.api

import com.yueyin.data.model.*
import okhttp3.ResponseBody
import retrofit2.http.*

interface TingReaderApi {
    @POST("/api/auth/login")
    suspend fun login(@Body request: TingLoginRequest): TingLoginResponse

    @GET("/api/books")
    suspend fun getBooks(@Header("Authorization") token: String): List<TingBook>

    @GET("/api/books/{id}")
    suspend fun getBook(@Header("Authorization") token: String, @Path("id") id: String): TingBook

    @GET("/api/books/{id}/chapters")
    suspend fun getChapters(@Header("Authorization") token: String, @Path("id") id: String): List<TingChapter>

    @GET("/api/stream/{chapterId}")
    suspend fun streamChapter(@Header("Authorization") token: String, @Path("chapterId") chapterId: String): ResponseBody

    @GET("/api/progress/recent")
    suspend fun getRecentProgress(@Header("Authorization") token: String): List<TingProgress>

    @GET("/api/progress/{bookId}")
    suspend fun getBookProgress(@Header("Authorization") token: String, @Path("bookId") bookId: String): TingProgress?

    @POST("/api/progress")
    suspend fun saveProgress(@Header("Authorization") token: String, @Body request: TingProgressRequest)

    @GET("/api/favorites")
    suspend fun getFavorites(@Header("Authorization") token: String): List<TingFavorite>

    @POST("/api/favorites/{bookId}")
    suspend fun addFavorite(@Header("Authorization") token: String, @Path("bookId") bookId: String)

    @DELETE("/api/favorites/{bookId}")
    suspend fun removeFavorite(@Header("Authorization") token: String, @Path("bookId") bookId: String)

    @GET("/api/search")
    suspend fun searchBooks(@Header("Authorization") token: String, @Query("q") query: String): List<TingBook>

    @GET("/api/stats")
    suspend fun getStats(@Header("Authorization") token: String): TingStats

    @GET("/api/proxy/cover")
    suspend fun proxyCover(@Header("Authorization") token: String, @Query("path") path: String): ResponseBody

    @GET("/api/libraries")
    suspend fun getLibraries(@Header("Authorization") token: String): List<TingLibrary>

    @GET("/api/books")
    suspend fun getBooksByLibrary(@Header("Authorization") token: String, @Query("library_id") libraryId: String): List<TingBook>
}
