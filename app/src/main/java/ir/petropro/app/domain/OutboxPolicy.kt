package ir.petropro.app.domain

enum class OutboxAction {
    /** سرور پذیرفت (یا قبلاً پذیرفته بود) → از صف حذف */
    SUCCESS,
    /** خطای موقت → همان کلید دوباره، با تأخیر */
    RETRY,
    /** توکن نامعتبر → ورود مجدد؛ صف دست‌نخورده می‌ماند */
    AUTH_REQUIRED,
    /** اشتراک پنل تمام شده → معلق؛ بعد از تمدید دستی دوباره تلاش می‌شود */
    BLOCKED,
    /** تعارضِ قطعی (مثلاً حرکت از برنامه حذف شده) → به کاربر نشان داده می‌شود */
    CONFLICT,
    /** سرور داده را رد کرد (۴xx قطعی) → به کاربر نشان داده می‌شود */
    REJECTED,
}

/** قراردادِ پاسخ‌های سرور نسخه ۵۱۱ (docs: MOBILE_API_511.md، بخش «قواعد کلاینت») */
object OutboxPolicy {
    fun decide(httpCode: Int, conflict: String?, panelSubExpired: Boolean): OutboxAction = when {
        httpCode in 200..299 -> OutboxAction.SUCCESS
        httpCode == 401 -> OutboxAction.AUTH_REQUIRED
        httpCode == 403 && panelSubExpired -> OutboxAction.BLOCKED
        httpCode == 409 && !conflict.isNullOrEmpty() -> OutboxAction.CONFLICT
        httpCode == 409 -> OutboxAction.RETRY          // request_in_progress
        httpCode == 408 || httpCode == 425 || httpCode == 429 -> OutboxAction.RETRY
        httpCode >= 500 -> OutboxAction.RETRY
        else -> OutboxAction.REJECTED
    }

    /** تأخیر تلاش بعدی: ۵ ثانیه × 2^تلاش، حداکثر ۱۵ دقیقه */
    fun backoffMillis(attempts: Int): Long {
        val exp = attempts.coerceIn(0, 10)
        return minOf(5_000L * (1L shl exp), 15 * 60_000L)
    }
}
