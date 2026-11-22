package ir.petropro.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import ir.petropro.app.R

/** شناسه‌ی کانال‌ها باید با سرور (_fcm_channel) یکی باشد؛ نگاشت نوع→کانال در Routes.channelFor است. */
object NotificationChannels {
    fun createAll(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        fun make(id: String, nameRes: Int, importance: Int) {
            nm.createNotificationChannel(NotificationChannel(id, context.getString(nameRes), importance))
        }
        make("injury", R.string.channel_injury, NotificationManager.IMPORTANCE_HIGH)
        make("announcements", R.string.channel_announcements, NotificationManager.IMPORTANCE_HIGH)
        make("program", R.string.channel_program, NotificationManager.IMPORTANCE_DEFAULT)
        make("reminders", R.string.channel_reminders, NotificationManager.IMPORTANCE_DEFAULT)
        make("general", R.string.channel_general, NotificationManager.IMPORTANCE_DEFAULT)
    }
}
