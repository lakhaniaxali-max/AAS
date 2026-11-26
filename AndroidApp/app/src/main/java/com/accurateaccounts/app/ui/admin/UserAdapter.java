package com.accurateaccounts.app.ui.admin;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.accurateaccounts.app.R;
import com.accurateaccounts.app.models.User;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final List<User> userList;

    public UserAdapter(List<User> userList) {
        this.userList = userList;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvInitial, tvDisplayName, tvUsername, tvRoleBadge, tvEmail, tvPhone;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitial = itemView.findViewById(R.id.tvUserInitial);
            tvDisplayName = itemView.findViewById(R.id.tvUserDisplayName);
            tvUsername = itemView.findViewById(R.id.tvUserUsername);
            tvRoleBadge = itemView.findViewById(R.id.tvUserRoleBadge);
            tvEmail = itemView.findViewById(R.id.tvUserEmail);
            tvPhone = itemView.findViewById(R.id.tvUserPhone);
        }

        public void bind(User user) {
            String displayName = user.getDisplayName();
            tvDisplayName.setText(displayName);
            tvUsername.setText("@" + (user.getUsername() != null ? user.getUsername() : "user"));

            if (displayName != null && !displayName.isEmpty()) {
                tvInitial.setText(displayName.substring(0, 1).toUpperCase());
            } else {
                tvInitial.setText("U");
            }

            String role = user.getRole() != null ? user.getRole().toLowerCase() : "user";
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setCornerRadius(16);

            if ("admin".equals(role)) {
                tvRoleBadge.setText("👑 Admin");
                tvRoleBadge.setTextColor(Color.parseColor("#2563EB"));
                badgeBg.setColor(Color.parseColor("#EFF6FF"));
            } else if ("employee".equals(role)) {
                tvRoleBadge.setText("💼 Staff");
                tvRoleBadge.setTextColor(Color.parseColor("#059669"));
                badgeBg.setColor(Color.parseColor("#ECFDF5"));
            } else if ("client".equals(role)) {
                tvRoleBadge.setText("🏢 Client");
                tvRoleBadge.setTextColor(Color.parseColor("#D97706"));
                badgeBg.setColor(Color.parseColor("#FFFBEB"));
            } else {
                tvRoleBadge.setText(role.toUpperCase());
                tvRoleBadge.setTextColor(Color.parseColor("#64748B"));
                badgeBg.setColor(Color.parseColor("#F1F5F9"));
            }
            tvRoleBadge.setBackground(badgeBg);

            tvEmail.setText(user.getEmail() != null && !user.getEmail().isEmpty() ? user.getEmail() : "No email registered");
            tvPhone.setText(user.getPhone() != null && !user.getPhone().isEmpty() ? user.getPhone() : "No phone");
        }
    }
}

