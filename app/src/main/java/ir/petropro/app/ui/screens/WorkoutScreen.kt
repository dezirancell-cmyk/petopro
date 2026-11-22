package ir.petropro.app.ui.screens

import android.widget.ImageView
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.Glide
import ir.petropro.app.core.AppContainer
import ir.petropro.app.core.Formatters
import ir.petropro.app.core.TimeUtils
import ir.petropro.app.database.SetLogEntity
import ir.petropro.app.ui.ExerciseUi
import ir.petropro.app.ui.ViewModelFactory
import ir.petropro.app.ui.WorkoutViewModel
import ir.petropro.app.ui.components.EmptyState
import ir.petropro.app.ui.components.PetroCard
import ir.petropro.app.ui.components.Pill
import ir.petropro.app.ui.components.petroFieldColors
import ir.petropro.app.ui.theme.PetroColors
import java.io.File

@Composable
fun WorkoutScreen(container: AppContainer, dayId: Int) {
    val vm: WorkoutViewModel = viewModel(key = "workout-$dayId", factory = ViewModelFactory(container, dayId))
    val ui by vm.ui.collectAsStateWithLifecycle()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "ثبت‌ها در گوشی ذخیره می‌شوند و با اتصال اینترنت خودکار ارسال می‌شوند. برای ویرایش یک ست ثبت‌شده، تیک آن را بردارید و دوباره ثبت کنید.",
                color = PetroColors.TextMute, fontSize = 12.sp,
            )
        }
        if (ui.day == null && ui.exercises.isEmpty()) {
            item { EmptyState("این روز دیگر در برنامه وجود ندارد (برنامه توسط مربی تغییر کرده است).") }
        }
        items(ui.exercises, key = { it.item.id }) { ex ->
            ExerciseCard(ex) { setNumber, done, weight, reps -> vm.logSet(ex, setNumber, done, weight, reps) }
        }
    }
}

@Composable
private fun ExerciseCard(ex: ExerciseUi, onToggle: (Int, Boolean, Double?, Int?) -> Unit) {
    val item = ex.item
    var expanded by remember { mutableStateOf(false) }
    PetroCard(padding = 14) {
        Text(item.nameFa ?: item.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = PetroColors.Text)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!item.equipmentFa.isNullOrBlank()) Text(item.equipmentFa, color = PetroColors.TextDim, fontSize = 12.sp)
            if (item.isWarmup) Pill("گرم‌کردن", PetroColors.Yellow)
        }
        MediaView(ex)
        val plan = buildList {
            add("${item.sets} ست × ${item.reps ?: "—"}")
            item.restSeconds?.let { add("استراحت $it ثانیه") }
            item.weightKg?.let { add("وزنه‌ی برنامه: ${Formatters.weight(it)} کیلوگرم") }
        }.joinToString(" • ")
        Text(plan, color = PetroColors.Text, fontSize = 14.sp)
        if (!item.notes.isNullOrBlank()) Text(item.notes, color = PetroColors.TextDim, fontSize = 13.sp)
        if (ex.instructions.isNotBlank()) {
            Text(
                ex.instructions, color = PetroColors.TextDim, fontSize = 13.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                modifier = Modifier.clickableNoRipple { expanded = !expanded },
            )
        }
        for (n in 1..item.sets) {
            SetRow(n, ex.logs[n], item.weightKg, TimeUtils.firstInt(item.reps)) { done, w, r -> onToggle(n, done, w, r) }
        }
    }
}

@Composable
private fun SetRow(n: Int, log: SetLogEntity?, plannedWeight: Double?, plannedReps: Int?, onToggle: (Boolean, Double?, Int?) -> Unit) {
    val done = log?.done == true
    val loggedWeight = log?.weightKg
    val loggedReps = log?.repsDone
    var weight by remember(done, loggedWeight) { mutableStateOf(Formatters.weight(loggedWeight ?: plannedWeight)) }
    var reps by remember(done, loggedReps) { mutableStateOf((loggedReps ?: plannedReps)?.toString().orEmpty()) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ست $n", color = PetroColors.Text, modifier = Modifier.width(52.dp), fontSize = 14.sp)
        OutlinedTextField(
            value = weight, onValueChange = { weight = it }, enabled = !done, singleLine = true,
            modifier = Modifier.weight(1f), placeholder = { Text("وزن (kg)", fontSize = 12.sp) },
            textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 15.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), colors = petroFieldColors(),
        )
        OutlinedTextField(
            value = reps, onValueChange = { reps = it }, enabled = !done, singleLine = true,
            modifier = Modifier.weight(1f), placeholder = { Text("تکرار", fontSize = 12.sp) },
            textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 15.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = petroFieldColors(),
        )
        Checkbox(
            checked = done,
            onCheckedChange = { checked ->
                onToggle(
                    checked,
                    Formatters.normalizeDigits(weight).toDoubleOrNull(),
                    Formatters.normalizeDigits(reps).toIntOrNull(),
                )
            },
            colors = CheckboxDefaults.colors(checkedColor = PetroColors.Brand, uncheckedColor = PetroColors.TextDim),
        )
    }
}

/** فایل محلیِ دانلودشده؛ GIF با فرمت اصلی و بدون اینترنت نمایش داده می‌شود. */
@Composable
private fun MediaView(ex: ExerciseUi) {
    val file = ex.localMediaPath?.let { File(it) }?.takeIf { it.isFile }
    Box(
        Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(9.dp)).background(PetroColors.Bg2),
        contentAlignment = Alignment.Center,
    ) {
        when {
            ex.mediaKind == null -> Text("این حرکت تصویر ندارد", color = PetroColors.TextMute, fontSize = 13.sp)
            file == null -> Text("رسانه‌ی این حرکت هنوز دانلود نشده است", color = PetroColors.TextMute, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
            ex.mediaKind == "video" -> AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx -> VideoView(ctx) },
                update = { vv ->
                    if (vv.tag != file.absolutePath) {
                        vv.tag = file.absolutePath
                        vv.setVideoPath(file.absolutePath)
                        vv.setOnPreparedListener { mp ->
                            mp.isLooping = true
                            mp.setVolume(0f, 0f)
                        }
                        vv.start()
                    }
                },
            )
            else -> AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
                update = { iv ->
                    if (iv.tag != file.absolutePath) {
                        iv.tag = file.absolutePath
                        Glide.with(iv).load(file).into(iv)
                    }
                },
            )
        }
    }
}
