package ir.petropro.app.ui.components

import android.content.Context
import android.graphics.Typeface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.json.JSONObject

/**
 * آیکون‌های Tabler (همان نسخه‌ی وب، 2.47.0). فونت و نقشه‌ی نام→کدنقطه را workflow هنگام Build در assets می‌گذارد
 * (fonts/tabler-icons.ttf و tabler-map.json). اگر نبودند، متن جایگزین (fallback) نشان داده می‌شود.
 */
object TablerIcons {
    private var loaded = false
    private var family: FontFamily? = null
    private var codepoints: Map<String, Int> = emptyMap()

    @Synchronized
    fun glyph(context: Context, name: String): Pair<FontFamily, String>? {
        if (!loaded) {
            loaded = true
            family = try {
                FontFamily(Typeface.createFromAsset(context.assets, "fonts/tabler-icons.ttf"))
            } catch (e: Exception) {
                null
            }
            codepoints = try {
                val obj = JSONObject(context.assets.open("tabler-map.json").bufferedReader().use { it.readText() })
                obj.keys().asSequence().associateWith { obj.getInt(it) }
            } catch (e: Exception) {
                emptyMap()
            }
        }
        val ff = family ?: return null
        val cp = codepoints[name] ?: return null
        return ff to String(Character.toChars(cp))
    }
}

@Composable
fun TablerIcon(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    tint: Color = LocalContentColor.current,
    fallback: String = "•",
) {
    val context = LocalContext.current
    val g = remember(name) { TablerIcons.glyph(context, name) }
    val sp = with(LocalDensity.current) { size.toSp() }
    if (g == null) {
        Box(modifier.size(size), contentAlignment = Alignment.Center) { Text(fallback, color = tint, fontSize = sp * 0.8f) }
    } else {
        Box(modifier.size(size), contentAlignment = Alignment.Center) { Text(g.second, fontFamily = g.first, fontSize = sp, color = tint) }
    }
}
