package com.yueyin.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class UpdateInfo(
    val versionCode: Int = 0,
    val versionName: String = "",
    val apkUrl: String = "",
    val updateLog: String = ""
)

data class VersionFile(
    @SerializedName("versionCode") val versionCode: Int = 0,
    @SerializedName("versionName") val versionName: String = "",
    @SerializedName("apkUrl") val apkUrl: String = "",
    @SerializedName("updateLog") val updateLog: String = ""
)

object UpdateChecker {
    private const val VERSION_URL = "https://ct.tthsdd.top/update/version.json"
    private const val GITHUB_VERSION_URL = "https://raw.githubusercontent.com/yueyoue/YueYin/main/update/version.json"

    suspend fun check(currentVersionCode: Int): UpdateInfo? = withContext(Dispatchers.IO) {
        // Try custom server first, then GitHub; pick the newest
        val server = tryServer(VERSION_URL, currentVersionCode)
        if (server != null) return@withContext server
        tryServer(GITHUB_VERSION_URL, currentVersionCode)
    }

    private suspend fun tryServer(url: String, currentVersionCode: Int): UpdateInfo? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.setRequestProperty("User-Agent", "YueYin-Android/${currentVersionCode}")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
            conn.setRequestProperty("Pragma", "no-cache")
            conn.instanceFollowRedirects = true
            conn.useCaches = false
            if (conn.responseCode != 200) { conn.disconnect(); return null }
            val json = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            if (json.isBlank()) return null
            val v = Gson().fromJson(json, VersionFile::class.java) ?: return null
            if (v.versionCode > currentVersionCode) {
                // Ensure apkUrl is absolute
                val apkUrl = when {
                    v.apkUrl.startsWith("http://") || v.apkUrl.startsWith("https://") -> v.apkUrl
                    v.apkUrl.isNotBlank() -> "https://github.com/yueyoue/YueYin/releases/download/${v.versionName}/app-release.apk"
                    else -> "https://github.com/yueyoue/YueYin/releases/download/${v.versionName}/app-release.apk"
                }
                UpdateInfo(v.versionCode, v.versionName, apkUrl, v.updateLog)
            } else null
        } catch (e: Exception) { null }
    }

    suspend fun downloadApk(context: Context, apkUrl: String, onProgress: (String) -> Unit): File? = withContext(Dispatchers.IO) {
        try {
            onProgress("正在下载...")
            // Follow redirects for GitHub releases
            var conn = URL(apkUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 30000
            conn.readTimeout = 60000
            conn.instanceFollowRedirects = true
            // Handle redirect chains (GitHub uses 302 redirects)
            var redirects = 0
            while (conn.responseCode in 301..308 && redirects < 10) {
                val location = conn.getHeaderField("Location") ?: break
                conn.disconnect()
                conn = URL(location).openConnection() as HttpURLConnection
                conn.connectTimeout = 30000
                conn.readTimeout = 60000
                conn.instanceFollowRedirects = true
                redirects++
            }
            if (conn.responseCode != 200) {
                onProgress("下载失败: HTTP ${conn.responseCode}")
                conn.disconnect()
                return@withContext null
            }
            val totalSize = conn.contentLength
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "yueyin_update.apk")
            conn.inputStream.use { input ->
                file.outputStream().use { output ->
                    val buf = ByteArray(8192); var read: Int; var total = 0L
                    while (input.read(buf).also { read = it } != -1) {
                        output.write(buf, 0, read); total += read
                        if (totalSize > 0) onProgress("下载中 ${(total * 100 / totalSize).toInt()}%")
                    }
                }
            }; conn.disconnect(); file
        } catch (e: Exception) { onProgress("下载失败: ${e.message}"); null }
    }

    fun installApk(context: Context, file: File): Boolean = try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }); true
    } catch (_: Exception) { false }
}
