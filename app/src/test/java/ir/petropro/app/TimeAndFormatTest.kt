package ir.petropro.app

import ir.petropro.app.core.Formatters
import ir.petropro.app.core.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant

class TimeAndFormatTest {
    // 20:45 UTC = 00:15 روز بعد به وقت تهران (UTC+3:30)
    private val clock = Clock.fixed(Instant.parse("2026-11-20T20:45:00Z"), TimeUtils.TEHRAN)

    @Test fun todayUsesTehranTimeZone() = assertEquals("2026-11-21", TimeUtils.todayTehran(clock))
    @Test fun yesterday() = assertEquals("2026-11-20", TimeUtils.yesterdayTehran(clock))
    @Test fun timestampFormatHasSecondsAndNoOffset() = assertEquals("2026-11-21T00:15:00", TimeUtils.nowTimestamp(clock))

    @Test fun firstInt() {
        assertEquals(8, TimeUtils.firstInt("8-12"))
        assertEquals(10, TimeUtils.firstInt("10"))
        assertNull(TimeUtils.firstInt("تا ناتوانی"))
        assertNull(TimeUtils.firstInt(null))
    }

    @Test fun persianDigitsAreNormalized() {
        assertEquals("72.5", Formatters.normalizeDigits("۷۲٫۵"))
        assertEquals("12", Formatters.normalizeDigits("١٢"))
    }

    @Test fun weightFormatting() {
        assertEquals("60", Formatters.weight(60.0))
        assertEquals("62.5", Formatters.weight(62.5))
        assertEquals("", Formatters.weight(null))
    }

    @Test fun bytesFormatting() {
        assertEquals("512 B", Formatters.bytes(512))
        assertEquals("2.0 MB", Formatters.bytes(2L * 1024 * 1024))
    }
}
