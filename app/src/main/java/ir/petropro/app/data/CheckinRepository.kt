package ir.petropro.app.data

import androidx.room.withTransaction
import ir.petropro.app.core.TimeUtils
import ir.petropro.app.database.AppDatabase
import ir.petropro.app.database.CheckinEntity
import ir.petropro.app.database.OutboxEntity
import ir.petropro.app.database.OutboxType
import ir.petropro.app.network.CheckinRequest
import kotlinx.serialization.json.Json
import java.util.UUID

class CheckinRepository(private val db: AppDatabase, private val json: Json) {

    fun observeToday() = db.checkinDao().observe(TimeUtils.todayTehran())

    /** ذخیره‌ی چک‌این‌ِ امروز در Room + Outbox در یک تراکنش. ویرایش دوباره در همان روز یک عملیات تازه می‌سازد (سرور: جدیدترین برنده). */
    suspend fun save(entity: CheckinEntity) {
        val date = entity.date
        val request = CheckinRequest(
            date = date, sleepHours = entity.sleepHours, sleepQuality = entity.sleepQuality, energy = entity.energy,
            fatigue = entity.fatigue, stress = entity.stress, soreness = entity.soreness, painLevel = entity.painLevel,
            note = entity.note, clientTs = TimeUtils.nowTimestamp(),
        )
        db.withTransaction {
            db.checkinDao().upsert(entity.copy(dirty = true))
            db.outboxDao().insert(
                OutboxEntity(
                    idemKey = UUID.randomUUID().toString(), type = OutboxType.CHECKIN, path = "api/mobile/checkin",
                    bodyJson = json.encodeToString(CheckinRequest.serializer(), request),
                    createdAt = System.currentTimeMillis(), refKey = refKey(date), refDate = date,
                )
            )
        }
    }

    companion object {
        fun refKey(date: String) = "checkin:$date"
    }
}
