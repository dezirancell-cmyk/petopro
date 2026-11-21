package ir.petropro.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import ir.petropro.app.PetroProApp
import ir.petropro.app.R
import ir.petropro.app.core.Formatters
import ir.petropro.app.core.TimeUtils
import ir.petropro.app.database.CheckinEntity
import ir.petropro.app.databinding.FragmentCheckinBinding
import kotlinx.coroutines.launch

class CheckinFragment : Fragment() {
    private var _binding: FragmentCheckinBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CheckinViewModel by viewModels {
        ViewModelFactory((requireActivity().application as PetroProApp).container)
    }
    private var prefilled = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCheckinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.buttonSave.setOnClickListener { save() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.today.collect { c ->
                    if (c != null && !prefilled) {
                        prefilled = true
                        binding.inputSleepHours.setText(Formatters.weight(c.sleepHours))
                        binding.sliderSleepQuality.value = c.sleepQuality.toFloat()
                        binding.sliderEnergy.value = c.energy.toFloat()
                        binding.sliderFatigue.value = c.fatigue.toFloat()
                        binding.sliderStress.value = c.stress.toFloat()
                        binding.sliderSoreness.value = c.soreness.toFloat()
                        binding.sliderPain.value = c.painLevel.toFloat()
                        binding.inputNote.setText(c.note.orEmpty())
                    }
                    binding.textStatus.visibility = if (c == null) View.GONE else View.VISIBLE
                    if (c != null) {
                        binding.textStatus.setText(if (c.dirty) R.string.checkin_saved_pending else R.string.checkin_saved_synced)
                    }
                }
            }
        }
    }

    private fun save() {
        val sleepRaw = Formatters.normalizeDigits(binding.inputSleepHours.text?.toString().orEmpty())
        val sleep = sleepRaw.toDoubleOrNull()
        if (sleepRaw.isNotEmpty() && (sleep == null || sleep < 0.0 || sleep > 24.0)) {
            binding.inputSleepHours.error = getString(R.string.checkin_sleep_hours)
            return
        }
        viewModel.save(
            CheckinEntity(
                date = TimeUtils.todayTehran(),
                sleepHours = sleep,
                sleepQuality = binding.sliderSleepQuality.value.toInt(),
                energy = binding.sliderEnergy.value.toInt(),
                fatigue = binding.sliderFatigue.value.toInt(),
                stress = binding.sliderStress.value.toInt(),
                soreness = binding.sliderSoreness.value.toInt(),
                painLevel = binding.sliderPain.value.toInt(),
                note = binding.inputNote.text?.toString()?.trim()?.takeIf { it.isNotEmpty() },
                dirty = true,
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
