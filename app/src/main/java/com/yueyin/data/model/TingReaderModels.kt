package com.yueyin.data.model

import com.google.gson.annotations.SerializedName

data class TingLoginRequest(val username: String, val password: String)
data class TingLoginResponse(val user: TingUserInfo? = null, val token: String = "")
data class TingUserInfo(val id: String = "", val username: String = "", val role: String = "")

data class TingBook(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val coverUrl: String? = null,
    val description: String? = null,
    val narrator: String? = null,
    val themeColor: String? = null,
    val tags: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val skipIntro: Int = 0,
    val skipOutro: Int = 0,
    val isFavorite: Boolean = false,
    val createdAt: String? = null,
    val libraryId: String? = null,
    val path: String? = null
)

data class TingChapter(
    val id: String = "",
    val bookId: String = "",
    val title: String = "",
    val duration: Double = 0.0,
    val chapterIndex: Int = 0,
    val isExtra: Int = 0,
    val progressPosition: Double? = null,
    val progressUpdatedAt: String? = null,
    val createdAt: String? = null
)

data class TingProgress(
    val id: String? = null,
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("book_id") val bookId: String = "",
    @SerializedName("chapter_id") val chapterId: String = "",
    val position: Double = 0.0,
    val duration: Double? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("book_title") val bookTitle: String? = null,
    @SerializedName("cover_url") val coverUrl: String? = null,
    @SerializedName("chapter_title") val chapterTitle: String? = null,
    @SerializedName("chapter_duration") val chapterDuration: Double? = null
)

data class TingProgressRequest(
    @SerializedName("book_id") val bookId: String,
    @SerializedName("chapter_id") val chapterId: String,
    val position: Double,
    val duration: Double
)

data class TingFavorite(
    @SerializedName("book_id") val bookId: String = "",
    @SerializedName("created_at") val createdAt: String? = null
)

data class TingStats(
    val totalBooks: Int = 0,
    val totalChapters: Int = 0,
    val totalDuration: Double = 0.0,
    val lastScanTime: String? = null
)

data class TingLibrary(
    val id: String = "",
    val name: String = "",
    val path: String? = null,
    val libraryType: String? = null,
    val bookCount: Int? = null,
    val createdAt: String? = null
)
