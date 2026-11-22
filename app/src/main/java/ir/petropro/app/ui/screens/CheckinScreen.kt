package ir.petropro.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.petropro.app.core.Formatters
import ir.petropro.app.core.TimeUtils
import ir.petropro.app.database.CheckinEntity
import ir.petropro.app.ui.CheckinViewModel
import ir.petropro.app.ui.ViewModelFactory
import ir.petropro.app.ui.components.PetroCard
import ir.petropro.app.ui.components.PrimaryButton
import ir.petropro.app.ui.components.petroFieldColors
import ir.petropro.app.ui.theme.PetroColors

@Composable
fun CheckinScreen(factory: ViewModelFactory) {
    val vm: CheckinViewModel = viewModel(factory = factory)
    val today by vm.today.collectAsStateWithLifecycle()

    var sleep by remember { mutableStateOf("") }
    var sleepQuality by remember { mutableFloatStateOf(3f) }
    var energy by remember { mutableFloatStateOf(3f) }
    var fatigue by remember { mutableFloatStateOf(3f) }
    var stress by remember { mutableFloatStateOf(3f) }
    var soreness by remember { mutableFloatStateOf(3f) }
    var pain by remember { mutableFloatStateOf(0f) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var prefilled by remember { mutableStateOf(false) }

    LaunchedEffect(today) {
        val c = today
        if (c != null && !prefilled) {
            prefilled = true
            sleep = Formatters.weight(c.sleepHours)
            sleepQuality = c.sleepQuality.toFloat(); energy = c.energy.toFloat(); fatigue = c.fatigue.toFloat()
            stress = c.stress.toFloat(); soreness = c.soreness.toFloat(); pain = c.painLevel.toFloat()
            note = c.note.orEmpty()
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PetroCard {
                Text("چک‌این امروز", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PetroColors.Brand)
                Text("۱ = خیلی کم، ۵ = خیلی زیاد", color = PetroColors.TextMute, fontSize = 12.sp)
                OutlinedTextField(
                    value = sleep, onValueChange = { sleep = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    label = { Text("ساعات خواب (۰ تا ۲۴)") }, colors = petroFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                ScaleRow("کیفیت خواب", sleepQuality, 1f..5f, 3) { sleepQuality = it }
                ScaleRow("سطح انرژی", energy, 1f..5f, 3) { energy = it }
                ScaleRow("خستگی", fatigue, 1f..5f, 3) { fatigue = it }
                ScaleRow("استرس", stress, 1f..5f, 3) { stress = it }
                ScaleRow("درد عضلانی", soreness, 1f..5f, 3) { soreness = it }
                ScaleRow("شدت درد (۰ تا ۱۰)", pain, 0f..10f, 9) { pain = it }
                OutlinedTextField(
                    value = note, onValueChange = { if (it.length <= 300) note = it }, modifier = Modifier.fillMaxWidth(),
                    label = { Text("یادداشت (اختیاری)") }, colors = petroFieldColors(), minLines = 2,
                )
                error?.let { Text(it, color = PetroColors.Red, fontSize = 13.sp) }
                today?.let {
                    Text(
                        if (it.dirty) "ذخیره شد و پس از اتصال ارسال می‌شود" else "ذخیره و ارسال شد",
                        color = PetroColors.Green, fontSize = 13.sp,
                    )
                }
                PrimaryButton("ذخیره", modifier = Modifier.fillMaxWidth(), onClick = {
                    val raw = Formatters.normalizeDigits(sleep)
                    val hours = raw.toDoubleOrNull()
                    if (raw.isNotEmpty() && (hours == null || hours < 0.0 || hours > 24.0)) {
                        error = "ساعات خواب باید عددی بین ۰ تا ۲۴ باشد"
                    } else {
                        error = null
                        vm.save(
                            CheckinEntity(
                                date = TimeUtils.todayTehran(), sleepHours = hours,
                                sleepQuality = sleepQuality.toInt(), energy = energy.toInt(), fatigue = fatigue.toInt(),
                                stress = stress.toInt(), soreness = soreness.toInt(), painLevel = pain.toInt(),
                                note = note.trim().ifEmpty { null }, dirty = true,
                            )
                        )
                    }
                })
            }
        }
    }
}

@Composable
private fun ScaleRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = PetroColors.Text, fontSize = 14.sp)
        Text(value.toInt().toString(), color = PetroColors.Brand, fontWeight = FontWeight.Bold)
    }
    Slider(
        value = value, onValueChange = onChange, valueRange = range, steps = steps,
        colors = SliderDefaults.colors(
            thumbColor = PetroColors.Brand, activeTrackColor = PetroColors.Brand,
            inactiveTrackColor = PetroColors.Border, activeTickColor = PetroColors.Brand, inactiveTickColor = PetroColors.TextMute,
        ),
    )
}
