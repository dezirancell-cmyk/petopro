package ir.petropro.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import ir.petropro.app.R
import ir.petropro.app.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = container.sessionStore
        if (!store.isLoggedIn) {
            // اگر داده‌ای از قبل روی گوشی هست (توکن گم شده)، ورود مجدد با حفظ داده‌ها
            startActivity(Intent(this, LoginActivity::class.java).putExtra(LoginActivity.EXTRA_REAUTH, store.ownerUserId != -1))
            finish()
            return
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_program -> ProgramFragment()
                R.id.nav_checkin -> CheckinFragment()
                else -> SettingsFragment()
            }
            supportFragmentManager.commit { replace(R.id.fragmentContainer, fragment) }
            true
        }
        if (savedInstanceState == null) binding.bottomNav.selectedItemId = R.id.nav_program

        binding.bannerText.setOnClickListener {
            if (store.sessionExpired.value) openReauth() else lifecycleScope.launch { container.syncManager.syncAll() }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(store.sessionExpired, container.database.outboxDao().observeUnsentCount()) { expired, pending ->
                    expired to pending
                }.collect { (expired, pending) ->
                    when {
                        expired -> {
                            binding.bannerText.setBackgroundResource(R.color.banner_error)
                            binding.bannerText.setText(R.string.banner_session_expired)
                            binding.bannerText.visibility = View.VISIBLE
                        }
                        pending > 0 -> {
                            binding.bannerText.setBackgroundResource(R.color.banner_warning)
                            binding.bannerText.text = getString(R.string.banner_pending, pending)
                            binding.bannerText.visibility = View.VISIBLE
                        }
                        else -> binding.bannerText.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // با هر ورود به برنامه یک همگام‌سازی؛ بدون اینترنت بی‌صدا شکست می‌خورد و داده‌ی محلی در دسترس می‌ماند
        if (container.sessionStore.isLoggedIn) {
            lifecycleScope.launch { container.syncManager.syncAll() }
        }
    }

    private fun openReauth() {
        startActivity(Intent(this, LoginActivity::class.java).putExtra(LoginActivity.EXTRA_REAUTH, true))
    }
}
