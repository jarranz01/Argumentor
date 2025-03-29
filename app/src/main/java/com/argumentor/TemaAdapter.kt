package com.argumentor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.argumentor.models.Tema
import com.argumentor.R

class TemaAdapter(
    private val temas: List<Tema>,
    private val onOpinionSelected: (String, String) -> Unit
) : RecyclerView.Adapter<TemaAdapter.TemaViewHolder>() {

    private var lastPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tema, parent, false)
        return TemaViewHolder(view)
    }

    override fun onBindViewHolder(holder: TemaViewHolder, position: Int) {
        val tema = temas[position]
        holder.bind(tema, onOpinionSelected)

        // Animación para cada item
        setAnimation(holder.itemView, position)
    }

    private fun setAnimation(viewToAnimate: View, position: Int) {
        if (position > lastPosition) {
            val animation = AnimationUtils.loadAnimation(viewToAnimate.context, R.anim.item_animation)
            viewToAnimate.startAnimation(animation)
            lastPosition = position
        }
    }

    override fun getItemCount(): Int = temas.size

    class TemaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textTema: TextView = itemView.findViewById(R.id.textTema)
        private val radioGroup: RadioGroup = itemView.findViewById(R.id.radioGroup)
        private val btnInfo: ImageButton = itemView.findViewById(R.id.btnInfo)
        private val context = itemView.context

        fun bind(tema: Tema, onOpinionSelected: (String, String) -> Unit) {
            textTema.text = tema.nombre

            // Obtener las cadenas de recursos
            val favorStr = context.getString(R.string.opinion_favor)
            val contraStr = context.getString(R.string.opinion_against)
            val indiferenteStr = context.getString(R.string.opinion_neutral)

            // Establecer la opción seleccionada
            when (tema.opinionSeleccionada) {
                favorStr -> (itemView.findViewById<RadioButton>(R.id.radioFavor)).isChecked = true
                contraStr -> (itemView.findViewById<RadioButton>(R.id.radioContra)).isChecked = true
                indiferenteStr -> (itemView.findViewById<RadioButton>(R.id.radioIndiferente)).isChecked = true
            }

            // Escuchar cambios en la selección
            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                val opinion = when (checkedId) {
                    R.id.radioFavor -> favorStr
                    R.id.radioContra -> contraStr
                    R.id.radioIndiferente -> indiferenteStr
                    else -> indiferenteStr
                }
                onOpinionSelected(tema.nombre, opinion)
            }

            // Configurar el botón de información
            btnInfo.setOnClickListener {
                MaterialAlertDialogBuilder(itemView.context)
                    .setTitle(tema.nombre)
                    .setMessage(tema.descripcion)
                    .setPositiveButton(R.string.understood, null)
                    .show()
            }
        }
    }
}