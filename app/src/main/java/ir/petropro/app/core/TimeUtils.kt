package ir.petropro.app.core

import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** سرور همه‌ی تاریخ‌ها را به وقت تهران و بدون tzinfo می‌خواهد؛ همین‌جا یکجا مدیریت می‌شود. */
object TimeUtils {
    val TEHRAN: ZoneId = ZoneId.of("Asia/Tehran")
    private val TS_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    /** تاریخ امروز (میلادی) به‌صورت YYYY-MM-DD به وقت تهران */
    fun todayTehran(clock: Clock = Clock.system(TEHRAN)): String = LocalDate.now(clock).toString()

    fun yesterdayTehran(clock: Clock = Clock.system(TEHRAN)): String = LocalDate.now(clock).minusDays(1).toString()

    /** زمان ثبت (client_ts) به وقت تهران: yyyy-MM-ddTHH:mm:ss */
    fun nowTimestamp(clock: Clock = Clock.system(TEHRAN)): String =
        LocalDateTime.now(clock).withNano(0).format(TS_FORMAT)

    /** عدد اولِ یک متن تکرار مثل «8-12» → 8 ؛ اگر عددی نبود null */
    fun firstInt(text: String?): Int? =
        text?.let { Regex("\\d+").find(it)?.value?.toIntOrNull() }
}
