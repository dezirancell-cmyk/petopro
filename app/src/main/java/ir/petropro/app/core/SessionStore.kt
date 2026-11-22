package ir.petropro.app.core

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * نگهداری نشست. توکن فقط در EncryptedSharedPreferences (کلید داخل Android Keystore) ذخیره می‌شود؛
 * هیچ چیزی در حافظه‌ی عمومی گوشی نوشته نمی‌شود.
 */
class SessionStore(private val context: Context) {

    private val plain: SharedPreferences =
        context.getSharedPreferences("petropro_plain", Context.MODE_PRIVATE)
    private val secure: SharedPreferences = createSecure()

    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    private fun createSecure(): SharedPreferences {
        fun build(): SharedPreferences {
            val key = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            return EncryptedSharedPreferences.create(
                context, "petropro_secure", key,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
        return try {
            build()
        } catch (e: Exception) {
            // کلید Keystore خراب/پاک شده (مثلاً بعد از ریست قفل گوشی): فایل را پاک و دوباره می‌سازیم.
            // فقط توکن از دست می‌رود؛ دیتابیس و Outbox دست‌نخورده می‌مانند و با ورود مجدد ادامه می‌یابند.
            context.deleteSharedPreferences("petropro_secure")
            build()
        }
    }

    var serverUrl: String?
        get() = plain.getString(KEY_SERVER, null)
        set(value) = plain.edit().putString(KEY_SERVER, value).apply()

    /** شناسه‌ی نصب؛ یک‌بار ساخته می‌شود و هم در ورود و هم در ثبت دستگاه استفاده می‌شود. */
    val deviceId: String
        get() = plain.getString(KEY_DEVICE, null) ?: UUID.randomUUID().toString().also {
            plain.edit().putString(KEY_DEVICE, it).apply()
        }

    var token: String?
        get() = secure.getString(KEY_TOKEN, null)
        private set(value) = secure.edit().putString(KEY_TOKEN, value).apply()

    /** کاربری که داده‌های فعلیِ دیتابیس متعلق به اوست (حتی بعد از انقضای توکن حفظ می‌شود) */
    var ownerUserId: Int
        get() = plain.getInt(KEY_OWNER, -1)
        private set(value) = plain.edit().putInt(KEY_OWNER, value).apply()

    var username: String?
        get() = plain.getString(KEY_USERNAME, null)
        private set(value) = plain.edit().putString(KEY_USERNAME, value).apply()

    var fullName: String?
        get() = plain.getString(KEY_FULLNAME, null)
        private set(value) = plain.edit().putString(KEY_FULLNAME, value).apply()

    var role: String?
        get() = plain.getString(KEY_ROLE, null)
        private set(value) = plain.edit().putString(KEY_ROLE, value).apply()

    var lastSyncMillis: Long
        get() = plain.getLong(KEY_LAST_SYNC, 0L)
        set(value) = plain.edit().putLong(KEY_LAST_SYNC, value).apply()

    /** کاربر می‌تواند Push را خاموش کند (به سرور هم اعلام می‌شود تا برای این دستگاه نفرستد) */
    var pushEnabled: Boolean
        get() = plain.getBoolean(KEY_PUSH, true)
        set(value) = plain.edit().putBoolean(KEY_PUSH, value).apply()

    var askedNotificationPermission: Boolean
        get() = plain.getBoolean(KEY_ASKED_PERM, false)
        set(value) = plain.edit().putBoolean(KEY_ASKED_PERM, value).apply()

    fun reminderEnabled(kind: String): Boolean = plain.getBoolean("reminder_${kind}_on", false)
    fun reminderHour(kind: String): Int = plain.getInt("reminder_${kind}_h", if (kind == KIND_CHECKIN) 9 else 17)
    fun reminderMinute(kind: String): Int = plain.getInt("reminder_${kind}_m", 0)
    fun setReminder(kind: String, enabled: Boolean, hour: Int, minute: Int) {
        plain.edit().putBoolean("reminder_${kind}_on", enabled).putInt("reminder_${kind}_h", hour)
            .putInt("reminder_${kind}_m", minute).apply()
    }

    val isLoggedIn: Boolean get() = !token.isNullOrEmpty()

    fun saveSession(token: String, userId: Int, username: String, fullName: String?, role: String) {
        this.token = token
        this.ownerUserId = userId
        this.username = username
        this.fullName = fullName
        this.role = role
        _sessionExpired.value = false
    }

    fun markExpired() {
        if (isLoggedIn) _sessionExpired.value = true
    }

    /** فقط توکن پاک می‌شود (برای ورود مجدد با حفظ داده‌ها). */
    fun clearToken() {
        token = null
        _sessionExpired.value = false
    }

    /** پاک‌سازی کامل کاربر (آدرس سرور و شناسه‌ی نصب حفظ می‌شود). */
    fun clearUser() {
        secure.edit().clear().apply()
        plain.edit()
            .remove(KEY_OWNER).remove(KEY_USERNAME).remove(KEY_FULLNAME).remove(KEY_ROLE).remove(KEY_LAST_SYNC)
            .apply()
        _sessionExpired.value = false
    }

    companion object {
        const val KIND_WORKOUT = "workout"
        const val KIND_CHECKIN = "checkin"
        const val KEY_PUSH = "push_enabled"
        const val KEY_ASKED_PERM = "asked_notif_permission"
        const val KEY_SERVER = "server_url"
        const val KEY_DEVICE = "device_id"
        const val KEY_TOKEN = "token"
        const val KEY_OWNER = "owner_user_id"
        const val KEY_USERNAME = "username"
        const val KEY_FULLNAME = "full_name"
        const val KEY_ROLE = "role"
        const val KEY_LAST_SYNC = "last_sync"
    }
}
