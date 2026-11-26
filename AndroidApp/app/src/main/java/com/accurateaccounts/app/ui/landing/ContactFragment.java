package com.accurateaccounts.app.ui.landing;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.accurateaccounts.app.R;
import com.accurateaccounts.app.api.ApiClient;
import com.accurateaccounts.app.models.GuestMessageRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ContactFragment extends Fragment {

    private TextInputLayout tilName, tilEmail, tilMessage;
    private TextInputEditText etName, etEmail, etMessage;
    private MaterialButton btnSubmit;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact, container, false);

        tilName = view.findViewById(R.id.tilContactName);
        tilEmail = view.findViewById(R.id.tilContactEmail);
        tilMessage = view.findViewById(R.id.tilContactMessage);
        etName = view.findViewById(R.id.etContactName);
        etEmail = view.findViewById(R.id.etContactEmail);
        etMessage = view.findViewById(R.id.etContactMessage);
        btnSubmit = view.findViewById(R.id.btnSubmitMessage);
        progressBar = view.findViewById(R.id.pbContactLoading);

        etName.addTextChangedListener(clearErrorWatcher(tilName));
        etEmail.addTextChangedListener(clearErrorWatcher(tilEmail));
        etMessage.addTextChangedListener(clearErrorWatcher(tilMessage));

        btnSubmit.setOnClickListener(v -> submitMessage());

        return view;
    }

    private TextWatcher clearErrorWatcher(TextInputLayout til) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (til != null) til.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        };
    }

    private void submitMessage() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String message = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";

        boolean hasError = false;
        if (name.isEmpty()) {
            tilName.setError("Please enter your name");
            hasError = true;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Please enter a valid email address");
            hasError = true;
        }
        if (message.isEmpty()) {
            tilMessage.setError("Please enter your message or inquiry");
            hasError = true;
        }
        if (hasError) return;

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        GuestMessageRequest request = new GuestMessageRequest(name, email, message);
        ApiClient.getService().sendGuestMessage(request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressBar.setVisibility(View.GONE);
                btnSubmit.setEnabled(true);
                if (!isAdded()) return;

                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Message sent! Our Admin & Employee team has received your inquiry.", Toast.LENGTH_LONG).show();
                    etName.setText("");
                    etEmail.setText("");
                    etMessage.setText("");
                    tilName.setError(null);
                    tilEmail.setError(null);
                    tilMessage.setError(null);
                    etName.clearFocus();
                    etEmail.clearFocus();
                    etMessage.clearFocus();
                } else {
                    tilMessage.setError("Could not send message. Please try again.");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnSubmit.setEnabled(true);
                if (!isAdded()) return;
                tilMessage.setError("Unable to reach server. Please check connection.");
            }
        });
    }
}
