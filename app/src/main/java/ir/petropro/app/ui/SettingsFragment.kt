package ir.petropro.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import ir.petropro.app.core.Formatters
import ir.petropro.app.database.OutboxEntity
import ir.petropro.app.database.OutboxState
import ir.petropro.app.database.OutboxType
import ir.petropro.app.databinding.FragmentSettingsBinding
import ir.petropro.app.databinding.ItemIssueBinding
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels {
        ViewModelFactory((requireActivity().application as PetroProApp).container)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = IssueAdapter()
        binding.recyclerIssues.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerIssues.adapter = adapter

        binding.buttonSync.setOnClickListener { viewModel.syncNow() }
        binding.buttonClearMedia.setOnClickListener { viewModel.clearMedia() }
        binding.buttonRetryBlocked.setOnClickListener { viewModel.retryBlocked() }
        binding.buttonDismissIssues.setOnClickListener { viewModel.dismissIssues() }
        binding.buttonLogout.setOnClickListener { confirmLogout() }

        val store = viewModel.sessionStore
        binding.textServer.text = getString(R.string.settings_server, store.serverUrl.orEmpty())
        binding.textUser.text = getString(R.string.settings_user, store.fullName ?: store.username.orEmpty(), store.role.orEmpty())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.ui.collect { ui ->
                        binding.textPending.text = getString(R.string.settings_pending, ui.pending)
                        binding.textMedia.text = getString(
                            R.string.settings_media, ui.mediaDone, ui.mediaTotal, Formatters.bytes(ui.mediaBytes)
                        )
                        binding.textStorage.text = getString(R.string.settings_storage_free, Formatters.bytes(viewModel.freeBytes()))
                        val lastSync = store.lastSyncMillis
                        binding.textLastSync.text = if (lastSync == 0L) getString(R.string.settings_never_synced)
                        else getString(R.string.settings_last_sync, Formatters.dateTime(lastSync))
                        adapter.submitList(ui.issues)
                        binding.textIssuesEmpty.visibility = if (ui.issues.isEmpty()) View.VISIBLE else View.GONE
                        binding.buttonDismissIssues.visibility =
                            if (ui.issues.any { it.state != OutboxState.BLOCKED }) View.VISIBLE else View.GONE
                        binding.buttonRetryBlocked.visibility =
                            if (ui.issues.any { it.state == OutboxState.BLOCKED }) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.syncing.collect { running ->
                        binding.progressSync.visibility = if (running) View.VISIBLE else View.GONE
                        binding.buttonSync.isEnabled = !running
                        binding.buttonSync.setText(if (running) R.string.settings_sync_running else R.string.settings_sync_now)
                    }
                }
                launch {
                    viewModel.messages.collect { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }

    private fun confirmLogout() {
        viewLifecycleOwner.lifecycleScope.launch {
            val unsent = viewModel.unsentCount()
            val message = if (unsent > 0) getString(R.string.logout_confirm_pending, unsent) else getString(R.string.logout_confirm_clean)
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.logout_confirm_title)
                .setMessage(message)
                .setPositiveButton(R.string.logout_confirm_yes) { _, _ ->
                    viewModel.logout {
                        startActivity(Intent(requireContext(), LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private class IssueAdapter : ListAdapter<OutboxEntity, IssueAdapter.VH>(DIFF) {
    class VH(val binding: ItemIssueBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemIssueBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val op = getItem(position)
        val ctx = holder.itemView.context
        val what = if (op.type == OutboxType.SET_LOG) ctx.getString(R.string.issue_set_log) else ctx.getString(R.string.issue_checkin)
        val detail = op.lastError.orEmpty()
        holder.binding.textIssueTitle.text = what
        holder.binding.textIssueDetail.text = when (op.state) {
            OutboxState.CONFLICT -> ctx.getString(R.string.issue_conflict, detail)
            OutboxState.BLOCKED -> ctx.getString(R.string.issue_blocked, detail)
            else -> ctx.getString(R.string.issue_rejected, detail)
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<OutboxEntity>() {
            override fun areItemsTheSame(a: OutboxEntity, b: OutboxEntity) = a.seq == b.seq
            override fun areContentsTheSame(a: OutboxEntity, b: OutboxEntity) = a == b
        }
    }
}
