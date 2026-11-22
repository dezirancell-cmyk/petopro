package ir.petropro.app

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ir.petropro.app.database.AppDatabase
import ir.petropro.app.database.Migrations
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** نیازمند فایل‌های app/schemas/*.json است که با اولین Build ساخته می‌شوند (و باید در گیت بمانند). */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java, emptyList(), FrameworkSQLiteOpenHelperFactory()
    )

    @Test fun migrate1To2KeepsOutboxAndCreatesNotifications() {
        helper.createDatabase("migration-test", 1).apply {
            execSQL(
                "INSERT INTO outbox (idemKey, type, path, bodyJson, createdAt, state, attempts, nextAttemptAt, refKey) " +
                    "VALUES ('key-migrate-1', 'SET_LOG', 'api/mobile/sets/log', '{}', 0, 'PENDING', 0, 0, 'r')"
            )
            close()
        }
        val db = helper.runMigrationsAndValidate("migration-test", 2, true, *Migrations.ALL)
        db.query("SELECT COUNT(*) FROM outbox").use { c ->
            c.moveToFirst()
            org.junit.Assert.assertEquals(1, c.getInt(0)) // ثبت‌های ارسال‌نشده‌ی کاربر باید حفظ شوند
        }
        db.query("SELECT COUNT(*) FROM notifications").use { c ->
            c.moveToFirst()
            org.junit.Assert.assertEquals(0, c.getInt(0))
        }
    }
}
