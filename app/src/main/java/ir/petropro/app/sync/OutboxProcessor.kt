package ir.petropro.app.sync

import ir.petropro.app.core.TimeUtils
import ir.petropro.app.data.CheckinRepository
import ir.petropro.app.data.WorkoutRepository
import ir.petropro.app.database.AppDatabase
import ir.petropro.app.database.CheckinEntity
import ir.petropro.app.database.OutboxEntity
import ir.petropro.app.database.OutboxState
import ir.petropro.app.database.OutboxType
import ir.petropro.app.domain.OutboxAction
import ir.petropro.app.domain.OutboxPolicy
import ir.petropro.app.network.ApiErrors
import ir.petropro.app.network.ApiService
import ir.petropro.app.network.CheckinResponse
import ir.petropro.app.network.SetLogRequest
import ir.petropro.app.network.SetLogResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

enum class FlushResult { DONE, NETWORK_ERROR, RETRY_LATER, AUTH_REQUIRED }

/**
 * ارسال Outbox به سرور، به‌ترتیب ثبت. هر عملیات همیشه با «همان» X-Idempotency-Key ارسال می‌شود؛
 * پس قطع و وصل‌شدن‌های پشت‌سرهم یا کرش وسط ارسال، هرگز ثبت تکراری نمی‌سازد.
 */
class OutboxProcessor(
    private val api: ApiService,
    private val db: AppDatabase,
    private val json: Json,
) {
    private val mutex = Mutex()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    suspend fun flush(): FlushResult = withContext(Dispatchers.IO) {
        mutex.withLock { flushLocked() }
    }

    private suspend fun flushLocked(): FlushResult {
        val dao = db.outboxDao()
        while (true) {
            val op = dao.firstPending() ?: return FlushResult.DONE
            val now = System.currentTimeMillis()
            if (op.nextAttemptAt > now) return FlushResult.RETRY_LATER
            val resp = try {
                api.postJson(op.path, op.idemKey, op.bodyJson.toRequestBody(jsonType))
            } catch (e: IOException) {
                dao.update(op.copy(attempts = op.attempts + 1, lastError = e.message))
                return FlushResult.NETWORK_ERROR
            }
            val code = resp.code()
            val raw = try {
                if (resp.isSuccessful) resp.body()?.string() else resp.errorBody()?.string()
            } catch (e: IOException) {
                null
            }
            val err = if (code >= 400) ApiErrors.parse(raw) else null
            when (OutboxPolicy.decide(code, err?.conflict, err?.panelSubExpired == true)) {
                OutboxAction.SUCCESS -> {
                    onSuccess(op, raw)
                    dao.delete(op.seq)
                }
                OutboxAction.RETRY -> {
                    dao.update(
                        op.copy(
                            attempts = op.attempts + 1,
                            nextAttemptAt = now + OutboxPolicy.backoffMillis(op.attempts),
                            lastError = err?.error ?: "HTTP $code",
                        )
                    )
                    return FlushResult.RETRY_LATER
                }
                OutboxAction.AUTH_REQUIRED -> return FlushResult.AUTH_REQUIRED
                OutboxAction.BLOCKED -> dao.update(op.copy(state = OutboxState.BLOCKED, lastError = err?.error))
                OutboxAction.CONFLICT -> {
                    onNotApplied(op)
                    dao.update(op.copy(state = OutboxState.CONFLICT, lastError = err?.error ?: err?.conflict))
                }
                OutboxAction.REJECTED -> {
                    onNotApplied(op)
                    dao.update(op.copy(state = OutboxState.REJECTED, lastError = err?.error ?: "HTTP $code"))
                }
            }
        }
    }

    private suspend fun onSuccess(op: OutboxEntity, raw: String?) {
        when (op.type) {
            OutboxType.SET_LOG -> {
                val req = json.decodeFromString(SetLogRequest.serializer(), op.bodyJson)
                val resp = raw?.let { runCatching { json.decodeFromString(SetLogResponse.serializer(), it) }.getOrNull() }
                var itemId = req.itemId
                val remapped = resp?.remappedItemId
                if (remapped != null && remapped != itemId) {
                    // مربی برنامه را در وب بازسازی کرده؛ ست روی حرکت جدید ثبت شد → ردیف محلی را به شناسه‌ی جدید منتقل کن
                    val old = db.setLogDao().get(itemId, req.setNumber, req.date)
                    if (old != null) {
                        db.setLogDao().delete(itemId, req.setNumber, req.date)
                        db.setLogDao().upsert(old.copy(itemId = remapped))
                    }
                    itemId = remapped
                }
                val ref = WorkoutRepository.refKey(req.itemId, req.setNumber, req.date)
                if (db.outboxDao().laterPendingCount(ref, op.seq) == 0) {
                    val row = db.setLogDao().get(itemId, req.setNumber, req.date)
                    if (row != null) {
                        if (row.done) db.setLogDao().markClean(itemId, req.setNumber, req.date)
                        else db.setLogDao().delete(itemId, req.setNumber, req.date)
                    }
                }
            }
            OutboxType.CHECKIN -> {
                val req = json.decodeFromString(ir.petropro.app.network.CheckinRequest.serializer(), op.bodyJson)
                val resp = raw?.let { runCatching { json.decodeFromString(CheckinResponse.serializer(), it) }.getOrNull() }
                if (resp?.conflict == "server_newer" && req.date == TimeUtils.todayTehran()) {
                    // نسخه‌ی جدیدترِ سرور (مثلاً ثبت از وب) برنده شد؛ مقدار واقعی را بگیر تا صفحه دروغ نگوید
                    adoptServerCheckin(req.date)
                }
                if (db.outboxDao().laterPendingCount(CheckinRepository.refKey(req.date), op.seq) == 0) {
                    db.checkinDao().markClean(req.date)
                }
            }
        }
    }

    private suspend fun adoptServerCheckin(date: String) {
        val resp = try { api.checkinToday() } catch (e: IOException) { return }
        val c = resp.body()?.checkin ?: return
        db.checkinDao().upsert(
            CheckinEntity(
                date = date, sleepHours = c.sleepHours, sleepQuality = c.sleepQuality ?: 3, energy = c.energy ?: 3,
                fatigue = c.fatigue ?: 3, stress = c.stress ?: 3, soreness = c.soreness ?: 3,
                painLevel = c.painLevel ?: 0, note = c.note, dirty = false,
            )
        )
    }

    /** عملیاتی که سرور نپذیرفت: وضعیتِ «در انتظار»ِ محلی را برنمی‌گردانیم که کاربر فکر نکند ثبت شده */
    private suspend fun onNotApplied(op: OutboxEntity) {
        when (op.type) {
            OutboxType.SET_LOG -> {
                val req = json.decodeFromString(SetLogRequest.serializer(), op.bodyJson)
                val ref = WorkoutRepository.refKey(req.itemId, req.setNumber, req.date)
                if (db.outboxDao().laterPendingCount(ref, op.seq) == 0) {
                    db.setLogDao().delete(req.itemId, req.setNumber, req.date)
                }
            }
            OutboxType.CHECKIN -> {
                val req = json.decodeFromString(ir.petropro.app.network.CheckinRequest.serializer(), op.bodyJson)
                db.checkinDao().markClean(req.date)
            }
        }
    }
}
