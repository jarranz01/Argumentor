package com.argumentor.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.argumentor.R
import com.argumentor.databinding.FragmentDebateStageBinding
import com.argumentor.models.DebatePosition
import com.argumentor.models.DebateStage
import com.argumentor.viewmodels.DebateViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar

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
            debateStage = it.getSerializable(ARG_STAGE) as DebateStage
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDebateStageBinding.inflate(inflater, container, false)
        
        binding.viewModel = viewModel
        binding.stage = debateStage
        binding.lifecycleOwner = viewLifecycleOwner
        
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupStageInfo()
        setupOpponentArgument()
        setupResponseArea()
        checkCompletionStatus()
        
        // Observar cambios que puedan afectar a la visualización
        viewModel.debateEntries.observe(viewLifecycleOwner) { entries ->
            setupOpponentArgument()
            checkCompletionStatus()
        }
        
        viewModel.isUserTurn.observe(viewLifecycleOwner) { isUserTurn ->
            updateInputState(isUserTurn)
        }
    }
    
    /**
     * Configura la información sobre la etapa actual y posición.
     */
    private fun setupStageInfo() {
        // Establecer el título según la etapa
        val stageTitle = when (debateStage) {
            DebateStage.INTRODUCCION -> getString(R.string.stage_introduction)
            DebateStage.REFUTACION1 -> getString(R.string.stage_refutation1)
            DebateStage.REFUTACION2 -> getString(R.string.stage_refutation2)
            DebateStage.CONCLUSION -> getString(R.string.stage_conclusion)
        }
        binding.textStage.text = stageTitle
        
        // Establecer instrucciones
        binding.textInstructions.text = viewModel.getInstructionForStage(debateStage)
        
        // Configurar el chip de posición
        viewModel.debate.observe(viewLifecycleOwner) { debate ->
            val userPosition = if (viewModel.debate.value?.participantFavor == "Usuario1") {
                DebatePosition.A_FAVOR
            } else {
                DebatePosition.EN_CONTRA
            }
            
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
            binding.chipPosition.chipBackgroundColor = 
                ContextCompat.getColorStateList(requireContext(), colorRes)
        }
    }
    
    /**
     * Configura la visualización del argumento del oponente, si corresponde.
     */
    private fun setupOpponentArgument() {
        // Solo aplica para refutaciones y conclusión
        if (debateStage == DebateStage.INTRODUCCION) {
            binding.cardOpponentArgument.visibility = View.GONE
            return
        }
        
        val opponentEntry = viewModel.getOpponentEntryToRefute(debateStage)
        
        if (opponentEntry != null) {
            binding.textOpponentArgument.text = opponentEntry.content
            binding.cardOpponentArgument.visibility = View.VISIBLE
        } else {
            binding.cardOpponentArgument.visibility = View.GONE
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
                    Snackbar.make(binding.root, R.string.debate_stage_complete, Snackbar.LENGTH_SHORT).show()
                    checkCompletionStatus()
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
            
            // Ocultar el campo de entrada
            binding.inputLayout.visibility = View.GONE
            binding.buttonSubmit.visibility = View.GONE
            binding.textCharacterCount.visibility = View.GONE
            
            // Mostrar estado de espera si es necesario
            val opponentHasResponded = when (debateStage) {
                DebateStage.INTRODUCCION -> viewModel.getOpponentEntryToRefute(DebateStage.REFUTACION1) != null
                DebateStage.REFUTACION1 -> viewModel.getOpponentEntryToRefute(DebateStage.REFUTACION2) != null
                DebateStage.REFUTACION2 -> viewModel.getOpponentEntryToRefute(DebateStage.CONCLUSION) != null
                DebateStage.CONCLUSION -> true // No hay siguiente paso después de la conclusión
            }
            
            if (!opponentHasResponded && debateStage != DebateStage.CONCLUSION) {
                binding.textWaitingState.visibility = View.VISIBLE
            } else {
                binding.textWaitingState.visibility = View.GONE
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
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}