package ir.petropro.app.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import ir.petropro.app.PetroProApp
import ir.petropro.app.core.AppContainer

abstract class BaseActivity : AppCompatActivity() {
    val container: AppContainer get() = (application as PetroProApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // رابط کاربری فارسی است؛ جهت راست‌به‌چپ را مستقل از زبان گوشی اجباری می‌کنیم
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
    }
}
