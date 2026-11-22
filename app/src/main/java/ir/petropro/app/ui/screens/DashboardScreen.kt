package ir.petropro.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import ir.petropro.app.core.AppContainer
import ir.petropro.app.core.Formatters
import ir.petropro.app.domain.RoleTabs
import ir.petropro.app.ui.Navigator
import ir.petropro.app.ui.Screen
import ir.petropro.app.ui.components.Pill
import ir.petropro.app.ui.components.PetroCard
import ir.petropro.app.ui.components.PrimaryButton
import ir.petropro.app.ui.components.SectionTitle
import ir.petropro.app.ui.components.TablerIcon
import ir.petropro.app.ui.theme.PetroColors
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

private data class Tile(val key: String, val label: String, val icon: String)

private fun roleLabel(role: String?) = when (role) {
    RoleTabs.ADMIN -> "مدیر"
    RoleTabs.COACH -> "مربی"
    RoleTabs.PRO_ATHLETE -> "ورزشکار پرو"
    else -> "ورزشکار"
}

@Composable
fun DashboardScreen(
    container: AppContainer, role: String?, navigator: Navigator,
    online: Boolean, syncing: Boolean, pending: Int, unread: Int,
) {
    val store = container.sessionStore
    val scope = rememberCoroutineScope()
    val tiles = RoleTabs.extras(role).map { Tile(it.key, it.label, it.icon) }.toMutableList()
    // ورزشکار در وب از داخل صفحه‌های دیگر گزارش آسیب‌دیدگی ثبت می‌کند؛ در اپ از اینجا در دسترس است
    if (RoleTabs.isAthleteRole(role) && tiles.none { it.key == "injuries" }) tiles += Tile("injuries", "گزارش آسیب‌دیدگی", "bandage")

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PetroCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("سلام، ${store.fullName ?: store.username.orEmpty()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PetroColors.Text)
                    Pill(roleLabel(role), PetroColors.Brand)
                }
            }
        }
        item {
            PetroCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("وضعیت اتصال", color = PetroColors.TextDim, fontSize = 13.sp)
                    Pill(if (online) "آنلاین" else "آفلاین", if (online) PetroColors.Green else PetroColors.Brand2)
                }
                val last = store.lastSyncMillis
                Text(
                    if (last == 0L) "هنوز همگام‌سازی نشده" else "آخرین همگام‌سازی موفق: ${Formatters.dateTime(last)}",
                    color = PetroColors.TextDim, fontSize = 13.sp,
                )
                Text("در انتظار ارسال: $pending مورد", color = PetroColors.TextDim, fontSize = 13.sp)
                PrimaryButton(
                    if (syncing) "در حال همگام‌سازی…" else "همگام‌سازی اکنون",
                    onClick = { scope.launch { container.syncManager.syncAll() } },
                    enabled = !syncing, modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            PetroCard(onClick = { navigator.selectTab("myprogram") }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TablerIcon("clipboard-list", tint = PetroColors.Brand, size = 28.dp)
                    Text("برنامه‌ی تمرینی من", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PetroColors.Text)
                }
                Text("مشاهده‌ی حرکات و ثبت ست‌ها (بدون اینترنت هم کار می‌کند)", color = PetroColors.TextDim, fontSize = 13.sp)
            }
        }
        if (unread > 0) {
            item {
                PetroCard(onClick = { navigator.push(Screen.Notifications) }) {
                    Text("$unread اعلان خوانده‌نشده دارید", color = PetroColors.Text, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (tiles.isNotEmpty()) {
            item { SectionTitle("بخش‌ها") }
            items(tiles.chunked(2)) { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (t in pair) {
                        PetroCard(Modifier.weight(1f), onClick = { navigator.selectTab(t.key) }, padding = 14) {
                            TablerIcon(t.icon, tint = PetroColors.Brand, size = 26.dp)
                            Text(t.label, color = PetroColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
