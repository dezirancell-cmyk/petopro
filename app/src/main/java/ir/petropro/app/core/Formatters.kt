package ir.petropro.app.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    fun bytes(value: Long): String {
        if (value < 0) return "-"
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        return when {
            value >= gb -> String.format(Locale.US, "%.1f GB", value / gb)
            value >= mb -> String.format(Locale.US, "%.1f MB", value / mb)
            value >= kb -> String.format(Locale.US, "%.0f KB", value / kb)
            else -> "$value B"
        }
    }

    fun dateTime(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(millis))

    /** ارقام فارسی/عربی و جداکننده‌ی اعشار فارسی را به ارقام لاتین تبدیل می‌کند تا toDouble() کار کند */
    fun normalizeDigits(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            sb.append(
                when (ch) {
                    in '\u06F0'..'\u06F9' -> '0' + (ch - '\u06F0')
                    in '\u0660'..'\u0669' -> '0' + (ch - '\u0660')
                    '\u066B', '\u066C', ',' -> '.'
                    else -> ch
                }
            )
        }
        return sb.toString().trim()
    }

    fun weight(value: Double?): String =
        if (value == null) "" else if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}
