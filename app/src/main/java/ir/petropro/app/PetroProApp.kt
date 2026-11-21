package ir.petropro.app

import android.app.Application
import ir.petropro.app.core.AppContainer

class PetroProApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        if (container.sessionStore.isLoggedIn) {
            container.scheduler.schedulePeriodic()
            container.scheduler.kickOutbox() // ارسال ثبت‌های مانده از اجرای قبلی (مثلاً بسته‌شدن ناگهانی)
        }
    }
}
