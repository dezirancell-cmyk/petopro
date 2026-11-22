package ir.petropro.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.petropro.app.core.AppContainer
import ir.petropro.app.database.NotificationEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationsViewModel(private val c: AppContainer) : ViewModel() {
    val items: StateFlow<List<NotificationEntity>> = c.notificationRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun refresh(onDone: (Boolean) -> Unit) {
        viewModelScope.launch { onDone(c.notificationRepository.refresh()) }
    }

    fun markRead(id: Int) {
        viewModelScope.launch {
            c.notificationRepository.markRead(listOf(id))
            c.scheduler.kickOutbox()
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            c.notificationRepository.markAllRead()
            c.scheduler.kickOutbox()
        }
    }
}
