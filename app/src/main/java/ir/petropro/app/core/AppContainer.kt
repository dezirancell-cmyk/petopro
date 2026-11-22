package ir.petropro.app.core

import android.content.Context
import ir.petropro.app.data.AuthRepository
import ir.petropro.app.data.CheckinRepository
import ir.petropro.app.data.InjuryRepository
import ir.petropro.app.data.NotificationRepository
import ir.petropro.app.notifications.PushManager
import ir.petropro.app.notifications.ReminderScheduler
import ir.petropro.app.data.ProgramRepository
import ir.petropro.app.data.WorkoutRepository
import ir.petropro.app.database.AppDatabase
import ir.petropro.app.media.MediaCacheManager
import ir.petropro.app.network.NetworkModule
import ir.petropro.app.sync.OutboxProcessor
import ir.petropro.app.sync.SyncManager
import ir.petropro.app.sync.SyncScheduler

/** تزریق وابستگی دستی (ساده و قابل‌فهم؛ بدون کتابخانه‌ی اضافه) */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val sessionStore = SessionStore(appContext)
    val database: AppDatabase = AppDatabase.build(appContext)
    val network = NetworkModule(sessionStore)
    val scheduler = SyncScheduler(appContext)
    val networkMonitor = NetworkMonitor(appContext)

    val mediaCache = MediaCacheManager(appContext, database, network.api, sessionStore, network.mediaClient)
    val programRepository = ProgramRepository(network.api, database)
    val workoutRepository = WorkoutRepository(network.api, database, network.json)
    val checkinRepository = CheckinRepository(database, network.json)
    val authRepository = AuthRepository(network.api, sessionStore, database, mediaCache)
    val outboxProcessor = OutboxProcessor(network.api, database, network.json)
    val notificationRepository = NotificationRepository(network.api, database, network.json)
    val injuryRepository = InjuryRepository(network.api)
    val pushManager = PushManager(appContext, sessionStore, network.api)
    val reminders = ReminderScheduler(appContext, sessionStore)
    val syncManager = SyncManager(sessionStore, outboxProcessor, programRepository, workoutRepository, mediaCache, scheduler, notificationRepository)
}
