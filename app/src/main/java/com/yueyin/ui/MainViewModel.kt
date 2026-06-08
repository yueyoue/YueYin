package com.yueyin.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yueyin.YueYinApp
import com.yueyin.data.model.*
import com.yueyin.data.repository.SettingsRepository
import com.yueyin.data.repository.TingReaderRepository
import com.yueyin.player.AudiobookPlayerManager
import com.yueyin.update.UpdateChecker
import com.yueyin.update.UpdateInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as YueYinApp
    val tingRepository: TingReaderRepository = app.tingRepository
    val audiobookPlayerManager: AudiobookPlayerManager = app.audiobookPlayerManager
    private val settings: SettingsRepository = app.settingsRepository

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()
    val tingServerUrl: StateFlow<String> = settings.tingServerUrl.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val tingToken: StateFlow<String> = settings.tingToken.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()
    fun clearToast() { _toastMessage.value = null }

    private val _allBooks = MutableStateFlow<List<TingBook>>(emptyList())
    val allBooks: StateFlow<List<TingBook>> = _allBooks.asStateFlow()
    private val _booksLoading = MutableStateFlow(false)
    val booksLoading: StateFlow<Boolean> = _booksLoading.asStateFlow()
    private val _bookProgress = MutableStateFlow<Map<String, TingProgress>>(emptyMap())
    val bookProgress: StateFlow<Map<String, TingProgress>> = _bookProgress.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _searchResults = MutableStateFlow<List<TingBook>>(emptyList())
    val searchResults: StateFlow<List<TingBook>> = _searchResults.asStateFlow()

    private val _genres = MutableStateFlow<List<String>>(emptyList())
    val genres: StateFlow<List<String>> = _genres.asStateFlow()
    private val _selectedGenre = MutableStateFlow("全部")
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    private val _libraries = MutableStateFlow<List<TingLibrary>>(emptyList())
    val libraries: StateFlow<List<TingLibrary>> = _libraries.asStateFlow()
    private val _libraryBooks = MutableStateFlow<Map<String, List<TingBook>>>(emptyMap())
    val libraryBooks: StateFlow<Map<String, List<TingBook>>> = _libraryBooks.asStateFlow()
    private val _libraryBooksLoading = MutableStateFlow(false)
    val libraryBooksLoading: StateFlow<Boolean> = _libraryBooksLoading.asStateFlow()

    private val _stats = MutableStateFlow(TingStats())
    val stats: StateFlow<TingStats> = _stats.asStateFlow()
    private val _favoriteBookIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteBookIds: StateFlow<Set<String>> = _favoriteBookIds.asStateFlow()

    private val _timerRemainingSeconds = MutableStateFlow(0L)
    val timerRemainingSeconds: StateFlow<Long> = _timerRemainingSeconds.asStateFlow()
    private var countdownJob: kotlinx.coroutines.Job? = null

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()
    private val _updateStatus = MutableStateFlow<String?>(null)
    val updateStatus: StateFlow<String?> = _updateStatus.asStateFlow()

    private var lastProgressSaveTime = 0L

    // Dark mode
    val darkMode: StateFlow<Boolean> = settings.darkMode.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Total listening hours
    val totalListeningHours: StateFlow<Double> = _bookProgress.map { progressMap ->
        progressMap.values.sumOf { it.duration ?: 0.0 } / 3600.0
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    init {
        viewModelScope.launch {
            combine(settings.tingServerUrl, settings.tingUsername, settings.tingPassword) { url, user, pass -> Triple(url, user, pass) }
                .collect { (url, user, pass) ->
                    if (url.isNotBlank() && user.isNotBlank() && pass.isNotBlank()) {
                        tingRepository.configure(url, user, pass)
                        if (tingRepository.login().isSuccess) {
                            settings.saveTingToken(tingRepository.getAuthToken().removePrefix("Bearer "))
                            _isLoggedIn.value = true; loadBooks()
                        }
                    }
                }
        }
        audiobookPlayerManager.onProgressUpdate = { bookId, chapterId, position, duration ->
            val now = System.currentTimeMillis()
            if (now - lastProgressSaveTime > 5000) {
                lastProgressSaveTime = now
                viewModelScope.launch { try { tingRepository.saveProgress(bookId, chapterId, position.toDouble() / 1000.0, duration.toDouble() / 1000.0) } catch (_: Exception) {} }
            }
        }
        audiobookPlayerManager.onChapterAutoAdvanced = { chapter ->
            viewModelScope.launch { try { tingRepository.saveProgress(audiobookPlayerManager.currentBookId.value, chapter.id, 0.0, chapter.duration) } catch (_: Exception) {} }
        }
        audiobookPlayerManager.onError = { msg ->
            _toastMessage.value = msg
        }
    }

    fun login(serverUrl: String, username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true; _loginError.value = null
            try {
                tingRepository.configure(serverUrl, username, password)
                val result = tingRepository.login()
                if (result.isSuccess) {
                    settings.saveTingLogin(serverUrl, username, password)
                    settings.saveTingToken(tingRepository.getAuthToken().removePrefix("Bearer "))
                    audiobookPlayerManager.updateStreamAuth(serverUrl, tingRepository.getAuthToken().removePrefix("Bearer "))
                    _isLoggedIn.value = true; loadBooks()
                } else { _loginError.value = result.exceptionOrNull()?.message ?: "登录失败" }
            } catch (e: Exception) { _loginError.value = e.message ?: "连接失败" }
            finally { _isLoading.value = false }
        }
    }

    fun logout() {
        viewModelScope.launch {
            settings.clearTingLogin(); _isLoggedIn.value = false
            _allBooks.value = emptyList(); _bookProgress.value = emptyMap()
            audiobookPlayerManager.release()
        }
    }

    fun updateServerInfo(serverUrl: String, username: String, password: String) {
        viewModelScope.launch {
            try {
                settings.saveTingLogin(serverUrl, username, password)
                tingRepository.configure(serverUrl, username, password)
                val result = tingRepository.login()
                if (result.isSuccess) {
                    settings.saveTingToken(tingRepository.getAuthToken().removePrefix("Bearer "))
                    audiobookPlayerManager.updateStreamAuth(serverUrl, tingRepository.getAuthToken().removePrefix("Bearer "))
                    _toastMessage.value = "服务器信息已更新"
                    loadBooks()
                } else { _toastMessage.value = "登录失败: ${result.exceptionOrNull()?.message}" }
            } catch (e: Exception) { _toastMessage.value = "更新失败: ${e.message}" }
        }
    }

    fun loadBooks() {
        viewModelScope.launch {
            val url = settings.tingServerUrl.first(); val user = settings.tingUsername.first(); val pass = settings.tingPassword.first()
            if (url.isBlank() || user.isBlank()) return@launch
            _booksLoading.value = true
            try {
                tingRepository.configure(url, user, pass); tingRepository.login()
                audiobookPlayerManager.updateStreamAuth(url, tingRepository.getAuthToken().removePrefix("Bearer "))
                // Fetch favorites FIRST to ensure sync
                val favIds = mutableSetOf<String>()
                val favResult = tingRepository.getFavorites()
                favResult.onSuccess { favs -> favs.forEach { favIds.add(it.bookId) } }
                _favoriteBookIds.value = favIds
                tingRepository.getBooks().onSuccess { books ->
                    val booksWithFav = books.map {
                        val resolvedCover = tingRepository.getCoverUrl(it.coverUrl, it.id)
                        it.copy(isFavorite = favIds.contains(it.id), coverUrl = resolvedCover)
                    }
                    _allBooks.value = booksWithFav
                    val gs = mutableSetOf<String>()
                    booksWithFav.forEach { b ->
                        b.genre?.split(",")?.forEach { if (it.isNotBlank()) gs.add(it.trim()) }
                    }
                    _genres.value = listOf("全部") + gs.sorted()
                }
                tingRepository.getRecentProgress().onSuccess { _bookProgress.value = it.associateBy { p -> p.bookId } }
                try { tingRepository.getStats().getOrNull()?.let { _stats.value = it } } catch (_: Exception) {}
                // Load libraries and per-library books
                loadLibraryBooks()
            } catch (e: Exception) { _toastMessage.value = "加载失败: ${e.message}" }
            finally { _booksLoading.value = false }
        }
    }

    private fun loadLibraryBooks() {
        viewModelScope.launch {
            _libraryBooksLoading.value = true
            try {
                tingRepository.getLibraries().onSuccess { libs ->
                    _libraries.value = libs
                    val map = mutableMapOf<String, List<TingBook>>()
                    libs.forEach { lib ->
                        tingRepository.getBooksByLibrary(lib.id).onSuccess { books ->
                            val resolved = books.map { b ->
                                val resolvedCover = tingRepository.getCoverUrl(b.coverUrl, b.id)
                                b.copy(
                                    coverUrl = resolvedCover,
                                    isFavorite = _favoriteBookIds.value.contains(b.id)
                                )
                            }.sortedByDescending { it.createdAt ?: "" }.take(9)
                            map[lib.id] = resolved
                        }
                    }
                    _libraryBooks.value = map
                }
            } catch (_: Exception) {}
            finally { _libraryBooksLoading.value = false }
        }
    }

    fun setSelectedGenre(genre: String) { _selectedGenre.value = genre }

    fun getFilteredBooks(): List<TingBook> {
        val g = _selectedGenre.value; val books = _allBooks.value
        if (g == "全部") return books
        return books.filter { it.genre?.contains(g, true) == true }
    }

    fun getRecentBooks(): List<TingBook> {
        val pm = _bookProgress.value
        return _allBooks.value.filter { pm.containsKey(it.id) }.sortedByDescending { pm[it.id]?.updatedAt ?: "" }.take(5)
    }

    fun searchBooks(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        viewModelScope.launch {
            tingRepository.searchBooks(query).onSuccess { books ->
                _searchResults.value = books.map { it.copy(coverUrl = tingRepository.getCoverUrl(it.coverUrl, it.id)) }
            }
        }
    }

    fun loadAndPlayAudiobook(bookId: String) {
        viewModelScope.launch {
            try {
                // If same book is already loaded, don't reload
                if (audiobookPlayerManager.currentBookId.value == bookId && audiobookPlayerManager.isBookLoaded()) {
                    // But still sync favorite status from server
                    tingRepository.getFavorites().onSuccess { favs ->
                        _favoriteBookIds.value = favs.map { it.bookId }.toSet()
                        _allBooks.value = _allBooks.value.map { it.copy(isFavorite = _favoriteBookIds.value.contains(it.id)) }
                    }
                    return@launch
                }
                val url = settings.tingServerUrl.first(); val user = settings.tingUsername.first(); val pass = settings.tingPassword.first()
                if (url.isNotBlank() && user.isNotBlank()) {
                    tingRepository.configure(url, user, pass); tingRepository.login()
                    audiobookPlayerManager.updateStreamAuth(url, tingRepository.getAuthToken().removePrefix("Bearer "))
                }
                // Sync favorites from server
                tingRepository.getFavorites().onSuccess { favs ->
                    _favoriteBookIds.value = favs.map { it.bookId }.toSet()
                    _allBooks.value = _allBooks.value.map { it.copy(isFavorite = _favoriteBookIds.value.contains(it.id)) }
                }
                tingRepository.getChapters(bookId).onSuccess { chapters ->
                    if (chapters.isEmpty()) { _toastMessage.value = "该书暂无章节"; return@launch }
                    var startIdx = 0; var startMs = 0L
                    tingRepository.getBookProgress(bookId).onSuccess { p ->
                        if (p != null) { val i = chapters.indexOfFirst { it.id == p.chapterId }; if (i >= 0) { startIdx = i; startMs = (p.position * 1000).toLong() } }
                    }
                    tingRepository.getBook(bookId).onSuccess { book ->
                        val resolvedCoverUrl = tingRepository.getCoverUrl(book.coverUrl, bookId)
                        audiobookPlayerManager.loadBook(bookId, book.title, book.author, book.narrator ?: "", resolvedCoverUrl, book.description ?: "", chapters, startIdx, startMs)
                    }
                }
            } catch (e: Exception) { _toastMessage.value = "播放失败: ${e.message}" }
        }
    }

    fun toggleFavorite(bookId: String) {
        viewModelScope.launch {
            val book = _allBooks.value.find { it.id == bookId }
            val currentlyFav = book?.isFavorite ?: _favoriteBookIds.value.contains(bookId)
            val newFav = !currentlyFav
            // Optimistic update - immediately update local state
            _allBooks.value = _allBooks.value.map { if (it.id == bookId) it.copy(isFavorite = newFav) else it }
            _favoriteBookIds.value = if (newFav) _favoriteBookIds.value + bookId else _favoriteBookIds.value - bookId
            // Then sync with server
            val result = if (currentlyFav) tingRepository.removeFavorite(bookId) else tingRepository.addFavorite(bookId)
            if (result.isFailure) {
                // Revert on failure
                _allBooks.value = _allBooks.value.map { if (it.id == bookId) it.copy(isFavorite = !newFav) else it }
                _favoriteBookIds.value = if (!newFav) _favoriteBookIds.value + bookId else _favoriteBookIds.value - bookId
                _toastMessage.value = "收藏操作失败: ${result.exceptionOrNull()?.message}"
            } else {
                // Re-sync favorites from server to ensure consistency
                tingRepository.getFavorites().onSuccess { favs ->
                    val serverFavIds = favs.map { it.bookId }.toSet()
                    _favoriteBookIds.value = serverFavIds
                    _allBooks.value = _allBooks.value.map { it.copy(isFavorite = serverFavIds.contains(it.id)) }
                }
            }
        }
    }

    // Single source of truth for favorite status - derived from _allBooks
    val favoriteStatusMap: StateFlow<Map<String, Boolean>> = _allBooks.map { books ->
        books.associate { it.id to it.isFavorite }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun isBookFavorite(bookId: String): StateFlow<Boolean> {
        return favoriteStatusMap.map { it[bookId] ?: false }
            .stateIn(viewModelScope, SharingStarted.Eagerly, _favoriteBookIds.value.contains(bookId))
    }

    fun syncFavoriteFromServer(bookId: String) {
        viewModelScope.launch {
            tingRepository.getFavorites().onSuccess { favs ->
                val serverFavIds = favs.map { it.bookId }.toSet()
                _favoriteBookIds.value = serverFavIds
                _allBooks.value = _allBooks.value.map { it.copy(isFavorite = serverFavIds.contains(it.id)) }
            }
        }
    }

    fun setAudiobookTimer(minutes: Int) {
        cancelTimer(); _timerRemainingSeconds.value = minutes * 60L
        countdownJob = viewModelScope.launch {
            while (_timerRemainingSeconds.value > 0) { kotlinx.coroutines.delay(1000); _timerRemainingSeconds.value = (_timerRemainingSeconds.value - 1).coerceAtLeast(0) }
            audiobookPlayerManager.forcePause()
        }
    }

    fun cancelTimer() { countdownJob?.cancel(); countdownJob = null; _timerRemainingSeconds.value = 0 }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { settings.saveDarkMode(enabled) }
    }

    fun checkForUpdate(silent: Boolean = true) {
        viewModelScope.launch {
            try {
                val ctx = getApplication<android.app.Application>()
                val code = if (android.os.Build.VERSION.SDK_INT >= 28) ctx.packageManager.getPackageInfo(ctx.packageName, 0).longVersionCode.toInt()
                else @Suppress("DEPRECATION") ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionCode
                val info = UpdateChecker.check(code)
                if (info != null) _updateInfo.value = info
                else if (!silent) _toastMessage.value = "当前已是最新版本 ✓"
            } catch (e: Exception) { if (!silent) _toastMessage.value = "检查更新失败: ${e.message}" }
        }
    }

    fun downloadUpdate() {
        val info = _updateInfo.value ?: return
        viewModelScope.launch {
            _updateStatus.value = "正在下载..."
            val ctx = getApplication<android.app.Application>()
            val file = UpdateChecker.downloadApk(ctx, info.apkUrl) { _updateStatus.value = it }
            if (file != null) { _updateStatus.value = "安装中..."; UpdateChecker.installApk(ctx, file); kotlinx.coroutines.delay(8000) }
            _updateStatus.value = null; _updateInfo.value = null
        }
    }

    fun dismissUpdate() { _updateInfo.value = null; _updateStatus.value = null }
}
