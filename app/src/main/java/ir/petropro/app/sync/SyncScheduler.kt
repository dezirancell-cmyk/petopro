package ir.petropro.app.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ir.petropro.app.PetroProApp
import ir.petropro.app.media.DownloadResult
import java.util.concurrent.TimeUnit

class SyncScheduler(private val context: Context) {
    private val wm: WorkManager get() = WorkManager.getInstance(context)
    private val needsNetwork = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** همگام‌سازی دوره‌ای در پس‌زمینه (حداقل بازه‌ی WorkManager: ۱۵ دقیقه) */
    fun schedulePeriodic() {
        wm.enqueueUniquePeriodicWork(
            "periodic-sync", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).setConstraints(needsNetwork).build()
        )
    }

    /** بعد از هر ثبت آفلاین صدا زده می‌شود؛ با اتصال اینترنت Outbox خودکار ارسال می‌شود */
    fun kickOutbox() {
        wm.enqueueUniqueWork(
            "outbox", ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<OutboxWorker>().setConstraints(needsNetwork)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
        )
    }

    fun syncSoon() {
        wm.enqueueUniqueWork(
            "sync-soon", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(needsNetwork)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
        )
    }

    fun enqueueMediaDownload() {
        wm.enqueueUniqueWork(
            "media-download", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<MediaDownloadWorker>().setConstraints(needsNetwork)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
        )
    }

    fun cancelAll() {
        wm.cancelAllWork()
    }
}

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as PetroProApp).container
        return when (container.syncManager.syncAll()) {
            SyncOutcome.OFFLINE, SyncOutcome.SERVER_ERROR -> Result.retry()
            else -> Result.success()
        }
    }
}

class OutboxWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as PetroProApp).container
        if (!container.sessionStore.isLoggedIn) return Result.success()
        return when (container.outboxProcessor.flush()) {
            FlushResult.NETWORK_ERROR, FlushResult.RETRY_LATER -> Result.retry()
            else -> Result.success()
        }
    }
}

class MediaDownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as PetroProApp).container
        if (!container.sessionStore.isLoggedIn) return Result.success()
        return when (container.mediaCache.downloadPending()) {
            DownloadResult.NETWORK_ERROR -> Result.retry()
            else -> Result.success()
        }
    }
}
