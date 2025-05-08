package com.argumentor.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.os.Build
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.argumentor.DebateViewActivity
import com.argumentor.R
import com.argumentor.databinding.FragmentDebateStageBinding
import com.argumentor.models.DebatePosition
import com.argumentor.models.DebateStage
import com.argumentor.viewmodels.DebateViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
import timber.log.Timber

/**
 * Fragmento que representa una etapa específica en el debate.
 * 
 * Se utiliza para mostrar y gestionar la interfaz para cada una de las
 * etapas del debate: introducción, refutaciones y conclusión.
 */
class DebateStageFragment : Fragment() {

    private var _binding: FragmentDebateStageBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: DebateViewModel by activityViewModels()
    
    private lateinit var debateStage: DebateStage
    
    private val MAX_CHARS = 2000
    
    companion object {
        private const val ARG_STAGE = "debate_stage"
        private const val REFRESH_INTERVAL = 10000L // 10 segundos
        
        /**
         * Crea una nueva instancia del fragmento para la etapa especificada.
         * 
         * @param stage La etapa del debate a representar
         * @return Una nueva instancia del fragmento
         */
        fun newInstance(stage: DebateStage): DebateStageFragment {
            return DebateStageFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_STAGE, stage)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.containsKey(ARG_STAGE)) {
                // Use newer API on Android Tiramisu (API 33) and above, fallback to deprecated method on older versions
                debateStage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    it.getSerializable(ARG_STAGE, DebateStage::class.java) ?: DebateStage.INTRODUCCION
                } else {
                    @Suppress("DEPRECATION")
                    it.getSerializable(ARG_STAGE) as? DebateStage ?: DebateStage.INTRODUCCION
                }
            }
        }
    }
    
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Asegurarnos de que estamos usando el contexto con el idioma correcto
        Timber.d("DebateStageFragment.onAttach() - Fragmento adjuntado al contexto")
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Obtener el contexto con la configuración regional correcta
        val contextWithCorrectLocale = requireContext()
        
        // Usar un inflater basado en ese contexto
        val localizedInflater = inflater.cloneInContext(contextWithCorrectLocale)
        
        // Usar _binding (la variable mutable) en lugar de binding (la propiedad de solo lectura)
        _binding = FragmentDebateStageBinding.inflate(localizedInflater, container, false)
        
        binding.viewModel = viewModel
        binding.stage = debateStage
        binding.lifecycleOwner = viewLifecycleOwner
        
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set initial position display
        setupInitialDisplay()
        
        // Setup the rest of the UI components
        setupPreviousOpponentResponse()
        setupResponseArea()
        checkCompletionStatus()
        
        // Configurar actualización periódica para detectar respuestas del oponente
        setupPeriodicUpdates()
        
        // Observar si el debate se ha cargado completamente para actualizar la UI
        viewModel.debateLoaded.observe(viewLifecycleOwner) { isLoaded ->
            if (isLoaded) {
                // Actualizar la información de posición cuando el debate esté completamente cargado
                setupStageInfo()
            }
        }
        
        // Observar cambios que puedan afectar a la visualización
        viewModel.debateEntries.observe(viewLifecycleOwner) { entries ->
            setupPreviousOpponentResponse()
            checkCompletionStatus()
        }
        
        viewModel.isUserTurn.observe(viewLifecycleOwner) { isUserTurn ->
            updateInputState(isUserTurn)
        }
    }
    
    /**
     * Configura una visualización inicial mientras se carga el debate.
     */
    private fun setupInitialDisplay() {
        try {
            // Establecer el título según la etapa
            val stageTitle = when (debateStage) {
                DebateStage.INTRODUCCION -> getString(R.string.stage_introduction)
                DebateStage.REFUTACION1 -> getString(R.string.stage_refutation1)
                DebateStage.REFUTACION2 -> getString(R.string.stage_refutation2)
                DebateStage.CONCLUSION -> getString(R.string.stage_conclusion)
            }
            binding.textStage.text = stageTitle
            
            // Establecer instrucciones usando directamente los recursos del fragmento
            val instructionText = when (debateStage) {
                DebateStage.INTRODUCCION -> getString(R.string.instruction_introduction)
                DebateStage.REFUTACION1 -> getString(R.string.instruction_refutation1)
                DebateStage.REFUTACION2 -> getString(R.string.instruction_refutation2)
                DebateStage.CONCLUSION -> getString(R.string.instruction_conclusion)
            }
            binding.textInstructions.text = instructionText
            
            // No mostrar la posición aún, se actualizará cuando el debate esté cargado
            binding.chipPosition.visibility = View.INVISIBLE
            
        } catch (e: Exception) {
            Timber.e(e, "Error en setupInitialDisplay")
        }
    }
    
    /**
     * Configura la información sobre la etapa actual y posición.
     */
    private fun setupStageInfo() {
        try {
            // La etapa ya fue establecida en setupInitialDisplay, solo necesitamos actualizar la posición
            
            try {
                // Configurar el chip de posición con la posición actual
                val userPosition = viewModel.getUserPosition()
                val positionText = when (userPosition) {
                    DebatePosition.A_FAVOR -> getString(R.string.position_favor)
                    DebatePosition.EN_CONTRA -> getString(R.string.position_against)
                }
                
                binding.chipPosition.text = positionText
                
                // Establecer color según la posición
                val colorRes = when (userPosition) {
                    DebatePosition.A_FAVOR -> android.R.color.holo_green_light
                    DebatePosition.EN_CONTRA -> android.R.color.holo_red_light
                }
                
                // Verificar que el contexto esté disponible antes de usar ContextCompat
                if (isAdded && !isDetached && context != null) {
                    binding.chipPosition.chipBackgroundColor = 
                        ContextCompat.getColorStateList(requireContext(), colorRes)
                }
                
                // Ahora que tenemos la posición correcta, mostrar el chip
                binding.chipPosition.visibility = View.VISIBLE
                
            } catch (e: Exception) {
                Timber.e(e, "Error al configurar el chip de posición")
                // Configurar un valor por defecto para evitar UI vacía
                binding.chipPosition.text = getString(R.string.position_favor)
                binding.chipPosition.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            Timber.e(e, "Error en setupStageInfo")
        }
    }
    
    /**
     * Configura la visualización de la respuesta previa del oponente (si existe).
     * Visible en etapas REFUTACION1, REFUTACION2 y CONCLUSION.
     */
    private fun setupPreviousOpponentResponse() {
        try {
            // Solo aplica para refutación 1, 2 y conclusión
            if (debateStage == DebateStage.INTRODUCCION) {
                binding.cardPrevOpponentArgument.visibility = View.GONE
                return
            }
            
            // Obtener la respuesta del oponente en la etapa anterior a la actual
            val previousStage = when (debateStage) {
                DebateStage.REFUTACION1 -> DebateStage.INTRODUCCION
                DebateStage.REFUTACION2 -> DebateStage.REFUTACION1
                DebateStage.CONCLUSION -> DebateStage.REFUTACION2
                else -> null
            }
            
            try {
                val previousOpponentEntry = previousStage?.let { prevStage ->
                    val opponentPosition = if (viewModel.getUserPosition() == DebatePosition.A_FAVOR) {
                        DebatePosition.EN_CONTRA
                    } else {
                        DebatePosition.A_FAVOR
                    }
                    viewModel.getEntryForStageAndPosition(prevStage, opponentPosition)
                }
                
                if (previousOpponentEntry != null) {
                    binding.cardPrevOpponentArgument.visibility = View.VISIBLE
                    binding.textPrevOpponentArgument.text = previousOpponentEntry.content
                    
                    // Configurar comportamiento del botón para mostrar/ocultar
                    binding.buttonTogglePrevResponse.setOnClickListener {
                        try {
                            val isVisible = binding.textPrevOpponentArgument.visibility == View.VISIBLE
                            if (isVisible) {
                                binding.textPrevOpponentArgument.visibility = View.GONE
                                binding.buttonTogglePrevResponse.text = getString(R.string.show_opponent_response)
                            } else {
                                binding.textPrevOpponentArgument.visibility = View.VISIBLE
                                binding.buttonTogglePrevResponse.text = getString(R.string.hide_opponent_response)
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error al cambiar visibilidad de respuesta del oponente")
                        }
                    }
                } else {
                    binding.cardPrevOpponentArgument.visibility = View.GONE
                }
            } catch (e: Exception) {
                Timber.e(e, "Error al obtener entrada del oponente")
                binding.cardPrevOpponentArgument.visibility = View.GONE
            }
        } catch (e: Exception) {
            Timber.e(e, "Error en setupPreviousOpponentResponse")
            binding.cardPrevOpponentArgument.visibility = View.GONE
        }
    }
    
    /**
     * Configura el área para escribir e introducir la respuesta.
     */
    private fun setupResponseArea() {
        // Configurar contador de caracteres
        binding.textCharacterCount.text = getString(R.string.char_limit, 0, MAX_CHARS)
        
        // Observador para el conteo de caracteres
        binding.editResponse.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentLength = s?.length ?: 0
                binding.textCharacterCount.text = getString(R.string.char_limit, currentLength, MAX_CHARS)
                
                // Desactivar botón si excede el límite
                binding.buttonSubmit.isEnabled = currentLength <= MAX_CHARS && currentLength > 0
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Configurar botón de enviar
        binding.buttonSubmit.setOnClickListener {
            val content = binding.editResponse.text.toString().trim()
            if (content.isNotEmpty() && content.length <= MAX_CHARS) {
                if (viewModel.submitEntry(content)) {
                    // Mostrar mensaje de confirmación
                    Snackbar.make(binding.root, R.string.debate_stage_complete, LENGTH_SHORT).show()
                    
                    // Actualizar la UI inmediatamente
                    binding.textSubmittedResponse.text = content
                    binding.textSubmittedResponse.visibility = View.VISIBLE
                    binding.inputLayout.visibility = View.GONE
                    binding.buttonSubmit.visibility = View.GONE
                    binding.textCharacterCount.visibility = View.GONE
                    
                    // Mostrar mensaje de espera para la respuesta del oponente
                    binding.textWaitingState.text = getString(R.string.waiting_for_opponent)
                    binding.textWaitingState.visibility = View.VISIBLE
                    
                    // Observar cambios en la etapa actual para navegar automáticamente
                    viewModel.currentStage.observe(viewLifecycleOwner) { newStage ->
                        if (newStage != debateStage) {
                            // Si la etapa cambió, navegar a la nueva pestaña
                            val activity = requireActivity() as? DebateViewActivity
                            activity?.navigateToStage(newStage)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Verifica el estado de completitud de la etapa y actualiza la UI adecuadamente.
     */
    private fun checkCompletionStatus() {
        val isCompleted = viewModel.isStageCompletedByUser(debateStage)
        
        if (isCompleted) {
            // Mostrar la entrada completada
            val userEntry = viewModel.getUserEntry(debateStage)
            if (userEntry != null) {
                binding.textSubmittedResponse.text = userEntry.content
                binding.textSubmittedResponse.visibility = View.VISIBLE
            }
            
            // Ocultar el campo de entrada y el botón de submit
            binding.inputLayout.visibility = View.GONE
            binding.buttonSubmit.visibility = View.GONE
            binding.textCharacterCount.visibility = View.GONE
            
            // Mostrar estado de espera si es necesario
            // En la fase de introducción, no mostramos mensaje de espera
            if (debateStage == DebateStage.INTRODUCCION) {
                binding.textWaitingState.visibility = View.GONE
            } else {
                val opponentHasResponded = when (debateStage) {
                    DebateStage.REFUTACION1 -> viewModel.getOpponentEntryToRefute(DebateStage.REFUTACION1) != null
                    DebateStage.REFUTACION2 -> viewModel.getOpponentEntryToRefute(DebateStage.REFUTACION2) != null
                    DebateStage.CONCLUSION -> viewModel.getOpponentEntryToRefute(DebateStage.CONCLUSION) != null
                    else -> true // Para cualquier otro caso, asumimos que ha respondido
                }
                
                if (!opponentHasResponded) {
                    binding.textWaitingState.text = getString(R.string.waiting_for_opponent)
                    binding.textWaitingState.visibility = View.VISIBLE
                } else {
                    binding.textWaitingState.visibility = View.GONE
                }
            }
        } else {
            // Verificar si el usuario debe esperar a que el oponente responda en fases anteriores
            val canUserRespond = when (debateStage) {
                DebateStage.INTRODUCCION -> true // Siempre puede responder en introducción
                DebateStage.REFUTACION1 -> {
                    // Debe tener la introducción de ambos
                    viewModel.isStageBothCompleted(DebateStage.INTRODUCCION)
                }
                DebateStage.REFUTACION2 -> {
                    // Debe tener la refutación 1 de ambos
                    viewModel.isStageBothCompleted(DebateStage.REFUTACION1)
                }
                DebateStage.CONCLUSION -> {
                    // Debe tener la refutación 2 de ambos
                    viewModel.isStageBothCompleted(DebateStage.REFUTACION2)
                }
            }
            
            if (!canUserRespond) {
                // No puede responder aún - mostrar mensaje de espera
                binding.textSubmittedResponse.visibility = View.GONE
                binding.inputLayout.visibility = View.GONE
                binding.buttonSubmit.visibility = View.GONE
                binding.textCharacterCount.visibility = View.GONE
                
                // No mostrar mensaje de espera en la introducción
                if (debateStage == DebateStage.INTRODUCCION) {
                    binding.textWaitingState.visibility = View.GONE
                } else {
                    binding.textWaitingState.text = getString(R.string.must_wait_for_opponent)
                    binding.textWaitingState.visibility = View.VISIBLE
                }
            } else {
                // Configurar modo entrada
                binding.textSubmittedResponse.visibility = View.GONE
                binding.inputLayout.visibility = View.VISIBLE
                binding.textCharacterCount.visibility = View.VISIBLE
                binding.buttonSubmit.visibility = View.VISIBLE
                binding.textWaitingState.visibility = View.GONE
                
                // Verificar si es turno del usuario
                updateInputState(viewModel.isUserTurn.value ?: false)
            }
        }
    }
    
    /**
     * Actualiza el estado de los campos de entrada según sea o no el turno del usuario.
     */
    private fun updateInputState(isUserTurn: Boolean) {
        binding.inputLayout.isEnabled = isUserTurn
        binding.buttonSubmit.isEnabled = isUserTurn && 
                binding.editResponse.text.toString().isNotEmpty() &&
                binding.editResponse.text.toString().length <= MAX_CHARS
        
        if (!isUserTurn && !viewModel.isStageCompletedByUser(debateStage)) {
            binding.textWaitingState.visibility = View.VISIBLE
        } else {
            binding.textWaitingState.visibility = View.GONE
        }
    }
    
    /**
     * Configura una actualización periódica para detectar cambios en el debate.
     */
    private fun setupPeriodicUpdates() {
        val handler = Handler(Looper.getMainLooper())
        val refreshRunnable = object : Runnable {
            override fun run() {
                try {
                    // Verificar que el fragmento está adjunto y activo antes de hacer cualquier operación
                    if (isAdded && !isDetached && !isRemoving && _binding != null) {
                        viewModel.refreshDebate()
                        handler.postDelayed(this, REFRESH_INTERVAL)
                    }
                } catch (e: Exception) {
                    // Capturar cualquier excepción para evitar crashes
                    Timber.e(e, "Error en la actualización periódica")
                }
            }
        }
        
        // Iniciar las actualizaciones periódicas
        handler.postDelayed(refreshRunnable, REFRESH_INTERVAL)
        
        // Detener las actualizaciones cuando el fragmento se destruye
        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                try {
                    handler.removeCallbacks(refreshRunnable)
                } catch (e: Exception) {
                    // Capturar cualquier excepción para evitar crashes
                    Timber.e(e, "Error al detener actualizaciones periódicas")
                }
            }
        })
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}