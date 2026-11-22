package ir.petropro.app.domain

/** مقصدِ لمس اعلان؛ مقدار data.route سرور (مثلاً injury/12) به مقصد تبدیل می‌شود. */
sealed class Route {
    data object Home : Route()
    data object Checkin : Route()
    data object Announcements : Route()
    data object Profile : Route()
    data class Program(val id: Int?) : Route()
    data object ProgramRequests : Route()
    data class Injury(val reportId: Int) : Route()
}

object Routes {
    fun parse(raw: String?): Route {
        val r = raw?.trim().orEmpty()
        return when {
            r.isEmpty() || r == "home" -> Route.Home
            r == "checkin" -> Route.Checkin
            r == "announcements" -> Route.Announcements
            r == "profile" -> Route.Profile
            r == "program-requests" -> Route.ProgramRequests
            r == "program" -> Route.Program(null)
            r.startsWith("program/") -> Route.Program(r.substringAfter('/').toIntOrNull())
            r.startsWith("injury/") -> r.substringAfter('/').toIntOrNull()?.let { Route.Injury(it) } ?: Route.Home
            else -> Route.Home
        }
    }

    /** نگاشت نوع اعلان به کانالِ اندروید — دقیقاً مثل سرور (_fcm_channel در varzesh511.py) */
    fun channelFor(kind: String): String = when {
        kind.startsWith("injury") -> "injury"
        kind.startsWith("program") || kind.startsWith("diet") -> "program"
        kind == "announcement" -> "announcements"
        kind.endsWith("_reminder") || kind.startsWith("sub_") -> "reminders"
        else -> "general"
    }
}

/** محاسبه‌ی تأخیر تا نوبت بعدیِ یادآور روزانه (خالص و قابل‌تست) */
object ReminderTime {
    private const val DAY_MS = 24L * 60 * 60_000L

    /** nowMillisOfDay = میلی‌ثانیه‌ی گذشته از نیمه‌شبِ محلی. اگر ساعت امروز گذشته باشد، فردا. */
    fun nextDelayMillis(nowMillisOfDay: Long, hour: Int, minute: Int): Long {
        val target = (hour * 60L + minute) * 60_000L
        val delta = target - nowMillisOfDay
        return if (delta > 1_000L) delta else delta + DAY_MS
    }
}
