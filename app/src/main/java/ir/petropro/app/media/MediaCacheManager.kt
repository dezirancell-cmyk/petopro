package ir.petropro.app.media

import android.content.Context
import ir.petropro.app.core.SessionStore
import ir.petropro.app.database.AppDatabase
import ir.petropro.app.database.MediaEntity
import ir.petropro.app.database.MediaState
import ir.petropro.app.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

enum class DownloadResult { DONE, NETWORK_ERROR, LOW_STORAGE }

/**
 * مدیریت فایل‌های رسانه (GIF/تصویر/ویدئو): دانلود واقعی روی حافظه‌ی داخلیِ خصوصی برنامه (filesDir)،
 * ادامه‌ی دانلود ناقص با Range، تأیید SHA-256، جلوگیری از دانلود تکراری، مدیریت فضا و پاک‌سازی.
 * فرمت اصلی فایل بدون تبدیل حفظ می‌شود.
 */
class MediaCacheManager(
    private val context: Context,
    private val db: AppDatabase,
    private val api: ApiService,
    private val store: SessionStore,
    private val client: OkHttpClient,
) {
    private val mutex = Mutex()
    private val dir: File get() = File(context.filesDir, "media").also { it.mkdirs() }

    fun observeDone() = db.mediaDao().observeDone()
    fun observeDoneCount() = db.mediaDao().observeDoneCount()
    fun observeTotalCount() = db.mediaDao().observeTotalCount()
    fun observeDoneBytes() = db.mediaDao().observeDoneBytes()

    fun freeBytes(): Long = context.filesDir.usableSpace

    /** آدرس کامل؛ مسیرهای /static/... از سرور خودمان، بقیه همان آدرس بیرونی */
    fun absoluteUrl(url: String): String {
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        val base = store.serverUrl?.trimEnd('/') ?: return url
        return base + (if (url.startsWith("/")) url else "/$url")
    }

    /**
     * Manifest رسانه‌ها را از سرور می‌گیرد و جدول محلی را هم‌تراز می‌کند:
     *  - فایل سالمِ دانلودشده (هم‌هش) دوباره دانلود نمی‌شود
     *  - فایلِ تغییرکرده در سرور (هش/اندازه‌ی متفاوت) برای دانلود مجدد علامت می‌خورد
     *  - فایل‌هایی که دیگر در برنامه‌ی فعال نیستند پاک می‌شوند (آزادسازی فضا)
     * خروجی false = سرور پاسخ موفق نداد (چیزی تغییر نکرد).
     */
    suspend fun refreshManifest(): Boolean = withContext(Dispatchers.IO) {
        val resp = api.mediaManifest()
        if (!resp.isSuccessful) return@withContext false
        val remote = resp.body()?.media.orEmpty().filter { it.exists }
        mutex.withLock {
            val remoteUrls = remote.map { it.url }.toSet()
            for (m in db.mediaDao().all()) {
                if (m.url !in remoteUrls) {
                    m.localPath?.let { File(it).delete() }
                    File(dir, fileNameFor(m.url) + ".part").delete()
                    db.mediaDao().delete(m.url)
                }
            }
            for (r in remote) {
                val existing = db.mediaDao().get(r.url)
                val fileOk = existing?.localPath?.let { File(it).isFile } == true
                val unchanged = existing != null && existing.state == MediaState.DONE && fileOk &&
                    (r.sha256 == null || r.sha256 == existing.expectedSha256)
                if (unchanged) continue
                existing?.localPath?.let { File(it).delete() }
                db.mediaDao().upsert(
                    MediaEntity(
                        url = r.url, kind = r.kind, isLocal = r.local, expectedSha256 = r.sha256, expectedSize = r.size,
                        state = MediaState.NEEDED, attempts = 0,
                    )
                )
            }
        }
        true
    }

    /** دانلودِ ترتیبی و قابل‌ازسرگیری. هر خطای شبکه دانلودها را متوقف می‌کند و فایل نیمه‌کاره (.part) را نگه می‌دارد. */
    suspend fun downloadPending(): DownloadResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            val pending = db.mediaDao().downloadable(MAX_ATTEMPTS)
            for (m in pending) {
                val needed = (m.expectedSize ?: ASSUMED_SIZE) + MIN_FREE_BYTES
                if (freeBytes() < needed) {
                    db.mediaDao().upsert(m.copy(state = MediaState.PAUSED_LOW_STORAGE, lastError = "فضای گوشی کم است"))
                    return@withLock DownloadResult.LOW_STORAGE
                }
                if (downloadOne(m) == DownloadResult.NETWORK_ERROR) return@withLock DownloadResult.NETWORK_ERROR
            }
            DownloadResult.DONE
        }
    }

    private suspend fun downloadOne(m: MediaEntity): DownloadResult {
        val target = File(dir, fileNameFor(m.url))
        val part = File(dir, target.name + ".part")
        val offset = if (part.isFile) part.length() else 0L
        val builder = Request.Builder().url(absoluteUrl(m.url))
        if (offset > 0) builder.header("Range", "bytes=$offset-")
        try {
            client.newCall(builder.build()).execute().use { resp ->
                if (resp.code == 416) { // بخش ذخیره‌شده با فایل سرور نمی‌خواند → از اول
                    part.delete()
                    return DownloadResult.NETWORK_ERROR
                }
                if (!resp.isSuccessful) {
                    db.mediaDao().upsert(m.copy(state = MediaState.FAILED, attempts = m.attempts + 1, lastError = "HTTP ${resp.code}"))
                    return DownloadResult.DONE // این فایل شکست خورد، سراغ بعدی
                }
                val append = resp.code == 206 && offset > 0
                val body = resp.body ?: throw IOException("پاسخ خالی")
                FileOutputStream(part, append).use { out -> body.byteStream().copyTo(out) }
            }
        } catch (e: IOException) {
            // قطع اینترنت وسط دانلود: .part حفظ می‌شود و بعداً ادامه می‌یابد؛ شمارنده‌ی خطا زیاد نمی‌شود
            return DownloadResult.NETWORK_ERROR
        }
        val expectedSize = m.expectedSize
        if (expectedSize != null && part.length() != expectedSize) {
            part.delete()
            db.mediaDao().upsert(m.copy(state = MediaState.FAILED, attempts = m.attempts + 1, lastError = "حجم فایل با سرور نمی‌خواند"))
            return DownloadResult.DONE
        }
        val expectedSha = m.expectedSha256
        if (expectedSha != null && !sha256(part).equals(expectedSha, ignoreCase = true)) {
            part.delete()
            db.mediaDao().upsert(m.copy(state = MediaState.FAILED, attempts = m.attempts + 1, lastError = "هش فایل نامعتبر است"))
            return DownloadResult.DONE
        }
        if (part.length() == 0L) {
            part.delete()
            db.mediaDao().upsert(m.copy(state = MediaState.FAILED, attempts = m.attempts + 1, lastError = "فایل خالی"))
            return DownloadResult.DONE
        }
        target.delete()
        if (!part.renameTo(target)) {
            db.mediaDao().upsert(m.copy(state = MediaState.FAILED, attempts = m.attempts + 1, lastError = "ذخیره‌ی فایل ناموفق"))
            return DownloadResult.DONE
        }
        db.mediaDao().upsert(
            m.copy(state = MediaState.DONE, localPath = target.absolutePath, sizeBytes = target.length(), attempts = 0, lastError = null)
        )
        return DownloadResult.DONE
    }

    /** پاک‌سازی دستیِ کش توسط کاربر: فایل‌ها و ردیف‌ها حذف می‌شوند و در همگام‌سازی بعدی دوباره ساخته می‌شوند */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        mutex.withLock {
            deleteFilesInternal()
            db.mediaDao().deleteAll()
        }
    }

    /** فقط فایل‌ها (برای خروج از حساب؛ دیتابیس جداگانه پاک می‌شود) */
    suspend fun deleteAllFiles() = withContext(Dispatchers.IO) { mutex.withLock { deleteFilesInternal() } }

    private fun deleteFilesInternal() {
        dir.listFiles()?.forEach { it.delete() }
    }

    private fun fileNameFor(url: String): String {
        val ext = url.substringBefore('?').substringAfterLast('.', "bin").lowercase()
            .takeIf { it.length in 2..5 && it.all { c -> c.isLetterOrDigit() } } ?: "bin"
        return sha256Text(url).take(32) + "." + ext
    }

    private fun sha256Text(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val MAX_ATTEMPTS = 6
        const val MIN_FREE_BYTES = 100L * 1024 * 1024
        const val ASSUMED_SIZE = 5L * 1024 * 1024
    }
}
