package com.argumentor.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.argumentor.R
import com.argumentor.models.DebateEntry
import com.argumentor.models.DebatePosition
import com.argumentor.models.DebateStage
import com.argumentor.viewmodels.DebateViewModel
import com.google.android.material.card.MaterialCardView

/**
 * Fragmento que muestra el historial de argumentos en un debate.
 * Este segundo fragmento complementa al DebateStageFragment existente.
 */
class ArgumentHistoryFragment : Fragment() {

    private var recyclerView: RecyclerView? = null
    private val viewModel: DebateViewModel by activityViewModels()
    private lateinit var debateId: String

    companion object {
        private const val ARG_DEBATE_ID = "debate_id"

        fun newInstance(debateId: String): ArgumentHistoryFragment {
            return ArgumentHistoryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DEBATE_ID, debateId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            debateId = it.getString(ARG_DEBATE_ID, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_argument_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewHistory)
        recyclerView?.layoutManager = LinearLayoutManager(context)

        // Observar los cambios en los argumentos del debate
        viewModel.debateEntries.observe(viewLifecycleOwner) { entriesMap ->
            val allEntries = mutableListOf<DebateEntry>()
            
            // Reunir todas las entradas en orden cronológico
            DebateStage.values().forEach { stage ->
                entriesMap[stage]?.values?.forEach { entry ->
                    allEntries.add(entry)
                }
            }
            
            // Ordenar por timestamp
            allEntries.sortBy { it.timestamp }
            
            // Actualizar el adaptador
            recyclerView?.adapter = ArgumentHistoryAdapter(allEntries)
        }
    }

    /**
     * Adaptador para mostrar los argumentos en un RecyclerView.
     */
    private inner class ArgumentHistoryAdapter(private val entries: List<DebateEntry>) : 
            RecyclerView.Adapter<ArgumentHistoryAdapter.ViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_argument_history, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val entry = entries[position]
            
            // Configurar el título con la etapa y posición
            val stageText = when (entry.stage) {
                DebateStage.INTRODUCCION -> getString(R.string.stage_introduction)
                DebateStage.REFUTACION1 -> getString(R.string.stage_refutation1)
                DebateStage.REFUTACION2 -> getString(R.string.stage_refutation2)
                DebateStage.CONCLUSION -> getString(R.string.stage_conclusion)
            }
            
            val positionText = when (entry.position) {
                DebatePosition.A_FAVOR -> getString(R.string.position_favor)
                DebatePosition.EN_CONTRA -> getString(R.string.position_against)
            }
            
            holder.titleText.text = "$stageText - $positionText"
            holder.contentText.text = entry.content
            
            // Establecer color según la posición
            val colorRes = when (entry.position) {
                DebatePosition.A_FAVOR -> R.color.light_green_background
                DebatePosition.EN_CONTRA -> R.color.light_red_background
            }
            
            // Aplicar color de fondo a la tarjeta
            holder.card.setCardBackgroundColor(resources.getColor(colorRes, null))
            
            // Asegurar que el texto es oscuro para mejor legibilidad
            holder.titleText.setTextColor(resources.getColor(R.color.colorText, null))
            holder.contentText.setTextColor(resources.getColor(R.color.colorText, null))
        }
        
        override fun getItemCount() = entries.size
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val titleText: TextView = view.findViewById(R.id.textArgumentTitle)
            val contentText: TextView = view.findViewById(R.id.textArgumentContent)
            val card: MaterialCardView = view.findViewById(R.id.cardArgument)
        }
    }
} 