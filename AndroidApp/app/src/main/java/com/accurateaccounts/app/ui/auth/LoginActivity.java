package com.accurateaccounts.app.ui.auth;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.accurateaccounts.app.R;
import com.accurateaccounts.app.api.ApiClient;
import com.accurateaccounts.app.models.AuthRequest;
import com.accurateaccounts.app.models.AuthResponse;
import com.accurateaccounts.app.models.User;
import com.accurateaccounts.app.ui.main.MainActivity;
import com.accurateaccounts.app.utils.Constants;
import com.accurateaccounts.app.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilUsername, tilPassword;
    private TextInputEditText etUsername, etPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private SessionManager sessionManager;
    private TextView tvCurrentServerUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        // Auto-login check if session exists
        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.loginToolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        tilUsername = findViewById(R.id.tilUsername);
        tilPassword = findViewById(R.id.tilPassword);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);

        tvCurrentServerUrl = findViewById(R.id.tvCurrentServerUrl);
        View layoutServerConfig = findViewById(R.id.layoutServerConfig);

        updateServerUrlDisplay();

        if (layoutServerConfig != null) {
            layoutServerConfig.setOnClickListener(v -> showServerConfigDialog());
        }

        // Clear error as user types
        etUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilUsername.setError(null);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilPassword.setError(null);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnLogin.setOnClickListener(v -> performLogin());
    }

    private void updateServerUrlDisplay() {
        if (tvCurrentServerUrl != null) {
            String currentUrl = Constants.getBaseUrl(this);
            tvCurrentServerUrl.setText("Server: " + currentUrl);
        }
    }

    private void showServerConfigDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⚙️ Server Network Settings");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);

        TextView tvHelp = new TextView(this);
        tvHelp.setText("Select your connection mode or enter your PC's IP address (Port 3000):");
        tvHelp.setTextSize(13);
        tvHelp.setTextColor(Color.parseColor("#475569"));
        layout.addView(tvHelp);

        // Quick Preset Buttons
        LinearLayout layoutPresets = new LinearLayout(this);
        layoutPresets.setOrientation(LinearLayout.HORIZONTAL);
        layoutPresets.setPadding(0, 16, 0, 16);

        MaterialButton btnPresetCloud = new MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle);
        btnPresetCloud.setText("🌐 Cloud");
        btnPresetCloud.setTextSize(12);
        btnPresetCloud.setAllCaps(false);

        MaterialButton btnPresetEmu = new MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle);
        btnPresetEmu.setText("💻 Emulator");
        btnPresetEmu.setTextSize(12);
        btnPresetEmu.setAllCaps(false);

        MaterialButton btnPresetWifi = new MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle);
        btnPresetWifi.setText("📱 Wi-Fi");
        btnPresetWifi.setTextSize(12);
        btnPresetWifi.setAllCaps(false);

        layoutPresets.addView(btnPresetCloud);
        layoutPresets.addView(btnPresetEmu);
        layoutPresets.addView(btnPresetWifi);
        layout.addView(layoutPresets);

        EditText etUrl = new EditText(this);
        etUrl.setText(Constants.getBaseUrl(this));
        etUrl.setTextColor(Color.parseColor("#0F172A"));
        etUrl.setTextSize(14);
        layout.addView(etUrl);

        btnPresetCloud.setOnClickListener(v -> etUrl.setText(Constants.CLOUD_PUBLIC_HOST));
        btnPresetEmu.setOnClickListener(v -> etUrl.setText(Constants.EMULATOR_HOST));
        btnPresetWifi.setOnClickListener(v -> etUrl.setText(Constants.LOCAL_WIFI_HOST));

        builder.setView(layout);

        builder.setPositiveButton("Save & Connect", (dialog, which) -> {
            String newUrl = etUrl.getText().toString().trim();
            if (!newUrl.isEmpty()) {
                Constants.setBaseUrl(LoginActivity.this, newUrl);
                ApiClient.resetClient();
                updateServerUrlDisplay();
                tilPassword.setError(null);
                Toast.makeText(LoginActivity.this, "Server URL updated!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void performLogin() {
        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        boolean hasError = false;
        if (username.isEmpty()) {
            tilUsername.setError("Please enter your username");
            hasError = true;
        } else {
            tilUsername.setError(null);
        }

        if (password.isEmpty()) {
            tilPassword.setError("Please enter your password");
            hasError = true;
        } else {
            tilPassword.setError(null);
        }

        if (hasError) return;

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        AuthRequest request = new AuthRequest(username, password);
        ApiClient.getService().login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);

                if (response.isSuccessful() && response.body() != null && response.body().getUser() != null) {
                    User user = response.body().getUser();
                    String fName = user.getFirstName() != null && !user.getFirstName().isEmpty()
                            ? user.getFirstName() : user.getCompanyName();
                    String lName = user.getLastName() != null && !user.getLastName().isEmpty()
                            ? user.getLastName() : user.getContactPerson();
                    sessionManager.createLoginSession(
                            user.getUserId(),
                            user.getClientId(),
                            user.getAuthId(),
                            user.getUsername(),
                            user.getRole(),
                            fName,
                            lName,
                            user.getEmail(),
                            user.getPhone(),
                            user.getCompanyName(),
                            user.getContactPerson(),
                            user.getGstin(),
                            user.getPanNumber()
                    );

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    tilPassword.setError("Invalid credentials. Please check your password.");
                    etPassword.requestFocus();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                String host = Constants.getBaseUrl(LoginActivity.this);
                tilPassword.setError("Unable to connect to " + host + ". Tap 'Server (Change)' below to adjust IP.");
            }
        });
    }
}
