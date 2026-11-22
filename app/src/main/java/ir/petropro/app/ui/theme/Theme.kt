package ir.petropro.app.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.material3.LocalContentColor

/** رنگ‌ها و اندازه‌ها دقیقاً از :root در CSS وب (varzesh510.py) برداشته شده‌اند. */
object PetroColors {
    val Bg = Color(0xFF0F1420)
    val Bg2 = Color(0xFF161D2E)
    val Panel = Color(0xFF1B2436)
    val Panel2 = Color(0xFF212C42)
    val Border = Color(0xFF2A3550)
    val Text = Color(0xFFE7ECF7)
    val TextDim = Color(0xFF9AA7C2)
    val TextMute = Color(0xFF6C7A99)
    val Brand = Color(0xFF5B8CFF)
    val Brand2 = Color(0xFF7C5CFF)
    val Green = Color(0xFF37C98F)
    val Red = Color(0xFFFF5C72)
    val Yellow = Color(0xFFC6FF3A)
    val TabBar = Color(0xEB141A2A) // rgba(20,26,42,.92)
}

object PetroShapes {
    val Radius = 14.dp
    val RadiusSm = 9.dp
}

/**
 * فونت Vazirmatn (همان نسخه‌ی وب v33.003). فایل‌ها را workflow هنگام Build در assets/fonts می‌گذارد
 * (یا خودتان بگذارید). اگر نبود، فونت پیش‌فرض اندروید استفاده می‌شود و برنامه خراب نمی‌شود.
 */
fun loadVazirmatn(context: Context): FontFamily {
    val names = runCatching { context.assets.list("fonts")?.toSet() }.getOrNull().orEmpty()
    val fonts = mutableListOf<Font>()
    if ("Vazirmatn-Regular.ttf" in names) fonts += Font("fonts/Vazirmatn-Regular.ttf", context.assets, FontWeight.Normal)
    if ("Vazirmatn-Bold.ttf" in names) fonts += Font("fonts/Vazirmatn-Bold.ttf", context.assets, FontWeight.Bold)
    if (fonts.isNotEmpty() && fonts.none { it.weight == FontWeight.Bold }) fonts += Font("fonts/Vazirmatn-Regular.ttf", context.assets, FontWeight.Bold)
    return if (fonts.isEmpty()) FontFamily.Default else FontFamily(fonts)
}

private fun typography(ff: FontFamily): Typography {
    val d = Typography()
    return Typography(
        displayLarge = d.displayLarge.copy(fontFamily = ff),
        displayMedium = d.displayMedium.copy(fontFamily = ff),
        displaySmall = d.displaySmall.copy(fontFamily = ff),
        headlineLarge = d.headlineLarge.copy(fontFamily = ff),
        headlineMedium = d.headlineMedium.copy(fontFamily = ff),
        headlineSmall = d.headlineSmall.copy(fontFamily = ff),
        titleLarge = d.titleLarge.copy(fontFamily = ff),
        titleMedium = d.titleMedium.copy(fontFamily = ff),
        titleSmall = d.titleSmall.copy(fontFamily = ff),
        bodyLarge = d.bodyLarge.copy(fontFamily = ff),
        bodyMedium = d.bodyMedium.copy(fontFamily = ff),
        bodySmall = d.bodySmall.copy(fontFamily = ff),
        labelLarge = d.labelLarge.copy(fontFamily = ff),
        labelMedium = d.labelMedium.copy(fontFamily = ff),
        labelSmall = d.labelSmall.copy(fontFamily = ff),
    )
}

@Composable
fun PetroTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val family = remember { loadVazirmatn(context) }
    val scheme = darkColorScheme(
        primary = PetroColors.Brand, onPrimary = Color.White,
        secondary = PetroColors.Brand2, onSecondary = Color.White,
        tertiary = PetroColors.Green,
        background = PetroColors.Bg, onBackground = PetroColors.Text,
        surface = PetroColors.Panel, onSurface = PetroColors.Text,
        surfaceVariant = PetroColors.Panel2, onSurfaceVariant = PetroColors.TextDim,
        outline = PetroColors.Border, outlineVariant = PetroColors.Border,
        error = PetroColors.Red, onError = Color.White,
    )
    MaterialTheme(colorScheme = scheme, typography = remember(family) { typography(family) }) {
        // رابط فارسی است؛ جهت راست‌به‌چپ مستقل از زبان گوشی اجباری است
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
            LocalContentColor provides PetroColors.Text,
        ) { content() }
    }
}
