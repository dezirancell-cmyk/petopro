package ir.petropro.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import ir.petropro.app.BuildConfig
import ir.petropro.app.R
import ir.petropro.app.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels { ViewModelFactory(container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val store = container.sessionStore
        val reauth = intent.getBooleanExtra(EXTRA_REAUTH, false)
        binding.textReauthHint.visibility = if (reauth) View.VISIBLE else View.GONE
        if (savedInstanceState == null) {
            binding.inputServer.setText(store.serverUrl ?: BuildConfig.DEFAULT_SERVER_URL)
            if (reauth) binding.inputUsername.setText(store.username.orEmpty())
        }

        binding.buttonLogin.setOnClickListener {
            viewModel.login(
                binding.inputServer.text?.toString().orEmpty(),
                binding.inputUsername.text?.toString().orEmpty(),
                binding.inputPassword.text?.toString().orEmpty(),
            )
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun render(state: LoginUiState) {
        binding.progress.visibility = if (state is LoginUiState.Loading) View.VISIBLE else View.GONE
        binding.buttonLogin.isEnabled = state !is LoginUiState.Loading
        binding.textError.visibility = View.GONE
        when (state) {
            is LoginUiState.Error -> {
                binding.textError.text = state.message
                binding.textError.visibility = View.VISIBLE
            }
            is LoginUiState.Success -> {
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                finish()
            }
            is LoginUiState.ConfirmDiscard -> {
                viewModel.consumeState()
                AlertDialog.Builder(this)
                    .setTitle(R.string.login_other_user_title)
                    .setMessage(getString(R.string.login_other_user_message, state.count))
                    .setPositiveButton(R.string.login_discard_and_continue) { _, _ ->
                        viewModel.discardAndLogin(
                            binding.inputServer.text?.toString().orEmpty(),
                            binding.inputUsername.text?.toString().orEmpty(),
                            binding.inputPassword.text?.toString().orEmpty(),
                        )
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
            else -> Unit
        }
    }

    companion object {
        const val EXTRA_REAUTH = "reauth"
    }
}
