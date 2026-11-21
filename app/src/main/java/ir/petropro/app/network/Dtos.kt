package ir.petropro.app.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** ساختارها دقیقاً از پاسخ‌های واقعی سرور نسخه ۵۱۱ گرفته شده‌اند (نمونه‌ها در app/src/test/resources/fixtures). */

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_name") val deviceName: String? = null,
)

@Serializable
data class UserDto(
    val id: Int,
    val username: String = "",
    val role: String = "",
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("coach_id") val coachId: Int? = null,
)

@Serializable
data class LoginResponse(
    val ok: Boolean = false,
    val token: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    val user: UserDto? = null,
    val error: String? = null,
)

@Serializable
data class ConfigResponse(
    val ok: Boolean = false,
    @SerialName("api_version") val apiVersion: Int = 0,
    @SerialName("push_enabled") val pushEnabled: Boolean = false,
    @SerialName("online_only_features") val onlineOnlyFeatures: List<String> = emptyList(),
)

@Serializable
data class ManifestProgramDto(
    val id: Int,
    val title: String = "",
    val active: Boolean = false,
    val hash: String = "",
)

@Serializable
data class MediaSummaryDto(val count: Int = 0, val signature: String = "")

@Serializable
data class ManifestResponse(
    val ok: Boolean = false,
    val programs: List<ManifestProgramDto> = emptyList(),
    val media: MediaSummaryDto = MediaSummaryDto(),
    @SerialName("unread_notifications") val unreadNotifications: Int = 0,
)

@Serializable
data class ItemDto(
    val id: Int,
    @SerialName("exercise_id") val exerciseId: Int? = null,
    val name: String = "",
    @SerialName("name_fa") val nameFa: String? = null,
    @SerialName("equipment_fa") val equipmentFa: String? = null,
    @SerialName("primary_muscles_fa") val primaryMusclesFa: List<String> = emptyList(),
    val images: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    @SerialName("instructions_fa") val instructionsFa: List<String> = emptyList(),
    val sets: Int? = null,
    val reps: String? = null,
    @SerialName("rest_seconds") val restSeconds: Int? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("set_time_seconds") val setTimeSeconds: Int? = null,
    val notes: String? = null,
    @SerialName("order_index") val orderIndex: Int = 0,
    @SerialName("is_warmup") val isWarmup: Boolean = false,
)

@Serializable
data class DayDto(
    val id: Int,
    @SerialName("day_index") val dayIndex: Int = 0,
    @SerialName("day_name") val dayName: String = "",
    val items: List<ItemDto> = emptyList(),
)

@Serializable
data class ProgramDto(
    val id: Int,
    val title: String = "",
    val notes: String? = null,
    val active: Boolean = false,
    @SerialName("athlete_id") val athleteId: Int = 0,
    val days: List<DayDto> = emptyList(),
)

@Serializable
data class ProgramResponse(val ok: Boolean = false, val program: ProgramDto? = null)

@Serializable
data class MediaDto(
    val url: String,
    val kind: String = "image",
    @SerialName("exercise_ids") val exerciseIds: List<Int> = emptyList(),
    val local: Boolean = false,
    val exists: Boolean = true,
    val size: Long? = null,
    val sha256: String? = null,
)

@Serializable
data class MediaManifestResponse(
    val ok: Boolean = false,
    val count: Int = 0,
    @SerialName("total_bytes") val totalBytes: Long = 0,
    val media: List<MediaDto> = emptyList(),
)

@Serializable
data class SetDoneDto(
    @SerialName("item_id") val itemId: Int,
    @SerialName("set_number") val setNumber: Int,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("reps_done") val repsDone: Int? = null,
    val rpe: Int? = null,
    val note: String? = null,
)

@Serializable
data class SetsTodayResponse(val ok: Boolean = false, val done: List<SetDoneDto> = emptyList())

/** بدنه‌ی ثبت ست که در Outbox به‌صورت JSON ذخیره و ارسال می‌شود */
@Serializable
data class SetLogRequest(
    @SerialName("item_id") val itemId: Int,
    @SerialName("set_number") val setNumber: Int,
    val date: String,
    val done: Boolean,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("reps_done") val repsDone: Int? = null,
    val rpe: Int? = null,
    val note: String? = null,
    @SerialName("client_ts") val clientTs: String,
    @SerialName("program_id") val programId: Int? = null,
    @SerialName("day_index") val dayIndex: Int? = null,
    @SerialName("exercise_id") val exerciseId: Int? = null,
    @SerialName("order_index") val orderIndex: Int? = null,
)

@Serializable
data class SetLogResponse(
    val ok: Boolean = false,
    val done: Boolean? = null,
    val unchanged: Boolean? = null,
    @SerialName("remapped_item_id") val remappedItemId: Int? = null,
    val conflict: String? = null,
    val error: String? = null,
)

@Serializable
data class CheckinRequest(
    val date: String,
    @SerialName("sleep_hours") val sleepHours: Double? = null,
    @SerialName("sleep_quality") val sleepQuality: Int? = null,
    val energy: Int? = null,
    val fatigue: Int? = null,
    val stress: Int? = null,
    val soreness: Int? = null,
    @SerialName("pain_level") val painLevel: Int? = null,
    val note: String? = null,
    @SerialName("client_ts") val clientTs: String,
)

@Serializable
data class CheckinResponse(
    val ok: Boolean = false,
    val applied: Boolean? = null,
    val conflict: String? = null,
    val readiness: JsonElement? = null,
    val error: String? = null,
)

/** بدنه‌ی خطای عمومی سرور: {ok:false,error:"..",conflict?,code?,panel_sub_expired?} */
@Serializable
data class ErrorBody(
    val ok: Boolean = false,
    val error: String? = null,
    val conflict: String? = null,
    val code: String? = null,
    @SerialName("panel_sub_expired") val panelSubExpired: Boolean = false,
)

@Serializable
data class CheckinValuesDto(
    @SerialName("sleep_hours") val sleepHours: Double? = null,
    @SerialName("sleep_quality") val sleepQuality: Int? = null,
    val energy: Int? = null,
    val fatigue: Int? = null,
    val stress: Int? = null,
    val soreness: Int? = null,
    @SerialName("pain_level") val painLevel: Int? = null,
    val note: String? = null,
)

@Serializable
data class CheckinTodayResponse(val ok: Boolean = false, val checkin: CheckinValuesDto? = null)
