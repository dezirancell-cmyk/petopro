package ir.petropro.app.notifications

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import ir.petropro.app.BuildConfig
import ir.petropro.app.core.SessionStore
import ir.petropro.app.network.ApiService
import ir.petropro.app.network.DeviceRegisterRequest
import kotlinx.coroutines.tasks.await
import java.io.IOException

/**
 * ثبت دستگاه و توکن FCM روی سرور. اگر فایل google-services.json نباشد، Firebase مقداردهی نمی‌شود
 * و Push غیرفعال می‌ماند (بقیه‌ی اپ، از جمله صندوق اعلان‌ها و یادآورهای محلی، کار می‌کنند).
 */
class PushManager(private val context: Context, private val store: SessionStore, private val api: ApiService) {

    fun isFirebaseAvailable(): Boolean = try {
        FirebaseApp.getApps(context).isNotEmpty()
    } catch (e: Exception) {
        false
    }

    private suspend fun currentToken(): String? {
        if (!isFirebaseAvailable()) return null
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            null
        }
    }

    /** true = ثبت موفق (یا نیازی نبود)؛ false = بعداً دوباره تلاش شود */
    suspend fun registerDevice(): Boolean {
        if (!store.isLoggedIn) return true
        val token = currentToken()
        val body = DeviceRegisterRequest(
            deviceId = store.deviceId, fcmToken = token, appVersion = BuildConfig.VERSION_NAME,
            notificationsEnabled = store.pushEnabled,
        )
        return try {
            val resp = api.registerDevice(body)
            resp.isSuccessful || resp.code() in 400..499 && resp.code() != 408 && resp.code() != 429
        } catch (e: IOException) {
            false
        }
    }
}
