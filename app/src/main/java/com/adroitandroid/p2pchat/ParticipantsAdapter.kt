package com.adroitandroid.p2pchat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.adroitandroid.near.model.Host
import com.adroitandroid.p2pchat.databinding.RowParticipantsBinding

/**
 * Created by pv on 20/06/17.
 */
internal class ParticipantsAdapter(private val listener: Listener) : RecyclerView.Adapter<ParticipantsAdapter.ViewHolder>() {
    private var participants: List<Host> = ArrayList()

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            TYPE_HEADER
        } else {
            TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantsAdapter.ViewHolder {
        val binding = RowParticipantsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(viewType, binding)
    }

    override fun onBindViewHolder(holder: ParticipantsAdapter.ViewHolder, position: Int) = with(holder.binding) {
        if (TYPE_HEADER == holder.viewType) {
            nameTv.text = "Participants Found:"
        } else {
            val host = participants[position - 1]
            nameTv.text = host.name
            root.setOnClickListener { listener.sendChatRequest(participants[holder.adapterPosition - 1]) }
        }
    }

    override fun getItemCount(): Int {
        return if (participants.isEmpty()) 0 else participants.size + 1
    }

    fun setData(hosts: Set<Host>) {
        participants = ArrayList(hosts)
        notifyDataSetChanged()
    }

    internal inner class ViewHolder(val viewType: Int, val binding: RowParticipantsBinding) : RecyclerView.ViewHolder(binding.root)

    fun interface Listener {
        fun sendChatRequest(host: Host)
    }

    companion object {
        private const val TYPE_HEADER = 1
        private const val TYPE_ITEM = 2
    }
}