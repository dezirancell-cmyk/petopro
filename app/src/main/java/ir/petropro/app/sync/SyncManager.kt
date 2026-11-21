package ir.petropro.app.sync

import ir.petropro.app.core.SessionStore
import ir.petropro.app.core.TimeUtils
import ir.petropro.app.data.ProgramRepository
import ir.petropro.app.data.RefreshResult
import ir.petropro.app.data.WorkoutRepository
import ir.petropro.app.media.MediaCacheManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

enum class SyncOutcome { OK, OFFLINE, AUTH_REQUIRED, SERVER_ERROR }

class SyncManager(
    private val store: SessionStore,
    private val outbox: OutboxProcessor,
    private val programs: ProgramRepository,
    private val workout: WorkoutRepository,
    private val media: MediaCacheManager,
    private val scheduler: SyncScheduler,
) {
    private val mutex = Mutex()
    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    /**
     * همگام‌سازی کامل و دوطرفه، به این ترتیب: ۱) ارسال Outbox ۲) دریافت برنامه‌های تغییرکرده
     * ۳) هم‌ترازی وضعیت ست‌ها ۴) Manifest رسانه و شروع دانلود. هر مرحله‌ی ناموفق، دادهٔ محلی را خراب نمی‌کند.
     */
    suspend fun syncAll(): SyncOutcome = mutex.withLock {
        if (!store.isLoggedIn) return SyncOutcome.AUTH_REQUIRED
        _running.value = true
        try {
            when (outbox.flush()) {
                FlushResult.AUTH_REQUIRED -> return SyncOutcome.AUTH_REQUIRED
                FlushResult.NETWORK_ERROR -> return SyncOutcome.OFFLINE
                else -> Unit // DONE یا RETRY_LATER: ادامه می‌دهیم، دریافت برنامه‌ها به ارسال وابسته نیست
            }
            when (val r = programs.refresh()) {
                is RefreshResult.Http -> return if (r.code == 401) SyncOutcome.AUTH_REQUIRED else SyncOutcome.SERVER_ERROR
                is RefreshResult.Ok -> Unit
            }
            workout.pullServerState(listOf(TimeUtils.todayTehran(), TimeUtils.yesterdayTehran()))
            media.refreshManifest()
            store.lastSyncMillis = System.currentTimeMillis()
            scheduler.enqueueMediaDownload()
            SyncOutcome.OK
        } catch (e: IOException) {
            SyncOutcome.OFFLINE
        } finally {
            _running.value = false
        }
    }
}
