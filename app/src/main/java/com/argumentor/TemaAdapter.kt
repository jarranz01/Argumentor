package com.argumentor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.argumentor.models.Tema
import com.argumentor.R

class TemaAdapter(
    private val temas: List<Tema>,
    private val onOpinionSelected: (String, String) -> Unit // Callback para actualizar el jugador
) : RecyclerView.Adapter<TemaAdapter.TemaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tema, parent, false)
        return TemaViewHolder(view)
    }

    override fun onBindViewHolder(holder: TemaViewHolder, position: Int) {
        val tema = temas[position]
        holder.bind(tema, onOpinionSelected)
    }

    override fun getItemCount(): Int = temas.size

    class TemaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textTema: TextView = itemView.findViewById(R.id.textTema)
        private val radioGroup: RadioGroup = itemView.findViewById(R.id.radioGroup)

        fun bind(tema: Tema, onOpinionSelected: (String, String) -> Unit) {
            textTema.text = tema.nombre

            when (tema.opinionSeleccionada) {
                "A favor" -> (itemView.findViewById<RadioButton>(R.id.radioFavor)).isChecked = true
                "En contra" -> (itemView.findViewById<RadioButton>(R.id.radioContra)).isChecked = true
                "Indiferente" -> (itemView.findViewById<RadioButton>(R.id.radioIndiferente)).isChecked = true
            }

            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                val opinion = when (checkedId) {
                    R.id.radioFavor -> "A favor"
                    R.id.radioContra -> "En contra"
                    R.id.radioIndiferente -> "Indiferente"
                    else -> "Indiferente"
                }
                onOpinionSelected(tema.nombre, opinion)
            }
        }
    }
}
