package ir.petropro.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ir.petropro.app.PetroProApp
import ir.petropro.app.R
import ir.petropro.app.databinding.FragmentProgramBinding
import ir.petropro.app.databinding.ItemDayBinding
import ir.petropro.app.sync.SyncOutcome
import kotlinx.coroutines.launch

class ProgramFragment : Fragment() {
    private var _binding: FragmentProgramBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProgramViewModel by viewModels {
        ViewModelFactory((requireActivity().application as PetroProApp).container)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProgramBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = DayAdapter { row ->
            startActivity(Intent(requireContext(), WorkoutActivity::class.java).putExtra(WorkoutActivity.EXTRA_DAY_ID, row.day.id))
        }
        binding.recyclerDays.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerDays.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh { outcome ->
                _binding?.swipeRefresh?.isRefreshing = false
                val msg = when (outcome) {
                    SyncOutcome.OK -> R.string.sync_result_ok
                    SyncOutcome.OFFLINE, SyncOutcome.SERVER_ERROR -> R.string.sync_result_offline
                    SyncOutcome.AUTH_REQUIRED -> R.string.sync_result_auth
                }
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ui.collect { ui ->
                    val program = ui.program
                    binding.textProgramTitle.visibility = if (program != null) View.VISIBLE else View.GONE
                    binding.textProgramTitle.text = program?.title.orEmpty()
                    binding.textProgramNotes.visibility = if (program?.notes.isNullOrBlank()) View.GONE else View.VISIBLE
                    binding.textProgramNotes.text = program?.notes.orEmpty()
                    binding.textEmpty.visibility = if (program == null) View.VISIBLE else View.GONE
                    adapter.submitList(ui.days)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private class DayAdapter(private val onClick: (DayRow) -> Unit) : ListAdapter<DayRow, DayAdapter.VH>(DIFF) {
    class VH(val binding: ItemDayBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemDayBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = getItem(position)
        holder.binding.textDayName.text = row.day.dayName
        holder.binding.textDayCount.text = holder.itemView.context.getString(R.string.program_day_items, row.itemCount)
        holder.itemView.setOnClickListener { onClick(row) }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<DayRow>() {
            override fun areItemsTheSame(a: DayRow, b: DayRow) = a.day.id == b.day.id
            override fun areContentsTheSame(a: DayRow, b: DayRow) = a == b
        }
    }
}
