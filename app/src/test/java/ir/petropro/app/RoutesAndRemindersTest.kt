package ir.petropro.app

import ir.petropro.app.domain.ReminderTime
import ir.petropro.app.domain.Route
import ir.petropro.app.domain.Routes
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesAndRemindersTest {
    @Test fun routesFromServerContract() {
        assertEquals(Route.Injury(12), Routes.parse("injury/12"))
        assertEquals(Route.Program(5), Routes.parse("program/5"))
        assertEquals(Route.Announcements, Routes.parse("announcements"))
        assertEquals(Route.Checkin, Routes.parse("checkin"))
        assertEquals(Route.Profile, Routes.parse("profile"))
        assertEquals(Route.ProgramRequests, Routes.parse("program-requests"))
        assertEquals(Route.Home, Routes.parse("home"))
    }

    @Test fun unknownOrBrokenRoutesFallBackToHome() {
        assertEquals(Route.Home, Routes.parse(null))
        assertEquals(Route.Home, Routes.parse(""))
        assertEquals(Route.Home, Routes.parse("injury/abc"))
        assertEquals(Route.Home, Routes.parse("something/else"))
    }

    @Test fun channelMappingMatchesServer() {
        // مقادیر مطابق _fcm_channel در varzesh511.py
        assertEquals("injury", Routes.channelFor("injury_reply"))
        assertEquals("injury", Routes.channelFor("injury_unanswered"))
        assertEquals("program", Routes.channelFor("program_updated"))
        assertEquals("program", Routes.channelFor("diet_updated"))
        assertEquals("announcements", Routes.channelFor("announcement"))
        assertEquals("reminders", Routes.channelFor("workout_reminder"))
        assertEquals("reminders", Routes.channelFor("checkin_reminder"))
        assertEquals("reminders", Routes.channelFor("sub_expiry"))
        assertEquals("general", Routes.channelFor("whatever"))
    }

    private fun ms(h: Int, m: Int) = (h * 60L + m) * 60_000L

    @Test fun reminderLaterTodayOrTomorrow() {
        assertEquals(ms(1, 0), ReminderTime.nextDelayMillis(ms(16, 0), 17, 0))             // امروز، یک ساعت دیگر
        assertEquals(ms(23, 0), ReminderTime.nextDelayMillis(ms(18, 0), 17, 0))            // گذشته → فردا
        assertEquals(24L * 60 * 60_000L, ReminderTime.nextDelayMillis(ms(17, 0), 17, 0))   // دقیقاً همین لحظه → فردا
    }
}
