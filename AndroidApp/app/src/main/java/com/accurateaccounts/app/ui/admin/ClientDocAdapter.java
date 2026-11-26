package com.accurateaccounts.app.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.accurateaccounts.app.R;
import com.accurateaccounts.app.models.User;

import java.util.ArrayList;
import java.util.List;

public class ClientDocAdapter extends RecyclerView.Adapter<ClientDocAdapter.ClientViewHolder> {

    public interface OnClientClickListener {
        void onClientClick(User client);
    }

    private final List<User> fullList = new ArrayList<>();
    private final List<User> displayList = new ArrayList<>();
    private final OnClientClickListener listener;

    public ClientDocAdapter(List<User> clients, OnClientClickListener listener) {
        this.listener = listener;
        setClients(clients);
    }

    public void setClients(List<User> clients) {
        fullList.clear();
        displayList.clear();
        if (clients != null) {
            fullList.addAll(clients);
            displayList.addAll(clients);
        }
        notifyDataSetChanged();
    }

    public void filter(String query) {
        displayList.clear();
        if (query == null || query.trim().isEmpty()) {
            displayList.addAll(fullList);
        } else {
            String lower = query.toLowerCase().trim();
            for (User u : fullList) {
                String comp = u.getCompanyName() != null ? u.getCompanyName().toLowerCase() : "";
                String name = u.getDisplayName() != null ? u.getDisplayName().toLowerCase() : "";
                String user = u.getUsername() != null ? u.getUsername().toLowerCase() : "";
                String contact = u.getContactPerson() != null ? u.getContactPerson().toLowerCase() : "";
                if (comp.contains(lower) || name.contains(lower) || user.contains(lower) || contact.contains(lower)) {
                    displayList.add(u);
                }
            }
        }
        notifyDataSetChanged();
    }

    public int getFilteredCount() {
        return displayList.size();
    }

    @NonNull
    @Override
    public ClientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_doc_client, parent, false);
        return new ClientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClientViewHolder holder, int position) {
        User client = displayList.get(position);
        holder.bind(client, listener);
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    static class ClientViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvInitial, tvCompanyName, tvContactPerson, tvContactInfo;

        public ClientViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitial = itemView.findViewById(R.id.tvDocClientInitial);
            tvCompanyName = itemView.findViewById(R.id.tvDocClientCompanyName);
            tvContactPerson = itemView.findViewById(R.id.tvDocClientContactPerson);
            tvContactInfo = itemView.findViewById(R.id.tvDocClientContactInfo);
        }

        public void bind(User client, OnClientClickListener listener) {
            String displayName = client.getDisplayName();
            tvCompanyName.setText(displayName);

            if (displayName != null && !displayName.trim().isEmpty()) {
                tvInitial.setText(displayName.trim().substring(0, 1).toUpperCase());
            } else {
                tvInitial.setText("C");
            }

            String contact = client.getContactPerson();
            if (contact != null && !contact.trim().isEmpty()) {
                tvContactPerson.setText("Contact: " + contact.trim());
            } else {
                tvContactPerson.setText("Client Account (@" + client.getUsername() + ")");
            }

            StringBuilder sb = new StringBuilder();
            if (client.getEmail() != null && !client.getEmail().trim().isEmpty()) {
                sb.append(client.getEmail().trim());
            }
            if (client.getPhone() != null && !client.getPhone().trim().isEmpty()) {
                if (sb.length() > 0) sb.append(" • ");
                sb.append(client.getPhone().trim());
            }
            if (sb.length() == 0) {
                sb.append("No contact details registered");
            }
            tvContactInfo.setText(sb.toString());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClientClick(client);
                }
            });
        }
    }
}
