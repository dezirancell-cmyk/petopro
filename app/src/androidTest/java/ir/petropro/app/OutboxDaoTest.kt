package ir.petropro.app

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ir.petropro.app.database.AppDatabase
import ir.petropro.app.database.OutboxEntity
import ir.petropro.app.database.OutboxState
import ir.petropro.app.database.OutboxType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OutboxDaoTest {
    private lateinit var db: AppDatabase

    @Before fun setUp() {
        val ctx: Context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).build()
    }

    @After fun tearDown() = db.close()

    private fun op(key: String, ref: String = "r") =
        OutboxEntity(idemKey = key, type = OutboxType.SET_LOG, path = "api/mobile/sets/log", bodyJson = "{}", createdAt = 0, refKey = ref)

    @Test fun operationsAreSentInInsertionOrder() = runBlocking {
        db.outboxDao().insert(op("key-aaaaaaaa"))
        db.outboxDao().insert(op("key-bbbbbbbb"))
        assertEquals("key-aaaaaaaa", db.outboxDao().firstPending()!!.idemKey)
        db.outboxDao().delete(db.outboxDao().firstPending()!!.seq)
        assertEquals("key-bbbbbbbb", db.outboxDao().firstPending()!!.idemKey)
    }

    @Test(expected = SQLiteConstraintException::class)
    fun duplicateIdempotencyKeyIsRejected() = runBlocking<Unit> {
        db.outboxDao().insert(op("same-key-0001"))
        db.outboxDao().insert(op("same-key-0001"))
    }

    @Test fun blockedAndConflictAreNotPendingButBlockedCountsAsUnsent() = runBlocking {
        db.outboxDao().insert(op("key-11111111"))
        db.outboxDao().insert(op("key-22222222"))
        db.outboxDao().update(db.outboxDao().firstPending()!!.copy(state = OutboxState.BLOCKED))
        db.outboxDao().update(db.outboxDao().firstPending()!!.copy(state = OutboxState.CONFLICT))
        assertNull(db.outboxDao().firstPending())
        assertEquals(1, db.outboxDao().unsentCount())   // فقط BLOCKED ارسال‌نشده حساب می‌شود
        db.outboxDao().retryBlocked()
        assertEquals(1, db.outboxDao().pendingCount())
        db.outboxDao().dismissIssues()
        assertEquals(1, db.outboxDao().unsentCount())
    }

    @Test fun laterPendingCountSupportsCollapsingLogic() = runBlocking {
        val first = db.outboxDao().insert(op("key-33333333", "set:1:1:d"))
        db.outboxDao().insert(op("key-44444444", "set:1:1:d"))
        assertEquals(1, db.outboxDao().laterPendingCount("set:1:1:d", first))
        assertEquals(0, db.outboxDao().laterPendingCount("other", first))
    }
}
