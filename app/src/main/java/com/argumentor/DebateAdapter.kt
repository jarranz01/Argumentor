package com.argumentor.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.argumentor.R
import com.argumentor.SessionManager
import com.argumentor.models.Debate

class DebateAdapter(
    private val onJoinClick: (Debate) -> Unit,
    private val showJoinButton: Boolean = true
) : RecyclerView.Adapter<DebateAdapter.DebateViewHolder>() {

    // Lista de debates para este adaptador
    private var debates: List<Debate> = emptyList()

    /**
     * Actualiza la lista de debates y notifica al adaptador
     */
    fun submitList(newList: List<Debate>) {
        debates = newList
        notifyDataSetChanged()
    }

    /**
     * Crea un nuevo ViewHolder inflando el layout para cada ítem del debate.
     *
     * @param parent Grupo de vistas padre.
     * @param viewType Tipo de vista utilizado para diferenciar layouts (no usado aquí).
     * @return Un [DebateViewHolder] con el layout inflado.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebateViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_debate, parent, false)
        return DebateViewHolder(view, onJoinClick, showJoinButton)
    }

    /**
     * Vincula los datos del debate en la posición especificada al ViewHolder.
     *
     * @param holder ViewHolder que debe mostrar los datos.
     * @param position Posición del debate en la lista.
     */
    override fun onBindViewHolder(holder: DebateViewHolder, position: Int) {
        holder.bind(debates[position])
    }

    /**
     * Devuelve el número total de elementos en la lista
     */
    override fun getItemCount(): Int = debates.size

    /**
     * ViewHolder para un debate.
     *
     * Se encarga de vincular los datos del debate a los elementos visuales del layout.
     *
     * @property onJoinClick Lambda que se invoca al hacer clic en el botón de unirse.
     * @property showJoinButton Indica si se debe mostrar el botón de unirse.
     * @param itemView Vista del ítem.
     */
    class DebateViewHolder(
        itemView: View,
        private val onJoinClick: (Debate) -> Unit,
        private val showJoinButton: Boolean
    ) : RecyclerView.ViewHolder(itemView) {

        private val textTitle: TextView = itemView.findViewById(R.id.textDebateTitle)
        private val textDescription: TextView = itemView.findViewById(R.id.textDebateDescription)
        private val textAuthor: TextView = itemView.findViewById(R.id.textDebateAuthor)
        private val textAvailablePosition: TextView = itemView.findViewById(R.id.textAvailablePosition)
        private val buttonJoin: Button = itemView.findViewById(R.id.buttonJoinDebate)
        private val sessionManager = SessionManager(itemView.context)

        /**
         * Vincula los datos de un debate a los elementos visuales.
         *
         * @param debate Objeto Debate que contiene la información a mostrar.
         */
        fun bind(debate: Debate) {
            textTitle.text = debate.title
            textDescription.text = debate.description
            textAuthor.text = itemView.context.getString(R.string.created_by, debate.author)

            // Determinar la posición disponible (opuesta a la del creador)
            val availablePosition = if (debate.participantFavor.isNotEmpty() && debate.participantContra.isEmpty()) {
                itemView.context.getString(R.string.position_against)
            } else if (debate.participantFavor.isEmpty() && debate.participantContra.isNotEmpty()) {
                itemView.context.getString(R.string.position_favor)
            } else {
                // Ambas posiciones están ocupadas o vacías
                null
            }

            // Mostrar posición disponible
            if (availablePosition != null) {
                textAvailablePosition.text = itemView.context.getString(R.string.position_available, availablePosition)
                textAvailablePosition.visibility = View.VISIBLE
            } else {
                textAvailablePosition.visibility = View.GONE
            }

            if (!showJoinButton) {
                // En Mis Debates: hacer clickable la tarjeta pero no mostrar botón Acceder
                buttonJoin.visibility = View.GONE
                itemView.setOnClickListener { onJoinClick(debate) }
            } else {
                // Verificar si debe mostrarse el botón de unirse
                buttonJoin.visibility = View.VISIBLE

                // Comprobar si el usuario actual es el autor, comparando con el participantFavor
                // ya que el debate siempre tiene al autor como participante a favor
                val currentUserId = sessionManager.getUserId()

                // Si ambas posiciones están llenas, no se puede unir
                val isFull = debate.participantFavor.isNotEmpty() && debate.participantContra.isNotEmpty()

                // Verificar si el usuario ya está participando
                val isParticipating = currentUserId != null &&
                        (debate.participantFavor == currentUserId ||
                                debate.participantContra == currentUserId)

                when {
                    isParticipating -> {
                        // El usuario ya está participando en este debate
                        buttonJoin.isEnabled = false
                        buttonJoin.text = itemView.context.getString(R.string.user_already_in_debate)
                        buttonJoin.backgroundTintList = ContextCompat.getColorStateList(
                            itemView.context, 
                            android.R.color.holo_blue_light
                        )
                    }
                    isFull -> {
                        // El debate ya tiene ambos participantes
                        buttonJoin.isEnabled = false
                        buttonJoin.text = itemView.context.getString(R.string.debate_full)
                        buttonJoin.backgroundTintList = ContextCompat.getColorStateList(
                            itemView.context, 
                            android.R.color.darker_gray
                        )
                    }
                    availablePosition != null -> {
                        // El usuario puede unirse al debate en la posición disponible
                        buttonJoin.isEnabled = true
                        val joinText = itemView.context.getString(R.string.join_position, availablePosition)
                        buttonJoin.text = joinText
                        
                        // Cambiar color según la posición disponible
                        val colorResId = if (availablePosition == itemView.context.getString(R.string.position_favor)) {
                            android.R.color.holo_green_light // Verde para "a favor"
                        } else {
                            android.R.color.holo_red_light // Rojo para "en contra"
                        }
                        buttonJoin.backgroundTintList = ContextCompat.getColorStateList(itemView.context, colorResId)
                        
                        buttonJoin.setOnClickListener {
                            onJoinClick(debate)
                        }
                    }
                    else -> {
                        // Estado desconocido o inválido
                        buttonJoin.isEnabled = false
                        buttonJoin.text = itemView.context.getString(R.string.join_debate)
                    }
                }
            }
        }
    }
}