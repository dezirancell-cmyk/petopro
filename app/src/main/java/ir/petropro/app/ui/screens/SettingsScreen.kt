package ir.petropro.app.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.petropro.app.core.AppContainer
import ir.petropro.app.core.Formatters
import ir.petropro.app.core.SessionStore
import ir.petropro.app.database.OutboxState
import ir.petropro.app.database.OutboxType
import ir.petropro.app.notifications.NotificationHelper
import ir.petropro.app.ui.LoginActivity
import ir.petropro.app.ui.SettingsViewModel
import ir.petropro.app.ui.ViewModelFactory
import ir.petropro.app.ui.components.PetroCard
import ir.petropro.app.ui.components.PrimaryButton
import ir.petropro.app.ui.components.SecondaryButton
import ir.petropro.app.ui.components.SectionTitle
import ir.petropro.app.ui.theme.PetroColors
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SettingsScreen(container: AppContainer, factory: ViewModelFactory) {
    val context = LocalContext.current
    val vm: SettingsViewModel = viewModel(factory = factory)
    val ui by vm.ui.collectAsStateWithLifecycle()
    val syncing by vm.syncing.collectAsStateWithLifecycle()
    val store = vm.sessionStore
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) }
    var showLogout by remember { mutableStateOf(false) }
    var unsent by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { vm.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() } }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.registerDevice()
        tick++
    }

    val canPost = remember(tick) { NotificationHelper.canPost(context) }
    val firebase = remember { vm.firebaseAvailable() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PetroCard {
                SectionTitle("حساب و همگام‌سازی")
                Text("سرور: ${store.serverUrl.orEmpty()}", color = PetroColors.TextDim, fontSize = 13.sp)
                Text("کاربر: ${store.fullName ?: store.username.orEmpty()} (${store.role.orEmpty()})", color = PetroColors.TextDim, fontSize = 13.sp)
                val last = store.lastSyncMillis
                Text(
                    if (last == 0L) "هنوز همگام‌سازی نشده" else "آخرین همگام‌سازی موفق: ${Formatters.dateTime(last)}",
                    color = PetroColors.TextDim, fontSize = 13.sp,
                )
                Text("در انتظار ارسال: ${ui.pending} مورد", color = PetroColors.TextDim, fontSize = 13.sp)
                Text(
                    "رسانه‌های ذخیره‌شده: ${ui.mediaDone} از ${ui.mediaTotal} (${Formatters.bytes(ui.mediaBytes)})",
                    color = PetroColors.TextDim, fontSize = 13.sp,
                )
                Text("فضای آزاد گوشی: ${Formatters.bytes(vm.freeBytes())}", color = PetroColors.TextDim, fontSize = 13.sp)
                PrimaryButton(if (syncing) "در حال همگام‌سازی…" else "همگام‌سازی اکنون", onClick = { vm.syncNow() }, enabled = !syncing, modifier = Modifier.fillMaxWidth())
                SecondaryButton("پاک‌سازی رسانه‌ها", onClick = { vm.clearMedia() }, modifier = Modifier.fillMaxWidth())
            }
        }
        item {
            PetroCard {
                SectionTitle("مشکلات همگام‌سازی")
                if (ui.issues.isEmpty()) Text("مشکلی وجود ندارد", color = PetroColors.TextDim, fontSize = 13.sp)
                for (op in ui.issues) {
                    val what = if (op.type == OutboxType.SET_LOG) "ثبت ست" else "ثبت چک‌این"
                    val detail = op.lastError.orEmpty()
                    val state = when (op.state) {
                        OutboxState.CONFLICT -> "تعارض: $detail"
                        OutboxState.BLOCKED -> "مسدود: $detail"
                        else -> "رد شد: $detail"
                    }
                    Text(what, color = PetroColors.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(state, color = PetroColors.TextDim, fontSize = 13.sp)
                }
                if (ui.issues.any { it.state == OutboxState.BLOCKED }) {
                    SecondaryButton("تلاش مجدد برای موارد مسدود", onClick = { vm.retryBlocked() }, modifier = Modifier.fillMaxWidth())
                }
                if (ui.issues.any { it.state != OutboxState.BLOCKED }) {
                    SecondaryButton("حذف همه‌ی موارد مشکل‌دار", onClick = { vm.dismissIssues() }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item {
            PetroCard {
                SectionTitle("اعلان‌ها و یادآورها")
                Text(
                    when {
                        !canPost -> "اجازه‌ی نمایش اعلان داده نشده است"
                        !firebase -> "Push در این نسخه تنظیم نشده است (فایل google-services.json گذاشته نشده)؛ اعلان‌ها هنگام باز کردن برنامه در صندوق اعلان‌ها دریافت می‌شوند."
                        else -> "Push فعال است"
                    },
                    color = PetroColors.TextDim, fontSize = 13.sp,
                )
                if (!canPost) {
                    PrimaryButton("فعال‌سازی اجازه‌ی اعلان", modifier = Modifier.fillMaxWidth(), onClick = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            )
                        }
                    })
                }
                SwitchRow("دریافت اعلان از سرور (Push)", remember(tick) { store.pushEnabled }, enabled = firebase) {
                    vm.setPush(it)
                    tick++
                }
                ReminderRow("یادآور روزانه‌ی تمرین", SessionStore.KIND_WORKOUT, store, vm, tick) { tick++ }
                ReminderRow("یادآور روزانه‌ی چک‌این", SessionStore.KIND_CHECKIN, store, vm, tick) { tick++ }
                Text(
                    "یادآورها روی خود گوشی ساخته می‌شوند و بدون اینترنت هم کار می‌کنند؛ اگر کار آن روز انجام شده باشد نمایش داده نمی‌شوند. زمان دقیق ممکن است چند دقیقه جابه‌جا شود.",
                    color = PetroColors.TextMute, fontSize = 12.sp,
                )
            }
        }
        item {
            SecondaryButton("خروج از حساب", modifier = Modifier.fillMaxWidth(), onClick = {
                scope.launch {
                    unsent = vm.unsentCount()
                    showLogout = true
                }
            })
        }
    }

    if (showLogout) {
        AlertDialog(
            onDismissRequest = { showLogout = false },
            containerColor = PetroColors.Panel,
            title = { Text("خروج از حساب", color = PetroColors.Text) },
            text = {
                Text(
                    if (unsent > 0) "$unsent مورد ثبت‌شده هنوز به سرور ارسال نشده و با خروج برای همیشه حذف می‌شود. بهتر است ابتدا همگام‌سازی کنید. ادامه می‌دهید؟"
                    else "با خروج، اطلاعات ذخیره‌شده‌ی این حساب از گوشی پاک می‌شود. ادامه می‌دهید؟",
                    color = PetroColors.TextDim,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLogout = false
                    vm.logout {
                        context.startActivity(Intent(context, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                    }
                }) { Text("خروج و حذف", color = PetroColors.Red) }
            },
            dismissButton = { TextButton(onClick = { showLogout = false }) { Text("انصراف", color = PetroColors.TextDim) } },
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = PetroColors.Text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked, onCheckedChange = onChange, enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = PetroColors.Brand, checkedThumbColor = PetroColors.Text),
        )
    }
}

@Composable
private fun ReminderRow(title: String, kind: String, store: SessionStore, vm: SettingsViewModel, tick: Int, onChanged: () -> Unit) {
    val context = LocalContext.current
    val enabled = remember(tick) { store.reminderEnabled(kind) }
    val hour = remember(tick) { store.reminderHour(kind) }
    val minute = remember(tick) { store.reminderMinute(kind) }
    SwitchRow(title, enabled) {
        vm.setReminder(kind, it, store.reminderHour(kind), store.reminderMinute(kind))
        onChanged()
    }
    SecondaryButton(
        "ساعت: " + String.format(Locale.US, "%02d:%02d", hour, minute),
        enabled = enabled,
        onClick = {
            TimePickerDialog(context, { _, h, m ->
                vm.setReminder(kind, store.reminderEnabled(kind), h, m)
                onChanged()
            }, hour, minute, true).show()
        },
    )
}
