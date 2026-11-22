package ir.petropro.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProgramEntity::class, DayEntity::class, ItemEntity::class, SetLogEntity::class,
        CheckinEntity::class, OutboxEntity::class, MediaEntity::class, NotificationEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun programDao(): ProgramDao
    abstract fun setLogDao(): SetLogDao
    abstract fun checkinDao(): CheckinDao
    abstract fun outboxDao(): OutboxDao
    abstract fun mediaDao(): MediaDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        const val NAME = "petropro.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                .addMigrations(*Migrations.ALL)
                // عمداً fallbackToDestructiveMigration() استفاده نمی‌شود: ثبت‌های ارسال‌نشده‌ی Outbox
                // نباید به‌خاطر یک تغییر اسکیما پاک شوند. اگر Migration فراموش شود، اپ خطا می‌دهد (نه حذف داده).
                .build()
    }
}

/**
 * تاریخچه: نسخه‌ی ۱ = فاز ۲، نسخه‌ی ۲ = فاز ۳ (جدول notifications).
 * هنگام بالا بردن version: یک Migration(n, n+1) اینجا بنویسید، در ALL بگذارید، فایل app/schemas/<n+1>.json را
 * در گیت نگه دارید و تست آن را در androidTest (MigrationTest) اضافه کنید.
 */
object Migrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `notifications` (`id` INTEGER NOT NULL, `kind` TEXT NOT NULL, " +
                    "`title` TEXT NOT NULL, `body` TEXT, `route` TEXT, `createdAt` TEXT NOT NULL, " +
                    "`isRead` INTEGER NOT NULL, PRIMARY KEY(`id`))"
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
