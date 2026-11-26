package com.accurateaccounts.app.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.accurateaccounts.app.R;
import com.accurateaccounts.app.models.Announcement;

import java.util.List;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.ViewHolder> {

    private final List<Announcement> announcements;

    public AnnouncementAdapter(List<Announcement> announcements) {
        this.announcements = announcements;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_announcement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Announcement item = announcements.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvMessage.setText(item.getMessage());

        String dateStr = item.getAnnouncementDate();
        if (dateStr == null || dateStr.trim().isEmpty()) {
            dateStr = item.getCreatedAt();
        }
        if (dateStr != null && dateStr.length() >= 10) {
            holder.tvDate.setText(dateStr.substring(0, 10));
        } else {
            holder.tvDate.setText("Today");
        }

        String author = item.getCreatedBy();
        if (author == null || author.trim().isEmpty()) {
            String f = item.getFirstName();
            String l = item.getLastName();
            if (f != null && !f.trim().isEmpty()) {
                author = f + (l != null ? " " + l : "");
            } else {
                author = "Admin";
            }
        }
        holder.tvAuthor.setText("📢 Announced by: " + author);
    }

    @Override
    public int getItemCount() {
        return announcements != null ? announcements.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvDate, tvAuthor;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvAnnouncementTitle);
            tvMessage = itemView.findViewById(R.id.tvAnnouncementMessage);
            tvDate = itemView.findViewById(R.id.tvAnnouncementDate);
            tvAuthor = itemView.findViewById(R.id.tvAnnouncementAuthor);
        }
    }
}

