package ir.petropro.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ir.petropro.app.core.AppContainer

class ViewModelFactory(private val container: AppContainer, private val dayId: Int = 0) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val vm: ViewModel = when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> LoginViewModel(container)
            modelClass.isAssignableFrom(ProgramViewModel::class.java) -> ProgramViewModel(container)
            modelClass.isAssignableFrom(WorkoutViewModel::class.java) -> WorkoutViewModel(container, dayId)
            modelClass.isAssignableFrom(CheckinViewModel::class.java) -> CheckinViewModel(container)
            modelClass.isAssignableFrom(NotificationsViewModel::class.java) -> NotificationsViewModel(container)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(container)
            else -> throw IllegalArgumentException("ViewModel ناشناخته: ${modelClass.name}")
        }
        return vm as T
    }
}
