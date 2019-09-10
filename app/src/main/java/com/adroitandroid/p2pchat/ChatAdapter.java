package com.adroitandroid.p2pchat;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.adroitandroid.p2pchat.databinding.RowMessagesBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pv on 22/06/17.
 */

class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageVH> {
    private final List<String> mMessages;
    private final List<String> mMessageSender;

    ChatAdapter() {
        mMessages = new ArrayList<>();
        mMessageSender = new ArrayList<>();
    }

    @Override
    public MessageVH onCreateViewHolder(ViewGroup parent, int viewType) {
        RowMessagesBinding binding = RowMessagesBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new MessageVH(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageVH holder, int position) {
        String sender = mMessageSender.get(position);
        holder.binding.setSender(sender);
        holder.binding.setMessage(mMessages.get(position));
        holder.binding.setUserIsSender("you".equalsIgnoreCase(sender));
        holder.binding.executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    void addMessage(String message, String senderName) {
        mMessages.add(message);
        mMessageSender.add(senderName);
        notifyItemInserted(mMessages.size() - 1);
    }

    class MessageVH extends RecyclerView.ViewHolder {

        private final RowMessagesBinding binding;

        MessageVH(@NonNull RowMessagesBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
