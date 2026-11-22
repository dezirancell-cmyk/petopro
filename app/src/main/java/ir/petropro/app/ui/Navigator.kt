package ir.petropro.app.ui

import androidx.compose.runtime.mutableStateListOf
import ir.petropro.app.domain.Route
import ir.petropro.app.domain.RoleTabs
import ir.petropro.app.domain.Routes

sealed class Screen {
    data class Tab(val key: String) : Screen()
    data class Workout(val dayId: Int) : Screen()
    data object InjuryList : Screen()
    data class InjuryDetail(val reportId: Int) : Screen()
    data object Notifications : Screen()
    data object Settings : Screen()
}

/** پشته‌ی ساده‌ی صفحات؛ تب‌ها هم‌سطح‌اند (انتخاب تب، پشته را به همان تب بازنشانی می‌کند). */
class Navigator(start: String) {
    val stack = mutableStateListOf<Screen>(Screen.Tab(start))
    val current: Screen get() = stack.last()
    val canPop: Boolean get() = stack.size > 1

    fun push(screen: Screen) {
        if (stack.last() != screen) stack.add(screen)
    }

    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun selectTab(key: String) {
        stack.clear()
        stack.add(Screen.Tab(key))
    }

    /** مقصد اعلان (route سرور) → صفحه‌ی مربوط */
    fun openRoute(raw: String?, role: String?) {
        when (val r = Routes.parse(raw)) {
            is Route.Home -> selectTab("dashboard")
            is Route.Program, is Route.ProgramRequests -> selectTab("myprogram")
            is Route.Checkin -> selectTab(if (RoleTabs.isAthleteRole(role)) "readiness" else "dashboard")
            is Route.Announcements -> push(Screen.Notifications)
            is Route.Profile -> push(Screen.Settings)
            is Route.Injury -> push(Screen.InjuryDetail(r.reportId))
        }
    }
}
