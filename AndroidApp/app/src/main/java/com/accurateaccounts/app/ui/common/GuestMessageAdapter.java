package com.accurateaccounts.app.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.accurateaccounts.app.R;
import com.accurateaccounts.app.models.GuestMessage;

import java.util.List;

public class GuestMessageAdapter extends RecyclerView.Adapter<GuestMessageAdapter.ViewHolder> {

    private final List<GuestMessage> items;

    public GuestMessageAdapter(List<GuestMessage> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feedback, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GuestMessage msg = items.get(position);
        String name = msg.getGuestName() != null && !msg.getGuestName().isEmpty() ? msg.getGuestName() : "Guest";
        String email = msg.getGuestEmail() != null ? msg.getGuestEmail() : "";

        holder.tvAvatar.setText(name.substring(0, 1).toUpperCase());
        holder.tvTitle.setText(name);
        holder.tvSubtitle.setText(email);
        holder.tvMessage.setText(msg.getMessage() != null ? msg.getMessage() : "");

        String date = msg.getCreatedAt();
        if (date != null && date.length() >= 10) {
            holder.tvDate.setText(date.substring(0, 10));
        } else {
            holder.tvDate.setText(date != null ? date : "");
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvTitle, tvSubtitle, tvDate, tvMessage;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvItemAvatar);
            tvTitle = itemView.findViewById(R.id.tvItemTitle);
            tvSubtitle = itemView.findViewById(R.id.tvItemSubtitle);
            tvDate = itemView.findViewById(R.id.tvItemDate);
            tvMessage = itemView.findViewById(R.id.tvItemMessage);
        }
    }
}

