package com.lechenmusic.data.model

import com.google.gson.annotations.SerializedName

// --- Auth ---
data class TingLoginRequest(
    val username: String,
    val password: String
)

data class TingLoginResponse(
    val user: TingUserInfo? = null,
    val token: String = ""
)

data class TingUserInfo(
    val id: String = "",
    val username: String = "",
    val role: String = ""
)

// --- Books ---
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

// --- Chapters ---
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

// --- Progress ---
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

// --- Favorites ---
data class TingFavorite(
    @SerializedName("book_id") val bookId: String = "",
    @SerializedName("created_at") val createdAt: String? = null
)

// --- Stats ---
data class TingStats(
    @SerializedName("total_books") val totalBooks: Int = 0,
    @SerializedName("total_chapters") val totalChapters: Int = 0,
    @SerializedName("total_users") val totalUsers: Int = 0
)
