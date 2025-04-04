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

/**
 * Activity para mostrar el tablero de debates.
 *
 * Esta pantalla muestra la lista de debates disponibles y permite al usuario
 * crear nuevos debates mediante un botón flotante. También se configura la toolbar
 * y se observa el ViewModel para actualizar la lista de debates.
 */
class DebateBoardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDebateBoardBinding
    private val viewModel: DebateBoardViewModel by viewModels()
    private lateinit var adapter: DebateAdapter

    /**
     * Configura la Activity al crearla.
     *
     * Se inicializa el Data Binding, se configura la toolbar, el RecyclerView,
     * el botón flotante y se establece la observación del ViewModel.
     *
     * @param savedInstanceState Estado anterior de la Activity.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_debate_board)

        setupToolbar()
        setupRecyclerView()
        setupFab()
        observeViewModel()
    }

    /**
     * Configura la toolbar de la Activity.
     *
     * Establece la toolbar como ActionBar, activa el botón de retorno y define
     * su acción para finalizar la Activity.
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    /**
     * Configura el RecyclerView que muestra la lista de debates.
     *
     * Inicializa el adapter y establece el LinearLayoutManager junto con el adapter.
     * Al hacer clic en el botón de "unirse" a un debate se muestra un Toast.
     */
    private fun setupRecyclerView() {
        adapter = DebateAdapter { debate ->
            // Acción para unirse al debate (por implementar)
            Toast.makeText(this, getString(R.string.joined_debate, debate.title), Toast.LENGTH_SHORT).show()
            Timber.i("Joined debate: ${debate.title}")
        }

        binding.recyclerDebates.layoutManager = LinearLayoutManager(this)
        binding.recyclerDebates.adapter = adapter
    }

    /**
     * Configura el botón flotante para crear un nuevo debate.
     *
     * Al hacer clic se muestra un diálogo para que el usuario ingrese el título
     * y la descripción del debate.
     */
    private fun setupFab() {
        binding.fabCreateDebate.setOnClickListener {
            showCreateDebateDialog()
        }
    }

    /**
     * Observa los cambios en la lista de debates del ViewModel.
     *
     * Actualiza la visibilidad del mensaje de "no hay debates" y del RecyclerView en
     * función de si la lista está vacía o no. Además, envía la lista actualizada al adapter.
     */
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

    /**
     * Muestra un diálogo para crear un nuevo debate.
     *
     * El diálogo permite al usuario ingresar el título y la descripción del debate.
     * Si los campos están completos, se agrega el debate mediante el ViewModel;
     * de lo contrario, se muestra un mensaje de error.
     */
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