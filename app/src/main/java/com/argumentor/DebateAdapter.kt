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

class DebateAdapter(private val onJoinClick: (Debate) -> Unit) :
    ListAdapter<Debate, DebateAdapter.DebateViewHolder>(DebateDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebateViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_debate, parent, false)
        return DebateViewHolder(view, onJoinClick)
    }

    override fun onBindViewHolder(holder: DebateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DebateViewHolder(itemView: View, private val onJoinClick: (Debate) -> Unit) :
        RecyclerView.ViewHolder(itemView) {

        private val textTitle: TextView = itemView.findViewById(R.id.textDebateTitle)
        private val textDescription: TextView = itemView.findViewById(R.id.textDebateDescription)
        private val textAuthor: TextView = itemView.findViewById(R.id.textDebateAuthor)
        private val buttonJoin: Button = itemView.findViewById(R.id.buttonJoinDebate)

        fun bind(debate: Debate) {
            textTitle.text = debate.title
            textDescription.text = debate.description
            textAuthor.text = itemView.context.getString(R.string.created_by, debate.author)

            buttonJoin.setOnClickListener {
                onJoinClick(debate)
            }
        }
    }

    class DebateDiffCallback : DiffUtil.ItemCallback<Debate>() {
        override fun areItemsTheSame(oldItem: Debate, newItem: Debate): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Debate, newItem: Debate): Boolean {
            return oldItem == newItem
        }
    }
}