package ir.petropro.app.data

import androidx.room.withTransaction
import ir.petropro.app.core.TimeUtils
import ir.petropro.app.database.AppDatabase
import ir.petropro.app.database.ItemEntity
import ir.petropro.app.database.OutboxEntity
import ir.petropro.app.database.OutboxType
import ir.petropro.app.database.SetLogEntity
import ir.petropro.app.network.ApiService
import ir.petropro.app.network.SetLogRequest
import kotlinx.serialization.json.Json
import java.util.UUID

class WorkoutRepository(private val api: ApiService, private val db: AppDatabase, private val json: Json) {

    fun observeSetLogs(date: String) = db.setLogDao().observeForDate(date)

    /**
     * ثبت یا لغوِ یک ست: ابتدا در Room (فوری، آفلاین) و همان لحظه در Outbox با یک کلید یکتا.
     * هر دو در «یک تراکنش» نوشته می‌شوند؛ اگر برنامه وسط کار بسته شود یا هیچ‌کدام ثبت می‌شود یا هر دو.
     */
    suspend fun logSet(
        item: ItemEntity,
        programId: Int,
        dayIndex: Int,
        setNumber: Int,
        done: Boolean,
        weightKg: Double?,
        repsDone: Int?,
    ) {
        val date = TimeUtils.todayTehran()
        val ts = TimeUtils.nowTimestamp()
        val request = SetLogRequest(
            itemId = item.id, setNumber = setNumber, date = date, done = done,
            weightKg = if (done) weightKg else null, repsDone = if (done) repsDone else null,
            clientTs = ts, programId = programId, dayIndex = dayIndex,
            exerciseId = item.exerciseId, orderIndex = item.orderIndex,
        )
        val refKey = refKey(item.id, setNumber, date)
        db.withTransaction {
            db.setLogDao().upsert(
                SetLogEntity(
                    itemId = item.id, setNumber = setNumber, date = date, done = done,
                    weightKg = weightKg, repsDone = repsDone, dirty = true,
                    programId = programId, dayIndex = dayIndex, exerciseId = item.exerciseId, orderIndex = item.orderIndex,
                )
            )
            db.outboxDao().insert(
                OutboxEntity(
                    idemKey = UUID.randomUUID().toString(), type = OutboxType.SET_LOG, path = "api/mobile/sets/log",
                    bodyJson = json.encodeToString(SetLogRequest.serializer(), request),
                    createdAt = System.currentTimeMillis(), refKey = refKey, refDate = date,
                )
            )
        }
    }

    /**
     * وضعیت سرور را برای چند تاریخ می‌گیرد و فقط ردیف‌های «تمیز» (بدون تغییر ارسال‌نشده) را با آن هم‌تراز می‌کند.
     * ردیف‌های dirty دست‌نخورده می‌مانند تا Outbox تکلیفشان را روشن کند (ثبت آفلاین هرگز گم نمی‌شود).
     */
    suspend fun pullServerState(dates: List<String>): Boolean {
        for (date in dates) {
            val resp = api.setsForDate(date)
            if (!resp.isSuccessful) return false
            val remote = resp.body()?.done ?: continue
            val local = db.setLogDao().getForDate(date)
            val remoteKeys = remote.map { it.itemId to it.setNumber }.toSet()
            db.withTransaction {
                for (l in local) {
                    if (!l.dirty && (l.itemId to l.setNumber) !in remoteKeys) {
                        db.setLogDao().delete(l.itemId, l.setNumber, l.date)
                    }
                }
                for (r in remote) {
                    val existing = db.setLogDao().get(r.itemId, r.setNumber, date)
                    if (existing != null && existing.dirty) continue
                    db.setLogDao().upsert(
                        SetLogEntity(
                            itemId = r.itemId, setNumber = r.setNumber, date = date, done = true,
                            weightKg = r.weightKg, repsDone = r.repsDone, dirty = false,
                            programId = existing?.programId ?: 0, dayIndex = existing?.dayIndex ?: 0,
                            exerciseId = existing?.exerciseId ?: 0, orderIndex = existing?.orderIndex ?: 0,
                        )
                    )
                }
            }
        }
        return true
    }

    companion object {
        fun refKey(itemId: Int, setNumber: Int, date: String) = "set:$itemId:$setNumber:$date"
    }
}
