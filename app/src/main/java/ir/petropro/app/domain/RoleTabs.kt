package ir.petropro.app.domain

data class TabDef(val key: String, val label: String, val icon: String)

/**
 * ساختار تب‌های هر نقش، دقیقاً مطابق TABS / CORE_TAB_KEYS / EXTRA_TAB_KEYS در وب (varzesh510.py).
 * فقط تب‌هایی که در اپ پیاده‌سازی شده‌اند (IMPLEMENTED) نمایش داده می‌شوند؛ هیچ دکمه‌ی بی‌عملکردی نمی‌سازیم.
 * با پیشرفت فازها کلیدها به IMPLEMENTED اضافه می‌شوند.
 */
object RoleTabs {
    const val ADMIN = "admin"
    const val COACH = "coach"
    const val ATHLETE = "athlete"
    const val PRO_ATHLETE = "pro_athlete"

    private val dashboard = TabDef("dashboard", "داشبورد", "layout-dashboard")
    private val myprogram = TabDef("myprogram", "برنامه من", "clipboard-list")
    private val progress = TabDef("progress", "پیشرفت من", "chart-bar")
    private val readiness = TabDef("readiness", "آمادگی", "heartbeat")
    private val aiCoach = TabDef("ai_coach", "AI Coach", "sparkles")
    private val recommendations = TabDef("recommendations", "پیشنهادهای هوشمند", "bulb")
    private val programs = TabDef("programs", "تمرین ورزشکاران", "clipboard-list")
    private val nutrition = TabDef("nutrition", "تغذیه ورزشکاران", "apple")
    private val checkin = TabDef("checkin", "ثبت حضور", "door-enter")
    private val analysis = TabDef("athlete_analysis", "آنالیز ورزشکاران", "scan")
    private val coachAssistant = TabDef("ai_coach_assistant", "دستیارِ هوشمندِ مربی", "sparkles")
    private val injuries = TabDef("injuries", "آسیب‌دیدگی", "bandage")
    private val athletesProgress = TabDef("athletes_progress", "پیشرفت ورزشکاران", "chart-bar")
    private val library = TabDef("library", "کتابخانه دستگاه‌ها", "barbell")

    private val ALL: Map<String, List<TabDef>> = mapOf(
        ADMIN to listOf(
            dashboard, myprogram, progress,
            TabDef("users", "کاربران", "users"), library, programs, nutrition, checkin, analysis, recommendations,
            TabDef("learning", "یادگیریِ داده‌محور", "chart-dots"), coachAssistant,
            TabDef("ai_audit", "بازرسیِ AI Coach", "shield-check"),
            TabDef("requests", "درخواست‌های برنامه", "inbox"), injuries, athletesProgress,
            TabDef("settings", "تنظیمات", "settings"),
        ),
        COACH to listOf(
            dashboard, myprogram, progress,
            TabDef("athletes", "ورزشکاران من", "users"), programs, nutrition, library, checkin, analysis,
            recommendations, coachAssistant, injuries, athletesProgress,
        ),
        // در وب «recommendations» در TABS ورزشکار هست ولی در EXTRA_TAB_KEYS او نیست (فقط پرو) → برای ورزشکار نمایش داده نمی‌شود
        ATHLETE to listOf(dashboard, readiness, myprogram, progress, aiCoach),
        PRO_ATHLETE to listOf(dashboard, readiness, myprogram, progress, aiCoach, recommendations),
    )

    /** تب‌های اصلی منوی شناور پایین (CORE_TAB_KEYS در وب) */
    private val CORE: Map<String, List<String>> = mapOf(
        ADMIN to listOf("dashboard", "myprogram", "progress"),
        COACH to listOf("dashboard", "myprogram", "progress"),
        ATHLETE to listOf("dashboard", "readiness", "myprogram", "progress"),
        PRO_ATHLETE to listOf("dashboard", "readiness", "myprogram", "progress"),
    )

    /** تب‌هایی که تا این فاز در اپ کار می‌کنند */
    val IMPLEMENTED: Set<String> = setOf("dashboard", "myprogram", "readiness", "injuries")

    private fun roleOrDefault(role: String?) = if (role != null && ALL.containsKey(role)) role else ATHLETE

    /** همه‌ی تب‌های وب برای نقش (برای مستندسازی/تست) */
    fun allInWeb(role: String?): List<TabDef> = ALL.getValue(roleOrDefault(role))

    fun available(role: String?): List<TabDef> = allInWeb(role).filter { it.key in IMPLEMENTED }

    fun core(role: String?): List<TabDef> {
        val keys = CORE.getValue(roleOrDefault(role))
        return available(role).filter { it.key in keys }
    }

    /** تب‌های غیرِ منوی پایین؛ در وب به‌صورت دکمه‌های موزاییکی بالای داشبورد می‌آیند */
    fun extras(role: String?): List<TabDef> {
        val keys = CORE.getValue(roleOrDefault(role))
        return available(role).filter { it.key !in keys }
    }

    fun isAthleteRole(role: String?) = role == ATHLETE || role == PRO_ATHLETE

    fun find(role: String?, key: String): TabDef? = allInWeb(role).firstOrNull { it.key == key }
}
