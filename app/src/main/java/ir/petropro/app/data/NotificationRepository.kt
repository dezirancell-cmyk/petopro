package ir.petropro.app.data

import androidx.room.withTransaction
import ir.petropro.app.database.AppDatabase
import ir.petropro.app.database.NotificationEntity
import ir.petropro.app.database.OutboxEntity
import ir.petropro.app.database.OutboxType
import ir.petropro.app.network.ApiService
import ir.petropro.app.network.NotificationsReadRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.IOException
import java.util.UUID

/** صندوق اعلان‌ها: روی گوشی ذخیره می‌شود تا آفلاین هم قابل مشاهده باشد. */
class NotificationRepository(private val api: ApiService, private val db: AppDatabase, private val json: Json) {

    fun observeAll() = db.notificationDao().observeAll()
    fun observeUnreadCount() = db.notificationDao().observeUnreadCount()

    /** اعلان‌های جدیدتر از آخرین شناسه‌ی محلی را (صفحه‌به‌صفحه) می‌گیرد. false = ناموفق (آفلاین/خطا) */
    suspend fun refresh(): Boolean {
        return try {
            var afterId = db.notificationDao().maxId().takeIf { it > 0 }
            repeat(MAX_PAGES) {
                val resp = api.notifications(afterId = afterId, limit = 100)
                if (!resp.isSuccessful) return false
                val body = resp.body() ?: return false
                if (body.notifications.isNotEmpty()) {
                    db.notificationDao().upsertAll(body.notifications.map { dto ->
                        NotificationEntity(
                            id = dto.id, kind = dto.kind, title = dto.title, body = dto.body,
                            route = (dto.data["route"] as? JsonPrimitive)?.contentOrNull,
                            createdAt = dto.createdAt, isRead = dto.read,
                        )
                    })
                    afterId = body.notifications.maxOf { it.id }
                }
                if (!body.hasMore) return true
            }
            true
        } catch (e: IOException) {
            false
        }
    }

    /** خوانده‌شدن را فوراً محلی ثبت می‌کند و برای سرور در Outbox می‌گذارد (آفلاین هم کار می‌کند) */
    suspend fun markRead(ids: List<Int>) {
        if (ids.isEmpty()) return
        db.withTransaction {
            db.notificationDao().markRead(ids)
            enqueue(NotificationsReadRequest(ids = ids))
        }
    }

    suspend fun markAllRead() {
        db.withTransaction {
            db.notificationDao().markAllRead()
            enqueue(NotificationsReadRequest(all = true))
        }
    }

    private suspend fun enqueue(req: NotificationsReadRequest) {
        db.outboxDao().insert(
            OutboxEntity(
                idemKey = UUID.randomUUID().toString(), type = OutboxType.NOTIF_READ, path = "api/mobile/notifications/read",
                bodyJson = json.encodeToString(NotificationsReadRequest.serializer(), req),
                createdAt = System.currentTimeMillis(), refKey = "notif-read",
            )
        )
    }

    private companion object {
        const val MAX_PAGES = 10
    }
}
