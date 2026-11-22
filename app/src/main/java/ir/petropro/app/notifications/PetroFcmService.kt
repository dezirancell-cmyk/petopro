package ir.petropro.app.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import ir.petropro.app.PetroProApp

class PetroFcmService : FirebaseMessagingService() {

    /** توکن جدید → ثبت دوباره روی سرور (با WorkManager و تلاش مجدد در قطعی) */
    override fun onNewToken(token: String) {
        val container = (application as PetroProApp).container
        if (container.sessionStore.isLoggedIn) container.scheduler.registerDevice()
    }

    /**
     * فقط وقتی برنامه در پیش‌زمینه است صدا زده می‌شود (در پس‌زمینه خودِ سیستم اعلان را نشان می‌دهد و با لمس آن،
     * extras همین data به MainActivity می‌رسد). صندوق اعلان‌ها با همگام‌سازی بعدی به‌روز می‌شود.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        val container = (application as PetroProApp).container
        if (!container.sessionStore.isLoggedIn) return
        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: return
        val body = message.notification?.body ?: data["body"]
        val nid = data["nid"]?.toIntOrNull()
        NotificationHelper.show(
            this, nid ?: System.currentTimeMillis().toInt(), data["kind"] ?: "general", title, body, data["route"], nid
        )
        container.scheduler.syncSoon()
    }
}
