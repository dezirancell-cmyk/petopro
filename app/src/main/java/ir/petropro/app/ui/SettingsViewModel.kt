package ir.petropro.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.petropro.app.R
import ir.petropro.app.core.AppContainer
import ir.petropro.app.database.OutboxEntity
import ir.petropro.app.sync.SyncOutcome
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUi(
    val pending: Int = 0,
    val issues: List<OutboxEntity> = emptyList(),
    val mediaDone: Int = 0,
    val mediaTotal: Int = 0,
    val mediaBytes: Long = 0,
)

class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    val ui: StateFlow<SettingsUi> = combine(
        c.database.outboxDao().observeUnsentCount(),
        c.database.outboxDao().observeIssues(),
        c.mediaCache.observeDoneCount(),
        c.mediaCache.observeTotalCount(),
        c.mediaCache.observeDoneBytes(),
    ) { pending, issues, done, total, bytes -> SettingsUi(pending, issues, done, total, bytes) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUi())

    val syncing: StateFlow<Boolean> = c.syncManager.running

    private val _messages = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val messages: SharedFlow<Int> = _messages

    fun syncNow() {
        viewModelScope.launch {
            val msg = when (c.syncManager.syncAll()) {
                SyncOutcome.OK -> R.string.sync_result_ok
                SyncOutcome.OFFLINE, SyncOutcome.SERVER_ERROR -> R.string.sync_result_offline
                SyncOutcome.AUTH_REQUIRED -> R.string.sync_result_auth
            }
            _messages.emit(msg)
        }
    }

    fun clearMedia() {
        viewModelScope.launch {
            c.mediaCache.clearCache()
            _messages.emit(R.string.settings_clear_media_done)
        }
    }

    fun retryBlocked() {
        viewModelScope.launch {
            c.database.outboxDao().retryBlocked()
            c.scheduler.kickOutbox()
        }
    }

    fun dismissIssues() {
        viewModelScope.launch { c.database.outboxDao().dismissIssues() }
    }

    suspend fun unsentCount(): Int = c.authRepository.unsentCount()

    fun freeBytes(): Long = c.mediaCache.freeBytes()

    /** خروج کامل: توقف کارهای پس‌زمینه، لغو توکن روی سرور، حذف دیتابیس و فایل‌های رسانه */
    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            c.scheduler.cancelAll()
            c.authRepository.logoutAndWipe()
            onDone()
        }
    }

    val sessionStore get() = c.sessionStore
}
