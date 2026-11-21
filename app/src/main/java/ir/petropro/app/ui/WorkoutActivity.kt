package ir.petropro.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ir.petropro.app.R
import ir.petropro.app.core.Formatters
import ir.petropro.app.core.TimeUtils
import ir.petropro.app.databinding.ActivityWorkoutBinding
import ir.petropro.app.databinding.ItemExerciseBinding
import ir.petropro.app.databinding.ItemSetRowBinding
import kotlinx.coroutines.launch
import java.io.File

class WorkoutActivity : BaseActivity() {
    private lateinit var binding: ActivityWorkoutBinding
    private val dayId: Int by lazy { intent.getIntExtra(EXTRA_DAY_ID, 0) }
    private val viewModel: WorkoutViewModel by viewModels { ViewModelFactory(container, dayId) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val adapter = ExerciseAdapter { ex, setNumber, done, weight, reps -> viewModel.logSet(ex, setNumber, done, weight, reps) }
        binding.recyclerItems.layoutManager = LinearLayoutManager(this)
        binding.recyclerItems.adapter = adapter
        // پرش‌های کوچک هنگام به‌روزرسانی ردیف‌ها نباید فیلدهای در حال تایپ را بپراند
        binding.recyclerItems.itemAnimator = null

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ui.collect { ui ->
                    binding.toolbar.title = ui.day?.dayName.orEmpty()
                    val missing = ui.day == null && ui.exercises.isEmpty()
                    binding.textNotFound.visibility = if (missing) View.VISIBLE else View.GONE
                    adapter.submitList(ui.exercises)
                }
            }
        }
    }

    companion object {
        const val EXTRA_DAY_ID = "day_id"
    }
}

private class ExerciseAdapter(
    private val onSetToggle: (ExerciseUi, Int, Boolean, Double?, Int?) -> Unit,
) : ListAdapter<ExerciseUi, ExerciseAdapter.VH>(DIFF) {

    class VH(val binding: ItemExerciseBinding) : RecyclerView.ViewHolder(binding.root)

    /** مقدارهای تایپ‌شده‌ی ستِ ثبت‌نشده؛ با بازسازی ردیف‌ها (بعد از ثبت یک ست دیگر) از بین نروند */
    private val typed = HashMap<String, Pair<String, String>>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemExerciseBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ex = getItem(position)
        val b = holder.binding
        val ctx = b.root.context
        val item = ex.item

        b.textName.text = item.nameFa ?: item.name
        b.textMeta.text = listOfNotNull(
            item.equipmentFa?.takeIf { it.isNotBlank() },
            if (item.isWarmup) ctx.getString(R.string.workout_warmup) else null,
        ).joinToString(" • ")
        b.textMeta.visibility = if (b.textMeta.text.isNullOrEmpty()) View.GONE else View.VISIBLE

        val planParts = mutableListOf(ctx.getString(R.string.workout_sets_format, item.sets, item.reps ?: "—"))
        item.restSeconds?.let { planParts += ctx.getString(R.string.workout_rest_format, it) }
        item.weightKg?.let { planParts += ctx.getString(R.string.workout_weight_format, Formatters.weight(it)) }
        b.textPlan.text = planParts.joinToString(" • ")

        b.textNotes.visibility = if (item.notes.isNullOrBlank()) View.GONE else View.VISIBLE
        b.textNotes.text = item.notes.orEmpty()

        b.textInstructions.visibility = if (ex.instructions.isBlank()) View.GONE else View.VISIBLE
        b.textInstructions.text = ex.instructions
        b.textInstructions.maxLines = 3
        b.textInstructions.setOnClickListener {
            b.textInstructions.maxLines = if (b.textInstructions.maxLines == 3) Int.MAX_VALUE else 3
        }

        bindMedia(b, ex)
        bindSets(b, ex)
    }

    private fun bindMedia(b: ItemExerciseBinding, ex: ExerciseUi) {
        val file = ex.localMediaPath?.let { File(it) }?.takeIf { it.isFile }
        b.imageMedia.visibility = View.GONE
        b.videoMedia.visibility = View.GONE
        b.textMediaStatus.visibility = View.GONE
        when {
            ex.mediaKind == null -> {
                b.textMediaStatus.setText(R.string.workout_no_media)
                b.textMediaStatus.visibility = View.VISIBLE
            }
            file == null -> {
                b.textMediaStatus.setText(R.string.workout_media_missing)
                b.textMediaStatus.visibility = View.VISIBLE
            }
            ex.mediaKind == "video" -> {
                b.videoMedia.visibility = View.VISIBLE
                b.videoMedia.setVideoPath(file.absolutePath)
                b.videoMedia.setOnPreparedListener { mp ->
                    mp.isLooping = true
                    mp.setVolume(0f, 0f)
                }
                b.videoMedia.start()
            }
            else -> {
                // فایل محلی؛ GIF با فرمت اصلی و بدون اینترنت متحرک نمایش داده می‌شود
                b.imageMedia.visibility = View.VISIBLE
                Glide.with(b.imageMedia).load(file).into(b.imageMedia)
            }
        }
    }

    private fun bindSets(b: ItemExerciseBinding, ex: ExerciseUi) {
        val ctx = b.root.context
        val item = ex.item
        b.containerSets.removeAllViews()
        val inflater = LayoutInflater.from(ctx)
        for (n in 1..item.sets) {
            val row = ItemSetRowBinding.inflate(inflater, b.containerSets, false)
            val log = ex.logs[n]
            val done = log?.done == true
            val key = "${item.id}:$n"
            val saved = typed[key]
            row.textSetNumber.text = ctx.getString(R.string.workout_set_number, n)
            row.inputWeight.setText(
                when {
                    log?.weightKg != null -> Formatters.weight(log.weightKg)
                    saved != null -> saved.first
                    else -> Formatters.weight(item.weightKg)
                }
            )
            row.inputReps.setText(
                when {
                    log?.repsDone != null -> log.repsDone.toString()
                    saved != null -> saved.second
                    else -> TimeUtils.firstInt(item.reps)?.toString().orEmpty()
                }
            )
            row.inputWeight.isEnabled = !done
            row.inputReps.isEnabled = !done
            row.checkDone.isChecked = done
            row.inputWeight.doAfterTextChanged { typed[key] = text(row.inputWeight) to text(row.inputReps) }
            row.inputReps.doAfterTextChanged { typed[key] = text(row.inputWeight) to text(row.inputReps) }
            row.checkDone.setOnCheckedChangeListener { _, checked ->
                val weight = Formatters.normalizeDigits(text(row.inputWeight)).toDoubleOrNull()
                val reps = Formatters.normalizeDigits(text(row.inputReps)).toIntOrNull()
                if (checked) typed.remove(key)
                onSetToggle(ex, n, checked, weight, reps)
            }
            b.containerSets.addView(row.root)
        }
    }

    private fun text(v: EditText) = v.text?.toString().orEmpty()

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<ExerciseUi>() {
            override fun areItemsTheSame(a: ExerciseUi, b: ExerciseUi) = a.item.id == b.item.id
            override fun areContentsTheSame(a: ExerciseUi, b: ExerciseUi) = a == b
        }
    }
}
