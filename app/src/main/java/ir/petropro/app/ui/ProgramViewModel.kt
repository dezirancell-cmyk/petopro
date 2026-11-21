package ir.petropro.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.petropro.app.core.AppContainer
import ir.petropro.app.database.DayEntity
import ir.petropro.app.database.ProgramEntity
import ir.petropro.app.sync.SyncOutcome
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DayRow(val day: DayEntity, val itemCount: Int)
data class ProgramUi(val program: ProgramEntity?, val days: List<DayRow>)

@OptIn(ExperimentalCoroutinesApi::class)
class ProgramViewModel(private val c: AppContainer) : ViewModel() {
    val ui: StateFlow<ProgramUi> = c.programRepository.observePrograms()
        .flatMapLatest { programs ->
            val program = programs.firstOrNull { it.active }
            if (program == null) {
                flowOf(ProgramUi(null, emptyList()))
            } else {
                combine(
                    c.programRepository.observeDays(program.id),
                    c.programRepository.observeDayCounts(program.id),
                ) { days, counts ->
                    val byDay = counts.associate { it.dayId to it.itemCount }
                    ProgramUi(program, days.map { DayRow(it, byDay[it.id] ?: 0) })
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgramUi(null, emptyList()))

    fun refresh(onDone: (SyncOutcome) -> Unit) {
        viewModelScope.launch { onDone(c.syncManager.syncAll()) }
    }
}
