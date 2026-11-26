package com.accurateaccounts.app.ui.common;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.accurateaccounts.app.R;
import com.accurateaccounts.app.api.ApiClient;
import com.accurateaccounts.app.models.Feedback;
import com.accurateaccounts.app.models.FeedbackSubmitRequest;
import com.accurateaccounts.app.models.GuestMessage;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FeedbackDialogHelper {

    public static void showClientFeedbacksPopup(Context context) {
        if (context == null) return;

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_scrollable_list, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.92);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ImageButton btnBack = view.findViewById(R.id.btnDialogBack);
        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        TextView tvSubtitle = view.findViewById(R.id.tvDialogSubtitle);
        RecyclerView rvList = view.findViewById(R.id.rvDialogList);
        ProgressBar pbLoading = view.findViewById(R.id.pbDialogLoading);
        TextView tvEmpty = view.findViewById(R.id.tvDialogEmpty);

        tvTitle.setText("Client Feedbacks");
        tvSubtitle.setText("Loading feedbacks...");
        tvEmpty.setText("No client feedbacks submitted yet.");

        btnBack.setOnClickListener(v -> dialog.dismiss());

        List<Feedback> feedbackList = new ArrayList<>();
        FeedbackAdapter adapter = new FeedbackAdapter(feedbackList);
        rvList.setLayoutManager(new LinearLayoutManager(context));
        rvList.setAdapter(adapter);

        pbLoading.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        ApiClient.getService().getAllFeedbacks().enqueue(new Callback<List<Feedback>>() {
            @Override
            public void onResponse(Call<List<Feedback>> call, Response<List<Feedback>> response) {
                pbLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    feedbackList.clear();
                    feedbackList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    tvSubtitle.setText(feedbackList.size() + " feedback(s) • Scroll to view all");
                    tvEmpty.setVisibility(feedbackList.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    tvSubtitle.setText("Failed to load feedbacks");
                    tvEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<Feedback>> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                tvSubtitle.setText("Connection error");
                tvEmpty.setText("Unable to load feedbacks. Please try again.");
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });

        dialog.show();
    }

    public static void showGuestInquiriesPopup(Context context) {
        if (context == null) return;

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_scrollable_list, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.92);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ImageButton btnBack = view.findViewById(R.id.btnDialogBack);
        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        TextView tvSubtitle = view.findViewById(R.id.tvDialogSubtitle);
        RecyclerView rvList = view.findViewById(R.id.rvDialogList);
        ProgressBar pbLoading = view.findViewById(R.id.pbDialogLoading);
        TextView tvEmpty = view.findViewById(R.id.tvDialogEmpty);

        tvTitle.setText("Guest Inquiries");
        tvSubtitle.setText("Loading inquiries...");
        tvEmpty.setText("No guest inquiries found.");

        btnBack.setOnClickListener(v -> dialog.dismiss());

        List<GuestMessage> guestList = new ArrayList<>();
        GuestMessageAdapter adapter = new GuestMessageAdapter(guestList);
        rvList.setLayoutManager(new LinearLayoutManager(context));
        rvList.setAdapter(adapter);

        pbLoading.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        ApiClient.getService().getAllGuestMessages().enqueue(new Callback<List<GuestMessage>>() {
            @Override
            public void onResponse(Call<List<GuestMessage>> call, Response<List<GuestMessage>> response) {
                pbLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    guestList.clear();
                    guestList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    tvSubtitle.setText(guestList.size() + " inquiry(s) • Scroll to view all");
                    tvEmpty.setVisibility(guestList.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    tvSubtitle.setText("Failed to load inquiries");
                    tvEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<GuestMessage>> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                tvSubtitle.setText("Connection error");
                tvEmpty.setText("Unable to load inquiries. Please try again.");
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });

        dialog.show();
    }

    public static void showWriteFeedbackPopup(Context context, int clientId, Runnable onSubmitted) {
        if (context == null) return;

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_write_feedback, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.92);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ImageButton btnBack = view.findViewById(R.id.btnWriteFeedbackBack);
        TextInputLayout tilMessage = view.findViewById(R.id.tilFeedbackMessage);
        TextInputEditText etMessage = view.findViewById(R.id.etFeedbackMessage);
        ProgressBar pbSend = view.findViewById(R.id.pbSendFeedback);
        MaterialButton btnSubmit = view.findViewById(R.id.btnSubmitFeedback);

        btnBack.setOnClickListener(v -> dialog.dismiss());

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilMessage.setError(null);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSubmit.setOnClickListener(v -> {
            String message = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
            if (message.isEmpty()) {
                tilMessage.setError("Please write your feedback before sending");
                return;
            }

            int effectiveClientId = clientId > 0 ? clientId : 1;
            pbSend.setVisibility(View.VISIBLE);
            btnSubmit.setEnabled(false);

            FeedbackSubmitRequest request = new FeedbackSubmitRequest(effectiveClientId, message);
            ApiClient.getService().submitFeedback(request).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    pbSend.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);

                    if (response.isSuccessful()) {
                        Toast.makeText(context, "Feedback sent to Admin & Employees!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        if (onSubmitted != null) onSubmitted.run();
                    } else {
                        tilMessage.setError("Failed to submit feedback. Please try again.");
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    pbSend.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    tilMessage.setError("Network error. Please check your connection.");
                }
            });
        });

        dialog.show();
    }
}

