package ir.petropro.app.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import ir.petropro.app.PetroProApp
import ir.petropro.app.core.AppContainer
import ir.petropro.app.notifications.NotificationHelper
import ir.petropro.app.ui.theme.PetroTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val container: AppContainer get() = (application as PetroProApp).container
    private val route = MutableStateFlow<String?>(null)

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        container.scheduler.registerDevice()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = container.sessionStore
        if (!store.isLoggedIn) {
            // اگر داده‌ای از قبل روی گوشی هست (توکن گم شده)، ورود مجدد با حفظ داده‌ها
            startActivity(Intent(this, LoginActivity::class.java).putExtra(LoginActivity.EXTRA_REAUTH, store.ownerUserId != -1))
            finish()
            return
        }
        if (savedInstanceState == null) {
            handleIntent(intent)
            askNotificationPermissionOnce()
        }
        setContent {
            PetroTheme {
                AppRoot(container, route) { route.value = null }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (container.sessionStore.isLoggedIn) handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        // با هر ورود به برنامه یک همگام‌سازی؛ بدون اینترنت بی‌صدا شکست می‌خورد و داده‌ی محلی در دسترس می‌ماند
        if (container.sessionStore.isLoggedIn) lifecycleScope.launch { container.syncManager.syncAll() }
    }

    /** لمس اعلان (محلی، FCM در پس‌زمینه یا پیش‌زمینه) → extras شامل route و nid */
    private fun handleIntent(intent: Intent?) {
        val r = intent?.getStringExtra(NotificationHelper.EXTRA_ROUTE) ?: return
        val nid = intent.getStringExtra(NotificationHelper.EXTRA_NID)?.toIntOrNull()
        intent.removeExtra(NotificationHelper.EXTRA_ROUTE)
        intent.removeExtra(NotificationHelper.EXTRA_NID)
        if (nid != null) {
            lifecycleScope.launch {
                container.notificationRepository.markRead(listOf(nid))
                container.scheduler.kickOutbox()
            }
        }
        route.value = r
    }

    private fun askNotificationPermissionOnce() {
        val store = container.sessionStore
        if (Build.VERSION.SDK_INT >= 33 && !store.askedNotificationPermission && !NotificationHelper.canPost(this)) {
            store.askedNotificationPermission = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
