package com.argumentor.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.argumentor.R
import com.argumentor.models.Debate

/**
 * Adapter para mostrar una lista de debates en un RecyclerView.
 *
 * Este adapter extiende de ListAdapter para gestionar de forma optimizada las actualizaciones
 * de la lista utilizando DebateDiffCallback. Se encarga de vincular cada objeto Debate
 * con su correspondiente ViewHolder.
 *
 * @param onJoinClick Lambda que se invoca cuando se hace clic en el botón para unirse a un debate.
 */
class DebateAdapter(private val onJoinClick: (Debate) -> Unit) :
    ListAdapter<Debate, DebateAdapter.DebateViewHolder>(DebateDiffCallback()) {

    /**
     * Crea un nuevo ViewHolder inflando el layout para cada ítem del debate.
     *
     * @param parent Grupo de vistas padre.
     * @param viewType Tipo de vista utilizado para diferenciar layouts (no usado aquí).
     * @return Un [DebateViewHolder] con el layout inflado.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebateViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_debate, parent, false)
        return DebateViewHolder(view, onJoinClick)
    }

    /**
     * Vincula los datos del debate en la posición especificada al ViewHolder.
     *
     * @param holder ViewHolder que debe mostrar los datos.
     * @param position Posición del debate en la lista.
     */
    override fun onBindViewHolder(holder: DebateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder para un debate.
     *
     * Se encarga de vincular los datos del debate a los elementos visuales del layout.
     *
     * @property onJoinClick Lambda que se invoca al hacer clic en el botón de unirse.
     *
     * @param itemView Vista del ítem.
     */
    class DebateViewHolder(itemView: View, private val onJoinClick: (Debate) -> Unit) :
        RecyclerView.ViewHolder(itemView) {

        private val textTitle: TextView = itemView.findViewById(R.id.textDebateTitle)
        private val textDescription: TextView = itemView.findViewById(R.id.textDebateDescription)
        private val textAuthor: TextView = itemView.findViewById(R.id.textDebateAuthor)
        private val buttonJoin: Button = itemView.findViewById(R.id.buttonJoinDebate)

        /**
         * Vincula los datos de un debate a los elementos visuales.
         *
         * @param debate Objeto Debate que contiene la información a mostrar.
         */
        fun bind(debate: Debate) {
            textTitle.text = debate.title
            textDescription.text = debate.description
            textAuthor.text = itemView.context.getString(R.string.created_by, debate.author)

            buttonJoin.setOnClickListener {
                onJoinClick(debate)
            }
        }
    }

    /**
     * Implementación de DiffUtil.ItemCallback para optimizar la actualización de la lista de debates.
     *
     * Compara dos objetos Debate para determinar si son el mismo item o si su contenido ha cambiado.
     */
    class DebateDiffCallback : DiffUtil.ItemCallback<Debate>() {

        /**
         * Determina si dos objetos Debate representan el mismo ítem.
         *
         * @param oldItem Debate anterior.
         * @param newItem Debate nuevo.
         * @return true si ambos debates tienen el mismo ID, false en caso contrario.
         */
        override fun areItemsTheSame(oldItem: Debate, newItem: Debate): Boolean {
            return oldItem.id == newItem.id
        }

        /**
         * Determina si el contenido de dos objetos Debate es igual.
         *
         * @param oldItem Debate anterior.
         * @param newItem Debate nuevo.
         * @return true si el contenido de ambos debates es igual, false en caso contrario.
         */
        override fun areContentsTheSame(oldItem: Debate, newItem: Debate): Boolean {
            return oldItem == newItem
        }
    }
}