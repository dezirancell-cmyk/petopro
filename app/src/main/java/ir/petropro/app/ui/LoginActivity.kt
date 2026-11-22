package ir.petropro.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.petropro.app.BuildConfig
import ir.petropro.app.PetroProApp
import ir.petropro.app.ui.components.Banner
import ir.petropro.app.ui.components.PrimaryButton
import ir.petropro.app.ui.components.petroFieldColors
import ir.petropro.app.ui.theme.PetroColors
import ir.petropro.app.ui.theme.PetroTheme

class LoginActivity : ComponentActivity() {
    private val viewModel: LoginViewModel by viewModels {
        ViewModelFactory((application as PetroProApp).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = (application as PetroProApp).container.sessionStore
        val reauth = intent.getBooleanExtra(EXTRA_REAUTH, false)
        setContent {
            PetroTheme {
                LoginScreen(
                    viewModel = viewModel,
                    reauth = reauth,
                    initialServer = store.serverUrl ?: BuildConfig.DEFAULT_SERVER_URL,
                    initialUser = if (reauth) store.username.orEmpty() else "",
                    onSuccess = {
                        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        const val EXTRA_REAUTH = "reauth"
    }
}

@Composable
private fun LoginScreen(
    viewModel: LoginViewModel, reauth: Boolean, initialServer: String, initialUser: String, onSuccess: () -> Unit,
) {
    // اگر آدرس سرور داخل برنامه (gradle.properties) تنظیم شده باشد، کادر آدرس نمایش داده نمی‌شود
    val serverLocked = BuildConfig.DEFAULT_SERVER_URL.isNotBlank()
    var server by remember { mutableStateOf(initialServer) }
    var username by remember { mutableStateOf(initialUser) }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var discardCount by remember { mutableIntStateOf(0) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val serverValue = if (serverLocked) BuildConfig.DEFAULT_SERVER_URL else server

    LaunchedEffect(state) {
        when (val s = state) {
            is LoginUiState.Success -> onSuccess()
            is LoginUiState.ConfirmDiscard -> {
                discardCount = s.count
                viewModel.consumeState()
            }
            else -> Unit
        }
    }

    Column(
        Modifier.fillMaxSize().background(PetroColors.Bg).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("ورود به پترو پرو", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = PetroColors.Brand, modifier = Modifier.padding(top = 48.dp, bottom = 8.dp))
        if (reauth) {
            Banner("نشست شما منقضی شده است. برای ادامه دوباره وارد شوید؛ اطلاعات ثبت‌شده‌ی آفلاین حفظ می‌شود.", PetroColors.Yellow)
        }
        if (!serverLocked) {
            OutlinedTextField(
                server, { server = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = petroFieldColors(),
                label = { Text("آدرس سرور (مثال: https://example.com)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
        }
        OutlinedTextField(
            username, { username = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = petroFieldColors(),
            label = { Text("نام کاربری (شماره موبایل)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )
        OutlinedTextField(
            password, { password = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = petroFieldColors(),
            label = { Text("رمز عبور") },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = { TextButton(onClick = { showPassword = !showPassword }) { Text(if (showPassword) "پنهان" else "نمایش", color = PetroColors.TextDim, fontSize = 12.sp) } },
        )
        (state as? LoginUiState.Error)?.let { Text(it.message, color = PetroColors.Red, fontSize = 13.sp) }
        if (state is LoginUiState.Loading) {
            CircularProgressIndicator(color = PetroColors.Brand)
        }
        PrimaryButton(
            "ورود", modifier = Modifier.fillMaxWidth(), enabled = state !is LoginUiState.Loading,
            onClick = { viewModel.login(serverValue, username, password) },
        )
    }

    if (discardCount > 0) {
        AlertDialog(
            onDismissRequest = { discardCount = 0 },
            containerColor = PetroColors.Panel,
            title = { Text("اطلاعات ارسال‌نشده‌ی کاربر دیگر", color = PetroColors.Text) },
            text = {
                Text(
                    "روی این گوشی $discardCount مورد ثبت‌شده‌ی ارسال‌نشده از کاربر قبلی وجود دارد. اگر با این حساب وارد شوید، این موارد برای همیشه حذف می‌شوند. ابتدا با حساب قبلی وارد شوید و همگام‌سازی کنید.",
                    color = PetroColors.TextDim,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    discardCount = 0
                    viewModel.discardAndLogin(serverValue, username, password)
                }) { Text("حذف و ورود", color = PetroColors.Red) }
            },
            dismissButton = { TextButton(onClick = { discardCount = 0 }) { Text("انصراف", color = PetroColors.TextDim) } },
        )
    }
}
