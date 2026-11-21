package ir.petropro.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        ProgramEntity::class, DayEntity::class, ItemEntity::class, SetLogEntity::class,
        CheckinEntity::class, OutboxEntity::class, MediaEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun programDao(): ProgramDao
    abstract fun setLogDao(): SetLogDao
    abstract fun checkinDao(): CheckinDao
    abstract fun outboxDao(): OutboxDao
    abstract fun mediaDao(): MediaDao

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
 * نسخه‌ی ۱ اولین نسخه است و Migration ندارد. هنگام بالا بردن version:
 *  ۱) version را زیاد کنید، ۲) یک Migration(n, n+1) اینجا اضافه کنید، ۳) در ALL بگذارید،
 *  ۴) فایل app/schemas/<n+1>.json را در گیت نگه دارید و تست Migration را در androidTest بنویسید.
 */
object Migrations {
    val ALL: Array<Migration> = emptyArray()
}
