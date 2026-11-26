package com.accurateaccounts.app.ui.client;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.accurateaccounts.app.R;

public class ClientBannerAdapter extends RecyclerView.Adapter<ClientBannerAdapter.SlideViewHolder> {

    private final int[] imageResIds;

    public ClientBannerAdapter(int[] imageResIds) {
        this.imageResIds = imageResIds;
    }

    @NonNull
    @Override
    public SlideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner_slide, parent, false);
        return new SlideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SlideViewHolder holder, int position) {
        holder.ivSlide.setImageResource(imageResIds[position]);
    }

    @Override
    public int getItemCount() {
        return imageResIds.length;
    }

    static class SlideViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSlide;

        SlideViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSlide = itemView.findViewById(R.id.ivBannerSlide);
        }
    }
}

