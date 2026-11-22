package ir.petropro.app.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "programs")
data class ProgramEntity(
    @PrimaryKey val id: Int,
    val athleteId: Int,
    val title: String,
    val notes: String?,
    val active: Boolean,
    /** hash محتوایی سرور؛ با تغییر مربی در وب عوض می‌شود */
    val contentHash: String,
    /** ترتیب نمایش (همان ترتیب سرور: جدیدترین اول) */
    val serverOrder: Int,
)

@Entity(
    tableName = "days",
    foreignKeys = [ForeignKey(entity = ProgramEntity::class, parentColumns = ["id"], childColumns = ["programId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("programId")],
)
data class DayEntity(
    @PrimaryKey val id: Int,
    val programId: Int,
    val dayIndex: Int,
    val dayName: String,
)

@Entity(
    tableName = "items",
    foreignKeys = [ForeignKey(entity = DayEntity::class, parentColumns = ["id"], childColumns = ["dayId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("dayId")],
)
data class ItemEntity(
    @PrimaryKey val id: Int,
    val dayId: Int,
    val exerciseId: Int,
    val name: String,
    val nameFa: String?,
    val equipmentFa: String?,
    val musclesFa: String,          // JSON آرایه‌ی رشته
    val imageUrlsJson: String,      // JSON آرایه‌ی رشته (آدرس‌های رسانه)
    val instructionsFa: String,     // JSON آرایه‌ی رشته
    val instructionsEn: String,     // JSON آرایه‌ی رشته
    val sets: Int,
    val reps: String?,
    val restSeconds: Int?,
    val weightKg: Double?,
    val setTimeSeconds: Int?,
    val notes: String?,
    val orderIndex: Int,
    val isWarmup: Boolean,
)

/**
 * وضعیت محلیِ یک ست. کلید شامل itemId است ولی به items وابسته (FK) نیست، چون با ذخیره‌ی برنامه
 * توسط مربی در وب شناسه‌ی حرکت‌ها عوض می‌شود و ثبت‌های آفلاین نباید همراه آن پاک شوند.
 */
@Entity(tableName = "set_logs", primaryKeys = ["itemId", "setNumber", "date"])
data class SetLogEntity(
    val itemId: Int,
    val setNumber: Int,
    val date: String,
    val done: Boolean,
    val weightKg: Double?,
    val repsDone: Int?,
    /** true یعنی تغییر هنوز به سرور تأیید نشده (در Outbox است) */
    val dirty: Boolean,
    val programId: Int,
    val dayIndex: Int,
    val exerciseId: Int,
    val orderIndex: Int,
)

@Entity(tableName = "checkins")
data class CheckinEntity(
    @PrimaryKey val date: String,
    val sleepHours: Double?,
    val sleepQuality: Int,
    val energy: Int,
    val fatigue: Int,
    val stress: Int,
    val soreness: Int,
    val painLevel: Int,
    val note: String?,
    val dirty: Boolean,
)

object OutboxState {
    const val PENDING = "PENDING"
    const val BLOCKED = "BLOCKED"     // اشتراک پنل تمام شده؛ پس از تمدید دوباره تلاش می‌شود
    const val CONFLICT = "CONFLICT"    // تعارض قطعی (مثلاً حرکت از برنامه حذف شده)
    const val REJECTED = "REJECTED"    // سرور داده را نپذیرفته (۴xx قطعی)
}

object OutboxType {
    const val SET_LOG = "SET_LOG"
    const val CHECKIN = "CHECKIN"
    /** علامت‌گذاری اعلان‌ها به‌عنوان خوانده‌شده؛ مهم نیست و در شمارش «ارسال‌نشده‌ها» نمی‌آید */
    const val NOTIF_READ = "NOTIF_READ"
}

/** صف عملیات آفلاین. ترتیب ارسال = ترتیب seq. idemKey همان X-Idempotency-Key است و هرگز عوض نمی‌شود. */
@Entity(tableName = "outbox", indices = [Index(value = ["idemKey"], unique = true), Index("state")])
data class OutboxEntity(
    @PrimaryKey(autoGenerate = true) val seq: Long = 0,
    val idemKey: String,
    val type: String,
    val path: String,
    val bodyJson: String,
    val createdAt: Long,
    val state: String = OutboxState.PENDING,
    val attempts: Int = 0,
    val nextAttemptAt: Long = 0,
    val lastError: String? = null,
    /** کلید مرجع (مثلاً itemId:setNumber:date) برای به‌روزرسانی وضعیت محلی بعد از ارسال */
    @ColumnInfo(defaultValue = "") val refKey: String = "",
    val refDate: String? = null,
)

object MediaState {
    const val NEEDED = "NEEDED"
    const val DONE = "DONE"
    const val FAILED = "FAILED"
    const val PAUSED_LOW_STORAGE = "PAUSED_LOW_STORAGE"
}

@Entity(tableName = "media")
data class MediaEntity(
    @PrimaryKey val url: String,
    val kind: String,
    val isLocal: Boolean,
    val expectedSha256: String?,
    val expectedSize: Long?,
    val sizeBytes: Long = 0,
    val localPath: String? = null,
    val state: String = MediaState.NEEDED,
    val attempts: Int = 0,
    val lastError: String? = null,
)

data class DoneMedia(val url: String, val localPath: String?)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: Int,
    val kind: String,
    val title: String,
    val body: String?,
    val route: String?,
    val createdAt: String,
    val isRead: Boolean,
)
