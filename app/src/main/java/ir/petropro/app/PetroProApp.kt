package ir.petropro.app

import android.app.Application
import ir.petropro.app.core.AppContainer
import ir.petropro.app.notifications.NotificationChannels

class PetroProApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationChannels.createAll(this)
        if (container.sessionStore.isLoggedIn) {
            container.scheduler.schedulePeriodic()
            container.scheduler.kickOutbox() // ارسال ثبت‌های مانده از اجرای قبلی (مثلاً بسته‌شدن ناگهانی)
            container.scheduler.registerDevice()
            container.reminders.rescheduleAll()
        }
    }
}
