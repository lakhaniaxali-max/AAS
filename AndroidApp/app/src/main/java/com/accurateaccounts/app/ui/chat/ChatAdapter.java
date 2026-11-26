package com.accurateaccounts.app.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.accurateaccounts.app.R;
import com.accurateaccounts.app.models.ChatMessage;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_OUTGOING = 1;
    private static final int VIEW_TYPE_INCOMING = 2;

    private final List<ChatMessage> messages;
    private final int currentUserId;

    public ChatAdapter(List<ChatMessage> messages, int currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        if (message.getSenderId() == currentUserId) {
            return VIEW_TYPE_OUTGOING;
        } else {
            return VIEW_TYPE_INCOMING;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_OUTGOING) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_outgoing, parent, false);
            return new OutgoingViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_incoming, parent, false);
            return new IncomingViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);

        if (holder instanceof OutgoingViewHolder) {
            ((OutgoingViewHolder) holder).tvMessage.setText(msg.getMessage());
            ((OutgoingViewHolder) holder).tvTime.setText(msg.getCreatedAt() != null ? msg.getCreatedAt().substring(11, 16) : "");
        } else if (holder instanceof IncomingViewHolder) {
            ((IncomingViewHolder) holder).tvMessage.setText(msg.getMessage());
            String sender = msg.getFirstName() != null ? msg.getFirstName() : "User " + msg.getSenderId();
            ((IncomingViewHolder) holder).tvSenderName.setText(sender);
            ((IncomingViewHolder) holder).tvTime.setText(msg.getCreatedAt() != null ? msg.getCreatedAt().substring(11, 16) : "");
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class OutgoingViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;

        OutgoingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }

    static class IncomingViewHolder extends RecyclerView.ViewHolder {
        TextView tvSenderName, tvMessage, tvTime;

        IncomingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}

