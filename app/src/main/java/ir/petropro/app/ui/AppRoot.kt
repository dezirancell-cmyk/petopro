package ir.petropro.app.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.petropro.app.core.AppContainer
import ir.petropro.app.domain.RoleTabs
import ir.petropro.app.ui.components.Banner
import ir.petropro.app.ui.components.TablerIcon
import ir.petropro.app.ui.screens.CheckinScreen
import ir.petropro.app.ui.screens.DashboardScreen
import ir.petropro.app.ui.screens.InjuryDetailScreen
import ir.petropro.app.ui.screens.InjuryListScreen
import ir.petropro.app.ui.screens.MyProgramScreen
import ir.petropro.app.ui.screens.NotificationsScreen
import ir.petropro.app.ui.screens.SettingsScreen
import ir.petropro.app.ui.screens.WorkoutScreen
import ir.petropro.app.ui.theme.PetroColors
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
fun AppRoot(container: AppContainer, routeFlow: StateFlow<String?>, onRouteConsumed: () -> Unit) {
    val context = LocalContext.current
    val store = container.sessionStore
    val role = store.role
    val navigator = remember { Navigator("dashboard") }
    val factory = remember { ViewModelFactory(container) }
    val scope = rememberCoroutineScope()

    val route by routeFlow.collectAsState()
    LaunchedEffect(route) {
        route?.let {
            navigator.openRoute(it, role)
            onRouteConsumed()
        }
    }
    BackHandler(enabled = navigator.canPop) { navigator.pop() }

    val unread by container.notificationRepository.observeUnreadCount().collectAsStateWithLifecycle(initialValue = 0)
    val pending by container.database.outboxDao().observeUnsentCount().collectAsStateWithLifecycle(initialValue = 0)
    val expired by store.sessionExpired.collectAsStateWithLifecycle()
    val syncing by container.syncManager.running.collectAsStateWithLifecycle()
    val online by container.networkMonitor.isOnline.collectAsStateWithLifecycle()

    val screen = navigator.current
    Column(Modifier.fillMaxSize().background(PetroColors.Bg).statusBarsPadding()) {
        TopBar(
            title = titleOf(screen, role),
            canBack = navigator.canPop,
            onBack = { navigator.pop() },
            syncing = syncing,
            online = online,
            unread = unread,
            onSync = { scope.launch { container.syncManager.syncAll() } },
            onBell = { navigator.push(Screen.Notifications) },
            onSettings = { navigator.push(Screen.Settings) },
        )
        when {
            expired -> Banner(
                "نشست منقضی شده است — برای ورود مجدد لمس کنید (اطلاعات ثبت‌شده‌ی آفلاین حفظ می‌شود)", PetroColors.Red,
                onClick = {
                    context.startActivity(Intent(context, LoginActivity::class.java).putExtra(LoginActivity.EXTRA_REAUTH, true))
                },
            )
            pending > 0 -> Banner(
                "$pending مورد در انتظار ارسال به سرور" + if (online) " — برای ارسال لمس کنید" else " — پس از اتصال اینترنت ارسال می‌شود",
                PetroColors.Yellow,
                onClick = { scope.launch { container.syncManager.syncAll() } },
            )
            !online -> Banner("آفلاین هستید؛ اطلاعات ذخیره‌شده‌ی گوشی در دسترس است", PetroColors.Brand2)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (screen) {
                is Screen.Tab -> when (screen.key) {
                    "dashboard" -> DashboardScreen(container, role, navigator, online, syncing, pending, unread)
                    "myprogram" -> MyProgramScreen(factory, navigator)
                    "readiness" -> CheckinScreen(factory)
                    "injuries" -> InjuryListScreen(container, role) { navigator.push(Screen.InjuryDetail(it)) }
                    else -> DashboardScreen(container, role, navigator, online, syncing, pending, unread)
                }
                is Screen.Workout -> WorkoutScreen(container, screen.dayId)
                is Screen.InjuryList -> InjuryListScreen(container, role) { navigator.push(Screen.InjuryDetail(it)) }
                is Screen.InjuryDetail -> InjuryDetailScreen(container, screen.reportId)
                is Screen.Notifications -> NotificationsScreen(factory) { navigator.openRoute(it, role) }
                is Screen.Settings -> SettingsScreen(container, factory)
            }
            if (screen is Screen.Tab) {
                FloatingTabBar(
                    tabs = RoleTabs.core(role),
                    selected = screen.key,
                    onSelect = { navigator.selectTab(it) },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

private fun titleOf(screen: Screen, role: String?): String = when (screen) {
    is Screen.Tab -> RoleTabs.find(role, screen.key)?.label ?: if (screen.key == "injuries") "آسیب‌دیدگی" else "پترو پرو"
    is Screen.Workout -> "تمرین"
    is Screen.InjuryList -> "آسیب‌دیدگی"
    is Screen.InjuryDetail -> "گفتگوی گزارش"
    is Screen.Notifications -> "اعلان‌ها"
    is Screen.Settings -> "تنظیمات برنامه"
}

/** معادل .topbar وب */
@Composable
private fun TopBar(
    title: String, canBack: Boolean, onBack: () -> Unit, syncing: Boolean, online: Boolean,
    unread: Int, onSync: () -> Unit, onBell: () -> Unit, onSettings: () -> Unit,
) {
    Surface(color = Color(0xD9141A2A), border = BorderStroke(1.dp, PetroColors.Border)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (canBack) {
                IconAction(onClick = onBack) { TablerIcon("arrow-right", fallback = "›") }
            }
            Text(title, color = PetroColors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(horizontal = 6.dp))
            IconAction(onClick = onSync) {
                when {
                    syncing -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = PetroColors.Brand)
                    !online -> TablerIcon("wifi-off", tint = PetroColors.TextMute, fallback = "⌀")
                    else -> TablerIcon("refresh", fallback = "⟳")
                }
            }
            IconAction(onClick = onBell) {
                Box {
                    TablerIcon("bell", fallback = "🔔")
                    if (unread > 0) {
                        Box(Modifier.align(Alignment.TopEnd).size(10.dp).clip(CircleShape).background(PetroColors.Red))
                    }
                }
            }
            IconAction(onClick = onSettings) { TablerIcon("settings", fallback = "⚙") }
        }
    }
}

@Composable
private fun IconAction(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) { content() }
}

/** معادل .tabbar وب: منوی شناور پایین با گوشه‌ی ۲۰ و حاشیه‌ی border */
@Composable
private fun FloatingTabBar(
    tabs: List<ir.petropro.app.domain.TabDef>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier,
) {
    if (tabs.isEmpty()) return
    Surface(
        modifier = modifier.navigationBarsPadding().padding(bottom = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = PetroColors.TabBar,
        border = BorderStroke(1.dp, PetroColors.Border),
        shadowElevation = 12.dp,
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (t in tabs) {
                val active = t.key == selected
                val tint = if (active) PetroColors.Brand else PetroColors.TextDim
                Column(
                    Modifier.clip(RoundedCornerShape(12.dp))
                        .background(if (active) PetroColors.Brand.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable { onSelect(t.key) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TablerIcon(t.icon, tint = tint, size = 22.dp)
                    Text(t.label, color = tint, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}
