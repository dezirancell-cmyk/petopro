package ir.petropro.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.petropro.app.core.AppContainer
import ir.petropro.app.core.JsonLists
import ir.petropro.app.core.TimeUtils
import ir.petropro.app.database.DayEntity
import ir.petropro.app.database.ItemEntity
import ir.petropro.app.database.SetLogEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** یک حرکت به‌همراه وضعیت ست‌ها و مسیر فایل رسانه‌ی «دانلودشده» (یا null اگر هنوز دانلود نشده) */
data class ExerciseUi(
    val item: ItemEntity,
    val logs: Map<Int, SetLogEntity>,
    val mediaKind: String?,       // gif | image | video | null (بدون رسانه)
    val localMediaPath: String?,
    val instructions: String,
)

data class WorkoutUi(val day: DayEntity?, val exercises: List<ExerciseUi>)

class WorkoutViewModel(private val c: AppContainer, private val dayId: Int) : ViewModel() {
    private val date = TimeUtils.todayTehran()

    val ui: StateFlow<WorkoutUi> = combine(
        c.programRepository.observeDay(dayId),
        c.programRepository.observeItems(dayId),
        c.workoutRepository.observeSetLogs(date),
        c.mediaCache.observeDone(),
    ) { day, items, logs, doneMedia ->
        val localByUrl = doneMedia.filter { it.localPath != null }.associate { it.url to it.localPath!! }
        val logsByItem = logs.groupBy { it.itemId }
        WorkoutUi(
            day = day,
            exercises = items.map { item ->
                val url = JsonLists.decode(item.imageUrlsJson).firstOrNull()
                val instructions = JsonLists.decode(item.instructionsFa).ifEmpty { JsonLists.decode(item.instructionsEn) }
                ExerciseUi(
                    item = item,
                    logs = (logsByItem[item.id] ?: emptyList()).associateBy { it.setNumber },
                    mediaKind = url?.let { kindOf(it) },
                    localMediaPath = url?.let { localByUrl[it] },
                    instructions = instructions.joinToString("\n"),
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutUi(null, emptyList()))

    fun logSet(ex: ExerciseUi, setNumber: Int, done: Boolean, weightKg: Double?, repsDone: Int?) {
        val day = ui.value.day ?: return
        viewModelScope.launch {
            c.workoutRepository.logSet(ex.item, day.programId, day.dayIndex, setNumber, done, weightKg, repsDone)
            c.scheduler.kickOutbox()
        }
    }

    private fun kindOf(url: String): String {
        val path = url.substringBefore('?').lowercase()
        return when {
            path.endsWith(".gif") -> "gif"
            path.endsWith(".mp4") || path.endsWith(".webm") || path.endsWith(".mov") -> "video"
            else -> "image"
        }
    }
}
