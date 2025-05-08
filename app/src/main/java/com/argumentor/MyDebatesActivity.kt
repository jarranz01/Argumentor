package com.argumentor

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.argumentor.adapters.DebateAdapter
import com.argumentor.databinding.ActivityMyDebatesBinding
import com.argumentor.models.Debate
import com.argumentor.viewmodels.MyDebatesViewModel
import timber.log.Timber

/**
 * Activity para mostrar la lista de debates en los que participa el usuario.
 * Muestra los debates separados en dos secciones: debates en curso y debates completados.
 */
class MyDebatesActivity : BaseLocaleActivity() {

    private lateinit var binding: ActivityMyDebatesBinding
    private val viewModel: MyDebatesViewModel by viewModels()
    private lateinit var ongoingAdapter: DebateAdapter
    private lateinit var completedAdapter: DebateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplicar configuración de idioma antes de inflar layouts
        applyStoredLanguageConfiguration()
        
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_my_debates)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        setupToolbar()
        setupRecyclerViews()
        setupSectionToggles()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerViews() {
        // Configurar adapter para debates en curso
        ongoingAdapter = DebateAdapter(
            onJoinClick = { debate -> navigateToDebateView(debate) },
            showJoinButton = false // No mostrar el botón de unirse en Mis Debates
        )

        binding.recyclerOngoingDebates.apply {
            layoutManager = LinearLayoutManager(this@MyDebatesActivity)
            adapter = ongoingAdapter
        }

        // Configurar adapter para debates completados
        completedAdapter = DebateAdapter(
            onJoinClick = { debate -> navigateToDebateView(debate) },
            showJoinButton = false
        )

        binding.recyclerCompletedDebates.apply {
            layoutManager = LinearLayoutManager(this@MyDebatesActivity)
            adapter = completedAdapter
        }
    }

    /**
     * Configura los listeners para expandir/colapsar secciones
     */
    private fun setupSectionToggles() {
        // Listener para la sección de debates en curso
        binding.headerOngoingDebates.setOnClickListener {
            viewModel.toggleOngoingSection()
        }

        // Listener para la sección de debates completados
        binding.headerCompletedDebates.setOnClickListener {
            viewModel.toggleCompletedSection()
        }
    }

    private fun observeViewModel() {
        // Observar debates en curso
        viewModel.ongoingDebates.observe(this) { debates ->
            ongoingAdapter.submitList(debates)
            
            // Mostrar mensaje si no hay debates en curso
            if (debates.isEmpty()) {
                binding.textNoOngoingDebates.visibility = View.VISIBLE
                binding.recyclerOngoingDebates.visibility = View.GONE
            } else {
                binding.textNoOngoingDebates.visibility = View.GONE
                binding.recyclerOngoingDebates.visibility = View.VISIBLE
            }
        }

        // Observar debates completados
        viewModel.completedDebates.observe(this) { debates ->
            completedAdapter.submitList(debates)
            
            // Mostrar mensaje si no hay debates completados
            if (debates.isEmpty()) {
                binding.textNoCompletedDebates.visibility = View.VISIBLE
                binding.recyclerCompletedDebates.visibility = View.GONE
            } else {
                binding.textNoCompletedDebates.visibility = View.GONE
                binding.recyclerCompletedDebates.visibility = View.VISIBLE
            }
        }

        // Observar si ambas listas están vacías para mostrar mensaje general
        viewModel.userDebates.observe(this) { debates ->
            binding.textNoDebates.visibility = if (debates.isEmpty()) View.VISIBLE else View.GONE
        }

        // Observar estado expandido/colapsado de la sección de debates en curso
        viewModel.isOngoingSectionExpanded.observe(this) { isExpanded ->
            binding.contentOngoingDebates.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.arrowOngoingDebates.setImageResource(
                if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )
        }

        // Observar estado expandido/colapsado de la sección de debates completados
        viewModel.isCompletedSectionExpanded.observe(this) { isExpanded ->
            binding.contentCompletedDebates.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.arrowCompletedDebates.setImageResource(
                if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )
        }
    }

    /**
     * Navega a la vista detallada del debate
     */
    private fun navigateToDebateView(debate: Debate) {
        val intent = Intent(this, DebateViewActivity::class.java)
        intent.putExtra("debate_id", debate.id)
        startActivity(intent)
        Timber.i("Navegando al debate: ${debate.title} (${debate.id})")
    }
} 