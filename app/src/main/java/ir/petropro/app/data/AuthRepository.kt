package ir.petropro.app.data

import android.os.Build
import androidx.room.withTransaction
import ir.petropro.app.core.SessionStore
import ir.petropro.app.database.AppDatabase
import ir.petropro.app.media.MediaCacheManager
import ir.petropro.app.network.ApiErrors
import ir.petropro.app.network.ApiService
import ir.petropro.app.network.LoginRequest
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException

sealed class LoginResult {
    data object Success : LoginResult()
    data class Failure(val message: String) : LoginResult()
    /** روی گوشی عملیات ارسال‌نشده‌ی «کاربر دیگری» هست؛ بدون تأیید صریح حذف نمی‌شود */
    data class PendingOtherUser(val count: Int) : LoginResult()
}

class AuthRepository(
    private val api: ApiService,
    private val store: SessionStore,
    private val db: AppDatabase,
    private val media: MediaCacheManager,
) {
    /** آدرس را نرمال می‌کند: فقط http/https، بدون / انتهایی. نامعتبر → null */
    fun normalizeServerUrl(raw: String): String? {
        val trimmed = raw.trim().trimEnd('/')
        if (!(trimmed.startsWith("http://") || trimmed.startsWith("https://"))) return null
        val url = trimmed.toHttpUrlOrNull() ?: return null
        return url.toString().trimEnd('/')
    }

    suspend fun login(serverUrl: String, username: String, password: String): LoginResult {
        val url = normalizeServerUrl(serverUrl) ?: return LoginResult.Failure("آدرس سرور نامعتبر است")
        store.serverUrl = url
        val resp = try {
            api.login(LoginRequest(username.trim(), password, store.deviceId, Build.MODEL))
        } catch (e: IOException) {
            return LoginResult.Failure("اتصال به سرور برقرار نشد. اینترنت و آدرس سرور را بررسی کنید.")
        }
        if (!resp.isSuccessful) {
            return LoginResult.Failure(ApiErrors.message(resp) ?: "ورود ناموفق بود (${resp.code()})")
        }
        val body = resp.body()
        val token = body?.token
        val user = body?.user
        if (body == null || !body.ok || token == null || user == null) {
            return LoginResult.Failure(body?.error ?: "پاسخ سرور نامعتبر است")
        }
        val owner = store.ownerUserId
        if (owner != -1 && owner != user.id) {
            val pending = db.outboxDao().unsentCount()
            if (pending > 0) {
                // توکنِ تازه‌صادرشده را فوراً باطل می‌کنیم؛ داده‌ی کاربر قبلی دست‌نخورده می‌ماند.
                try { api.logoutWith("Bearer $token") } catch (e: IOException) { /* منقضی می‌شود */ }
                return LoginResult.PendingOtherUser(pending)
            }
            wipeLocalData()
        }
        store.saveSession(token, user.id, user.username.ifEmpty { username.trim() }, user.fullName, user.role)
        return LoginResult.Success
    }

    suspend fun unsentCount(): Int = db.outboxDao().unsentCount()

    /** حذف همه‌ی داده‌های محلی (دیتابیس + فایل‌های رسانه) — فقط با تأیید کاربر صدا زده شود */
    suspend fun wipeLocalData() {
        db.withTransaction { db.clearAllTables() }
        media.deleteAllFiles()
        store.clearUser()
    }

    /** خروج: لغوِ توکن روی سرور (اگر اینترنت باشد) و پاک‌سازی کامل داده‌های محلی */
    suspend fun logoutAndWipe() {
        try { api.logout() } catch (e: IOException) { /* آفلاین: توکن روی سرور منقضی می‌شود */ }
        wipeLocalData()
    }
}
