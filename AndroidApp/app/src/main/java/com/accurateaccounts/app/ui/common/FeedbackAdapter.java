package com.accurateaccounts.app.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.accurateaccounts.app.R;
import com.accurateaccounts.app.models.Feedback;

import java.util.List;

public class FeedbackAdapter extends RecyclerView.Adapter<FeedbackAdapter.ViewHolder> {

    private final List<Feedback> items;

    public FeedbackAdapter(List<Feedback> items) {
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
        Feedback fb = items.get(position);
        String company = fb.getCompanyName() != null && !fb.getCompanyName().isEmpty() ? fb.getCompanyName() : "Client";
        String contact = fb.getContactPerson() != null && !fb.getContactPerson().isEmpty() ? fb.getContactPerson() : "Verified Client";

        holder.tvAvatar.setText(company.substring(0, 1).toUpperCase());
        holder.tvTitle.setText(company);
        holder.tvSubtitle.setText(contact);
        holder.tvMessage.setText(fb.getMessage() != null ? fb.getMessage() : "");

        String date = fb.getCreatedAt();
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

