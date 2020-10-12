package com.adroitandroid.p2pchat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.adroitandroid.p2pchat.databinding.RowMessagesBinding
import java.util.*

/**
 * Created by pv on 22/06/17.
 */
internal class ChatAdapter : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {
    private val messages: MutableList<String> = ArrayList()
    private val messageSenders: MutableList<String> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatAdapter.ViewHolder {
        val binding = RowMessagesBinding.inflate(
                LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = with(holder.binding) {
        val messageSender = messageSenders[position]
        sender = messageSender
        message = messageSenders[position]
        userIsSender = "you".equals(sender, ignoreCase = true)
        executePendingBindings()
    }

    override fun getItemCount(): Int {
        return messageSenders.size
    }

    fun addMessage(message: String, senderName: String) {
        messageSenders.add(message)
        messageSenders.add(senderName)
        notifyItemInserted(messages.size - 1)
    }

    internal inner class ViewHolder(val binding: RowMessagesBinding) : RecyclerView.ViewHolder(binding.root)
}