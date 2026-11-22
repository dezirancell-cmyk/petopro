package ir.petropro.app

import ir.petropro.app.network.ApiErrors
import ir.petropro.app.network.ApiJson
import ir.petropro.app.network.CheckinResponse
import ir.petropro.app.network.InjuryListResponse
import ir.petropro.app.network.InjuryMessagesResponse
import ir.petropro.app.network.NotificationsResponse
import ir.petropro.app.network.OkResponse
import ir.petropro.app.network.LoginResponse
import ir.petropro.app.network.ManifestResponse
import ir.petropro.app.network.MediaManifestResponse
import ir.petropro.app.network.ProgramResponse
import ir.petropro.app.network.SetLogRequest
import ir.petropro.app.network.SetLogResponse
import ir.petropro.app.network.SetsTodayResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.junit.Test

/** نمونه‌ها (fixtures) با اجرای واقعیِ کد سرور نسخه ۵۱۱ ساخته شده‌اند؛ این تست‌ها قرارداد DTOها را با سرور تضمین می‌کنند. */
class ContractParsingTest {
    private val json = ApiJson.instance
    private fun fixture(name: String): String =
        javaClass.classLoader!!.getResource("fixtures/$name.json")!!.readText(Charsets.UTF_8)

    @Test fun login() {
        val r = json.decodeFromString(LoginResponse.serializer(), fixture("login_ok"))
        assertTrue(r.ok)
        assertEquals("FIXTURE_TOKEN", r.token)
        assertEquals("athlete", r.user?.role)
        assertNotNull(r.user?.id)
        val bad = ApiErrors.parse(fixture("login_bad"))
        assertFalse(bad!!.ok)
        assertNotNull(bad.error)
    }

    @Test fun program() {
        val p = json.decodeFromString(ProgramResponse.serializer(), fixture("program_get")).program!!
        assertEquals(2, p.days.size)
        val first = p.days[0].items[0]
        assertEquals("اسکوات", first.nameFa)
        assertEquals(3, first.sets)
        assertEquals("8-12", first.reps)
        assertEquals(60.0, first.weightKg!!, 0.0)
        assertEquals(listOf("/static/library_cache_media/animated/0001.gif"), first.images)
        assertTrue(p.days[0].items[1].isWarmup)
        assertEquals(45, p.days[1].items[0].setTimeSeconds)
    }

    @Test fun manifest() {
        val m = json.decodeFromString(ManifestResponse.serializer(), fixture("manifest"))
        assertEquals(1, m.programs.size)
        assertTrue(m.programs[0].hash.isNotEmpty())
        assertTrue(m.programs[0].active)
    }

    @Test fun mediaManifest() {
        val m = json.decodeFromString(MediaManifestResponse.serializer(), fixture("media_manifest"))
        val gif = m.media.first { it.url.endsWith(".gif") && it.local }
        assertEquals("gif", gif.kind)
        assertEquals(64, gif.sha256!!.length)
        assertNotNull(gif.size)
        assertFalse(m.media.first { !it.local }.url.startsWith("/static/"))
    }

    @Test fun setLogResponses() {
        val ok = json.decodeFromString(SetLogResponse.serializer(), fixture("set_log_ok"))
        assertTrue(ok.ok); assertEquals(true, ok.done); assertNull(ok.remappedItemId)
        val unchanged = json.decodeFromString(SetLogResponse.serializer(), fixture("set_log_unchanged"))
        assertEquals(true, unchanged.unchanged)
        val conflict = ApiErrors.parse(fixture("set_log_conflict"))!!
        assertEquals("item_removed", conflict.conflict)
        assertEquals("409", fixture("set_log_conflict_status").trim())
    }

    @Test fun checkinResponses() {
        val ok = json.decodeFromString(CheckinResponse.serializer(), fixture("checkin_ok"))
        assertEquals(true, ok.applied); assertNull(ok.conflict); assertNotNull(ok.readiness)
        val newer = json.decodeFromString(CheckinResponse.serializer(), fixture("checkin_server_newer"))
        assertEquals("server_newer", newer.conflict); assertEquals(false, newer.applied)
    }

    @Test fun setsToday() {
        val s = json.decodeFromString(SetsTodayResponse.serializer(), fixture("sets_today"))
        assertEquals(1, s.done.size)
        assertEquals(1, s.done[0].setNumber)
        assertEquals(10, s.done[0].repsDone)
    }

    @Test fun setLogRequestSerializationMatchesServerContract() {
        val req = SetLogRequest(itemId = 5, setNumber = 2, date = "2026-11-21", done = true, weightKg = 60.0, repsDone = 10,
            clientTs = "2026-11-21T10:00:00", programId = 1, dayIndex = 0, exerciseId = 3, orderIndex = 0)
        val text = json.encodeToString(SetLogRequest.serializer(), req)
        assertTrue(text.contains("\"item_id\":5"))
        assertTrue(text.contains("\"set_number\":2"))
        assertTrue(text.contains("\"done\":true"))
        assertTrue(text.contains("\"client_ts\":\"2026-11-21T10:00:00\""))
        assertFalse("null فیلدها نباید ارسال شوند", text.contains("\"rpe\""))
        // بازگشت‌پذیری (Outbox بدنه را به‌صورت متن ذخیره و بعداً دوباره می‌خواند)
        assertEquals(req, json.decodeFromString(SetLogRequest.serializer(), text))
    }

    @Test fun notificationsAndDevices() {
        val n = json.decodeFromString(NotificationsResponse.serializer(), fixture("notifications_list"))
        val first = n.notifications[0]
        assertEquals("injury_reply", first.kind)
        // data شامل عدد (report_id) و متن (route) است
        assertEquals("injury/1", (first.data["route"] as JsonPrimitive).contentOrNull)
        assertEquals(1, n.unreadCount)
        assertFalse(first.read)
        assertTrue(json.decodeFromString(OkResponse.serializer(), fixture("device_register")).ok)
        assertEquals(0, json.decodeFromString(NotificationsResponse.serializer(), "{\"ok\":true,\"unread_count\":0}").unreadCount)
    }

    @Test fun injury() {
        val list = json.decodeFromString(InjuryListResponse.serializer(), fixture("injury_list"))
        assertEquals(1, list.reports.size)
        assertEquals("اسکوات", list.reports[0].exerciseName)
        assertTrue(list.reports[0].unread)
        val conv = json.decodeFromString(InjuryMessagesResponse.serializer(), fixture("injury_messages"))
        assertEquals("coach", conv.messages.last().senderRole)
        assertNotNull(conv.report)
        assertEquals(1, json.decodeFromString(OkResponse.serializer(), fixture("injury_create")).id)
    }
}
