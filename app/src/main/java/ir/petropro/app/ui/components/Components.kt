package ir.petropro.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.petropro.app.ui.theme.PetroColors
import ir.petropro.app.ui.theme.PetroShapes

private val cardShape = RoundedCornerShape(PetroShapes.Radius)
private val smShape = RoundedCornerShape(PetroShapes.RadiusSm)

/** معادل .card در وب: پس‌زمینه‌ی panel، حاشیه‌ی border، گوشه‌ی ۱۴، فاصله‌ی داخلی ۱۸ */
@Composable
fun PetroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Int = 18,
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = if (onClick != null) modifier.clip(cardShape).clickable(onClick = onClick) else modifier
    Surface(
        modifier = base.fillMaxWidth(),
        shape = cardShape,
        color = PetroColors.Panel,
        border = BorderStroke(1.dp, PetroColors.Border),
    ) {
        Column(Modifier.padding(padding.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier, shape = smShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = PetroColors.Brand, contentColor = Color.White,
            disabledContainerColor = PetroColors.Panel2, disabledContentColor = PetroColors.TextMute,
        ),
    ) { Text(text, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp) }
}

/** معادل .btn در وب */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick, enabled = enabled, modifier = modifier, shape = smShape,
        border = BorderStroke(1.dp, PetroColors.Border),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = PetroColors.Panel2, contentColor = PetroColors.Text),
    ) { Text(text, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp) }
}

/** معادل .pill در وب */
@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(99.dp)).background(color.copy(alpha = 0.18f)).padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = color, fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, color = PetroColors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier.fillMaxWidth().padding(24.dp), color = PetroColors.TextDim, fontSize = 14.sp)
}

@Composable
fun Banner(text: String, color: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val m = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    Box(m.fillMaxWidth().background(color.copy(alpha = 0.2f)).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(text, color = PetroColors.Text, fontSize = 13.sp)
    }
}

@Composable
fun petroFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PetroColors.Brand, unfocusedBorderColor = PetroColors.Border,
    disabledBorderColor = PetroColors.Border,
    focusedContainerColor = PetroColors.Bg2, unfocusedContainerColor = PetroColors.Bg2, disabledContainerColor = PetroColors.Bg2,
    focusedTextColor = PetroColors.Text, unfocusedTextColor = PetroColors.Text, disabledTextColor = PetroColors.TextDim,
    focusedLabelColor = PetroColors.Brand, unfocusedLabelColor = PetroColors.TextDim,
    cursorColor = PetroColors.Brand,
)
