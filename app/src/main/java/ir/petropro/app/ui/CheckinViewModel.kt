package ir.petropro.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.petropro.app.core.AppContainer
import ir.petropro.app.database.CheckinEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CheckinViewModel(private val c: AppContainer) : ViewModel() {
    val today: StateFlow<CheckinEntity?> = c.checkinRepository.observeToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(entity: CheckinEntity) {
        viewModelScope.launch {
            c.checkinRepository.save(entity)
            c.scheduler.kickOutbox()
        }
    }
}
