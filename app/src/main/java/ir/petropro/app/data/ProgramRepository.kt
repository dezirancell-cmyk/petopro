package ir.petropro.app.data

import androidx.room.withTransaction
import ir.petropro.app.database.AppDatabase
import ir.petropro.app.database.DayEntity
import ir.petropro.app.database.ItemEntity
import ir.petropro.app.database.ProgramEntity
import ir.petropro.app.network.ApiService
import ir.petropro.app.network.ProgramDto
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class ProgramRepository(private val api: ApiService, private val db: AppDatabase) {
    private val json = Json

    fun observePrograms() = db.programDao().observeAll()
    fun observeDays(programId: Int) = db.programDao().observeDays(programId)
    fun observeDayCounts(programId: Int) = db.programDao().observeDayCounts(programId)
    fun observeDay(dayId: Int) = db.programDao().observeDay(dayId)
    fun observeItems(dayId: Int) = db.programDao().observeItems(dayId)

    /**
     * برنامه‌ها را با Manifest سرور مقایسه و فقط موارد تغییرکرده را دانلود می‌کند.
     * خطای شبکه به بالا پرتاب می‌شود (IOException) تا SyncManager وضعیت را درست گزارش کند.
     */
    suspend fun refresh(): RefreshResult {
        val manifestResp = api.manifest()
        if (!manifestResp.isSuccessful) return RefreshResult.Http(manifestResp.code())
        val manifest = manifestResp.body() ?: return RefreshResult.Http(500)
        val local = db.programDao().getAll().associateBy { it.id }
        var updated = 0
        manifest.programs.forEachIndexed { index, mp ->
            val existing = local[mp.id]
            if (existing != null && existing.contentHash == mp.hash) {
                if (existing.serverOrder != index || existing.active != mp.active) {
                    db.programDao().insertProgram(existing.copy(serverOrder = index, active = mp.active))
                }
                return@forEachIndexed
            }
            val resp = api.program(mp.id)
            if (!resp.isSuccessful) return RefreshResult.Http(resp.code())
            val dto = resp.body()?.program ?: return RefreshResult.Http(500)
            store(dto, mp.hash, index)
            updated++
        }
        val remoteIds = manifest.programs.map { it.id }.toSet()
        val removed = local.keys.filter { it !in remoteIds }
        removed.forEach { db.programDao().delete(it) }
        return RefreshResult.Ok(updated = updated, removed = removed.size)
    }

    private suspend fun store(dto: ProgramDto, hash: String, order: Int) {
        val days = dto.days.map { DayEntity(it.id, dto.id, it.dayIndex, it.dayName) }
        val items = dto.days.flatMap { day ->
            day.items.map { it ->
                ItemEntity(
                    id = it.id, dayId = day.id, exerciseId = it.exerciseId ?: 0, name = it.name,
                    nameFa = it.nameFa?.takeIf { n -> n.isNotBlank() }, equipmentFa = it.equipmentFa,
                    musclesFa = json.encodeToString(ListSerializer(String.serializer()), it.primaryMusclesFa),
                    imageUrlsJson = json.encodeToString(ListSerializer(String.serializer()), it.images),
                    instructionsFa = json.encodeToString(ListSerializer(String.serializer()), it.instructionsFa),
                    instructionsEn = json.encodeToString(ListSerializer(String.serializer()), it.instructions),
                    sets = it.sets ?: 0, reps = it.reps, restSeconds = it.restSeconds, weightKg = it.weightKg,
                    setTimeSeconds = it.setTimeSeconds, notes = it.notes?.takeIf { n -> n.isNotBlank() },
                    orderIndex = it.orderIndex, isWarmup = it.isWarmup,
                )
            }
        }
        db.withTransaction {
            // حذف قبلی (cascade روزها و حرکات) و درج نسخه‌ی جدید در یک تراکنش: یا همه یا هیچ
            db.programDao().delete(dto.id)
            db.programDao().insertProgram(ProgramEntity(dto.id, dto.athleteId, dto.title, dto.notes, dto.active, hash, order))
            db.programDao().insertDays(days)
            db.programDao().insertItems(items)
        }
    }
}

sealed class RefreshResult {
    data class Ok(val updated: Int, val removed: Int) : RefreshResult()
    data class Http(val code: Int) : RefreshResult()
}
