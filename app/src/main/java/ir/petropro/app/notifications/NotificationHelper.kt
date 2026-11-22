package ir.petropro.app.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ir.petropro.app.R
import ir.petropro.app.domain.Routes
import ir.petropro.app.ui.MainActivity

object NotificationHelper {
    const val EXTRA_ROUTE = "route"
    const val EXTRA_NID = "nid"

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** اعلان محلی/دریافتی را نشان می‌دهد؛ لمس آن MainActivity را با مقصد (route) باز می‌کند. */
    @SuppressLint("MissingPermission") // با canPost() بررسی شده است
    fun show(context: Context, notificationId: Int, kind: String, title: String, body: String?, route: String?, serverNid: Int?) {
        if (!canPost(context)) return
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_ROUTE, route ?: "home")
        if (serverNid != null) intent.putExtra(EXTRA_NID, serverNid.toString())
        val pending = PendingIntent.getActivity(
            context, notificationId, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, Routes.channelFor(kind))
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
