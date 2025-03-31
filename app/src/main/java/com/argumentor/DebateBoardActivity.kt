package com.argumentor

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.argumentor.adapters.DebateAdapter
import com.argumentor.databinding.ActivityDebateBoardBinding
import com.argumentor.models.Debate
import com.argumentor.viewmodels.DebateBoardViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import timber.log.Timber

class DebateBoardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDebateBoardBinding
    private val viewModel: DebateBoardViewModel by viewModels()
    private lateinit var adapter: DebateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_debate_board)

        setupToolbar()
        setupRecyclerView()
        setupFab()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        adapter = DebateAdapter { debate ->
            // Acción para unirse al debate (por implementar)
            Toast.makeText(this, getString(R.string.joined_debate, debate.title), Toast.LENGTH_SHORT).show()
            Timber.i("Joined debate: ${debate.title}")
        }

        binding.recyclerDebates.layoutManager = LinearLayoutManager(this)
        binding.recyclerDebates.adapter = adapter
    }

    private fun setupFab() {
        binding.fabCreateDebate.setOnClickListener {
            showCreateDebateDialog()
        }
    }

    private fun observeViewModel() {
        viewModel.debates.observe(this) { debates ->
            if (debates.isEmpty()) {
                binding.textNoDebates.visibility = View.VISIBLE
                binding.recyclerDebates.visibility = View.GONE
            } else {
                binding.textNoDebates.visibility = View.GONE
                binding.recyclerDebates.visibility = View.VISIBLE
                adapter.submitList(debates)
            }
        }
    }

    private fun showCreateDebateDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_debate, null)
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.editDebateTitle)
        val descriptionInput = dialogView.findViewById<TextInputEditText>(R.id.editDebateDescription)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.create_debate)
            .setView(dialogView)
            .setPositiveButton(R.string.create) { _, _ ->
                val title = titleInput.text.toString().trim()
                val description = descriptionInput.text.toString().trim()

                if (title.isNotEmpty() && description.isNotEmpty()) {
                    val author = getString(R.string.current_user)
                    viewModel.addDebate(title, description, author)
                    Timber.i("Created new debate: $title")
                } else {
                    Toast.makeText(this, R.string.empty_fields_error, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}