package ir.petropro.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.petropro.app.core.AppContainer
import ir.petropro.app.data.LoginResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    data object Idle : LoginUiState()
    data object Loading : LoginUiState()
    data class Error(val message: String) : LoginUiState()
    data object Success : LoginUiState()
    data class ConfirmDiscard(val count: Int) : LoginUiState()
}

class LoginViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun login(server: String, username: String, password: String) {
        if (server.isBlank() || username.isBlank() || password.isEmpty()) {
            _state.value = LoginUiState.Error("همه‌ی فیلدها را پر کنید")
            return
        }
        viewModelScope.launch {
            _state.value = LoginUiState.Loading
            _state.value = when (val r = c.authRepository.login(server, username, password)) {
                is LoginResult.Success -> {
                    c.scheduler.schedulePeriodic()
                    c.scheduler.kickOutbox()
                    LoginUiState.Success
                }
                is LoginResult.Failure -> LoginUiState.Error(r.message)
                is LoginResult.PendingOtherUser -> LoginUiState.ConfirmDiscard(r.count)
            }
        }
    }

    /** با تأیید صریح کاربر: داده‌های ارسال‌نشده‌ی کاربر قبلی حذف و ورود تکرار می‌شود */
    fun discardAndLogin(server: String, username: String, password: String) {
        viewModelScope.launch {
            c.authRepository.wipeLocalData()
            login(server, username, password)
        }
    }

    fun consumeState() {
        if (_state.value !is LoginUiState.Loading) _state.value = LoginUiState.Idle
    }
}
