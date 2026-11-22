package ir.petropro.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ir.petropro.app.PetroProApp
import ir.petropro.app.R
import ir.petropro.app.core.SessionStore
import ir.petropro.app.core.TimeUtils
import ir.petropro.app.domain.ReminderTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * یادآورهای روزانه‌ی «محلی»: بدون اینترنت و بدون سرور کار می‌کنند. هر اجرا، نوبت بعدی را برای فردا می‌گذارد
 * (WorkManager بعد از ریبوت هم نگه می‌دارد). زمان در WorkManager تقریبی است (ممکن است چند دقیقه جابه‌جا شود).
 */
class ReminderScheduler(private val context: Context, private val store: SessionStore) {

    fun schedule(kind: String) {
        val wm = WorkManager.getInstance(context)
        if (!store.reminderEnabled(kind) || !store.isLoggedIn) {
            wm.cancelUniqueWork(workName(kind))
            return
        }
        val nowOfDay = LocalTime.now().toNanoOfDay() / 1_000_000L
        val delay = ReminderTime.nextDelayMillis(nowOfDay, store.reminderHour(kind), store.reminderMinute(kind))
        wm.enqueueUniqueWork(
            workName(kind), ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(Data.Builder().putString(KEY_KIND, kind).build())
                .build()
        )
    }

    fun rescheduleAll() {
        schedule(SessionStore.KIND_WORKOUT)
        schedule(SessionStore.KIND_CHECKIN)
    }

    companion object {
        const val KEY_KIND = "kind"
        fun workName(kind: String) = "reminder-$kind"
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val kind = inputData.getString(ReminderScheduler.KEY_KIND) ?: return Result.success()
        val container = (applicationContext as PetroProApp).container
        val store = container.sessionStore
        if (!store.isLoggedIn || !store.reminderEnabled(kind)) return Result.success()

        val today = TimeUtils.todayTehran()
        val shouldNotify = when (kind) {
            SessionStore.KIND_WORKOUT -> container.database.setLogDao().countDone(today) == 0
            SessionStore.KIND_CHECKIN -> container.database.checkinDao().get(today) == null
            else -> false
        }
        if (shouldNotify) {
            val workout = kind == SessionStore.KIND_WORKOUT
            NotificationHelper.show(
                applicationContext,
                notificationId = if (workout) 9001 else 9002,
                kind = if (workout) "workout_reminder" else "checkin_reminder",
                title = applicationContext.getString(if (workout) R.string.reminder_workout_title else R.string.reminder_checkin_title),
                body = applicationContext.getString(if (workout) R.string.reminder_workout_body else R.string.reminder_checkin_body),
                route = if (workout) "home" else "checkin",
                serverNid = null,
            )
        }
        container.reminders.schedule(kind) // نوبت فردا
        return Result.success()
    }
}
