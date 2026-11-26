package com.accurateaccounts.app.ui.admin;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.accurateaccounts.app.R;
import com.accurateaccounts.app.api.ApiClient;
import com.accurateaccounts.app.models.RegisterRequest;
import com.accurateaccounts.app.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageUsersFragment extends Fragment {

    // Tab buttons & containers
    private MaterialButton btnTabDirectory, btnTabCreate;
    private View layoutDirectoryContainer, layoutCreateContainer;
    private SwipeRefreshLayout swipeRefresh;

    // Form fields
    private TextInputEditText etNewUsername, etNewPassword, etNewFirstName, etNewLastName, etNewEmail, etNewPhone;
    private TextInputLayout tilNewFirstName, tilNewLastName;
    private RadioGroup rgRoleSelection;
    private RadioButton rbRoleAdmin, rbRoleEmployee, rbRoleClient;
    private MaterialButton btnSubmitCreateUser, btnRefreshUsers;
    private ProgressBar pbCreateUser, pbLoadingUsers;
    private TextView tvDirectoryTitle, tvNoUsersFound;

    // Directory list & Pagination
    private static final int PAGE_SIZE = 10;
    private int currentPage = 1;
    private View layoutPaginationControls;
    private LinearLayout layoutPageNumbers;
    private TextView tvPaginationInfo;
    private RecyclerView rvUsersList;
    private UserAdapter userAdapter;
    private final List<User> allUsers = new ArrayList<>();
    private final List<User> pagedUsers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_users, container, false);

        // Tabs
        btnTabDirectory = view.findViewById(R.id.btnTabDirectory);
        btnTabCreate = view.findViewById(R.id.btnTabCreate);
        layoutDirectoryContainer = view.findViewById(R.id.layoutDirectoryContainer);
        layoutCreateContainer = view.findViewById(R.id.layoutCreateContainer);
        swipeRefresh = view.findViewById(R.id.swipeRefreshManageUsers);

        // Form
        etNewUsername = view.findViewById(R.id.etNewUsername);
        etNewPassword = view.findViewById(R.id.etNewPassword);
        etNewFirstName = view.findViewById(R.id.etNewFirstName);
        etNewLastName = view.findViewById(R.id.etNewLastName);
        etNewEmail = view.findViewById(R.id.etNewEmail);
        etNewPhone = view.findViewById(R.id.etNewPhone);

        tilNewFirstName = view.findViewById(R.id.tilNewFirstName);
        tilNewLastName = view.findViewById(R.id.tilNewLastName);

        rgRoleSelection = view.findViewById(R.id.rgRoleSelection);
        rbRoleAdmin = view.findViewById(R.id.rbRoleAdmin);
        rbRoleEmployee = view.findViewById(R.id.rbRoleEmployee);
        rbRoleClient = view.findViewById(R.id.rbRoleClient);

        btnSubmitCreateUser = view.findViewById(R.id.btnSubmitCreateUser);
        btnRefreshUsers = view.findViewById(R.id.btnRefreshUsers);
        pbCreateUser = view.findViewById(R.id.pbCreateUser);
        pbLoadingUsers = view.findViewById(R.id.pbLoadingUsers);

        tvDirectoryTitle = view.findViewById(R.id.tvDirectoryTitle);
        tvNoUsersFound = view.findViewById(R.id.tvNoUsersFound);
        rvUsersList = view.findViewById(R.id.rvUsersList);

        // Pagination Views
        layoutPaginationControls = view.findViewById(R.id.layoutPaginationControls);
        layoutPageNumbers = view.findViewById(R.id.layoutPageNumbers);
        tvPaginationInfo = view.findViewById(R.id.tvPaginationInfo);

        // RecyclerView setup with pagedUsers
        rvUsersList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvUsersList.setHasFixedSize(true);
        userAdapter = new UserAdapter(pagedUsers);
        rvUsersList.setAdapter(userAdapter);

        // Tab click listeners
        btnTabDirectory.setOnClickListener(v -> showDirectoryTab());
        btnTabCreate.setOnClickListener(v -> showCreateTab());

        // Role change listener
        rgRoleSelection.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbRoleClient) {
                tilNewFirstName.setHint("Company Name *");
                tilNewLastName.setHint("Contact Person");
            } else {
                tilNewFirstName.setHint("First Name *");
                tilNewLastName.setHint("Last Name");
            }
        });

        btnSubmitCreateUser.setOnClickListener(v -> performCreateUser());
        btnRefreshUsers.setOnClickListener(v -> loadUsers());
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::loadUsers);
        }

        // Show Directory Tab initially & load live database users
        showDirectoryTab();

        return view;
    }

    private void showDirectoryTab() {
        layoutDirectoryContainer.setVisibility(View.VISIBLE);
        layoutCreateContainer.setVisibility(View.GONE);

        // Style Tab Directory as active
        btnTabDirectory.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
        btnTabDirectory.setTextColor(Color.WHITE);
        btnTabDirectory.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#2563EB")));

        // Style Tab Create as inactive
        btnTabCreate.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
        btnTabCreate.setTextColor(Color.parseColor("#2563EB"));
        btnTabCreate.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#2563EB")));

        loadUsers();
    }

    private void showCreateTab() {
        layoutDirectoryContainer.setVisibility(View.GONE);
        layoutCreateContainer.setVisibility(View.VISIBLE);

        // Style Tab Create as active
        btnTabCreate.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
        btnTabCreate.setTextColor(Color.WHITE);
        btnTabCreate.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#2563EB")));

        // Style Tab Directory as inactive
        btnTabDirectory.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
        btnTabDirectory.setTextColor(Color.parseColor("#2563EB"));
        btnTabDirectory.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#2563EB")));
    }

    private void performCreateUser() {
        String username = etNewUsername.getText() != null ? etNewUsername.getText().toString().trim() : "";
        String password = etNewPassword.getText() != null ? etNewPassword.getText().toString().trim() : "";
        String firstName = etNewFirstName.getText() != null ? etNewFirstName.getText().toString().trim() : "";
        String lastName = etNewLastName.getText() != null ? etNewLastName.getText().toString().trim() : "";
        String email = etNewEmail.getText() != null ? etNewEmail.getText().toString().trim() : "";
        String phone = etNewPhone.getText() != null ? etNewPhone.getText().toString().trim() : "";

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Username and Password are required", Toast.LENGTH_SHORT).show();
            return;
        }

        String role = "employee";
        if (rbRoleAdmin.isChecked()) {
            role = "admin";
        } else if (rbRoleClient.isChecked()) {
            role = "client";
        }

        final String finalRole = role;
        final String finalUsername = username;

        pbCreateUser.setVisibility(View.VISIBLE);
        btnSubmitCreateUser.setEnabled(false);

        RegisterRequest request;
        if ("client".equals(finalRole)) {
            String companyName = firstName.isEmpty() ? finalUsername + " Enterprises" : firstName;
            String contactPerson = lastName.isEmpty() ? finalUsername : lastName;
            request = new RegisterRequest(finalUsername, password, finalRole, companyName, contactPerson, email, phone, "", "");
        } else {
            String fName = firstName.isEmpty() ? finalUsername : firstName;
            request = new RegisterRequest(finalUsername, password, finalRole, fName, lastName, email, phone);
        }

        ApiClient.getService().registerUser(request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                pbCreateUser.setVisibility(View.GONE);
                btnSubmitCreateUser.setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "User @" + finalUsername + " (" + finalRole + ") created successfully!", Toast.LENGTH_LONG).show();
                    // Clear inputs
                    etNewUsername.setText("");
                    etNewPassword.setText("");
                    etNewFirstName.setText("");
                    etNewLastName.setText("");
                    etNewEmail.setText("");
                    etNewPhone.setText("");

                    // Automatically switch to Directory tab to show the new user
                    showDirectoryTab();
                } else {
                    String errorMsg = "Creation failed.";
                    try {
                        if (response.errorBody() != null) {
                            String errJson = response.errorBody().string();
                            org.json.JSONObject obj = new org.json.JSONObject(errJson);
                            if (obj.has("error")) {
                                errorMsg = obj.getString("error");
                                if (obj.has("details")) {
                                    errorMsg += ": " + obj.getString("details");
                                }
                            } else if (obj.has("details")) {
                                errorMsg = obj.getString("details");
                            }
                        }
                    } catch (Exception ignored) {
                        errorMsg = "Creation failed. Please try again.";
                    }
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                pbCreateUser.setVisibility(View.GONE);
                btnSubmitCreateUser.setEnabled(true);
                Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUsers() {
        pbLoadingUsers.setVisibility(View.VISIBLE);
        tvNoUsersFound.setVisibility(View.GONE);

        ApiClient.getService().getAllUsers().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                pbLoadingUsers.setVisibility(View.GONE);
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    allUsers.clear();
                    allUsers.addAll(response.body());
                    fetchClients();
                } else {
                    fetchClients();
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                fetchClients();
            }
        });
    }

    private void fetchClients() {
        ApiClient.getService().getAllClients().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                pbLoadingUsers.setVisibility(View.GONE);
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    for (User clientUser : response.body()) {
                        boolean exists = false;
                        for (User u : allUsers) {
                            if (u.getAuthId() == clientUser.getAuthId() && u.getAuthId() != 0) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            allUsers.add(clientUser);
                        }
                    }
                }
                updatePagination();
                btnTabDirectory.setText("👥 Directory (" + allUsers.size() + ")");
                tvDirectoryTitle.setText("Registered Users (" + allUsers.size() + ")");
                tvNoUsersFound.setVisibility(allUsers.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                pbLoadingUsers.setVisibility(View.GONE);
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);

                updatePagination();
                btnTabDirectory.setText("👥 Directory (" + allUsers.size() + ")");
                tvDirectoryTitle.setText("Registered Users (" + allUsers.size() + ")");
                tvNoUsersFound.setVisibility(allUsers.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void updatePagination() {
        int totalUsers = allUsers.size();
        int totalPages = (int) Math.ceil((double) totalUsers / PAGE_SIZE);
        if (totalPages < 1) totalPages = 1;
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        int fromIndex = (currentPage - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalUsers);

        pagedUsers.clear();
        if (fromIndex < totalUsers) {
            pagedUsers.addAll(allUsers.subList(fromIndex, toIndex));
        }
        userAdapter.notifyDataSetChanged();

        if (totalUsers > 0) {
            tvPaginationInfo.setText("Showing " + (fromIndex + 1) + "–" + toIndex + " of " + totalUsers + " registered users");
            layoutPaginationControls.setVisibility(View.VISIBLE);

            if (layoutPageNumbers != null) {
                layoutPageNumbers.removeAllViews();
                for (int p = 1; p <= totalPages; p++) {
                    final int pageNum = p;
                    TextView btnPage = new TextView(getContext());
                    btnPage.setText(String.valueOf(p));
                    btnPage.setTextSize(13f);
                    btnPage.setTypeface(null, android.graphics.Typeface.BOLD);
                    btnPage.setGravity(android.view.Gravity.CENTER);

                    int sizePx = (int) (32 * getResources().getDisplayMetrics().density);
                    int marginPx = (int) (3 * getResources().getDisplayMetrics().density);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
                    lp.setMargins(marginPx, 0, marginPx, 0);
                    btnPage.setLayoutParams(lp);

                    android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
                    bg.setCornerRadius(8 * getResources().getDisplayMetrics().density);

                    if (p == currentPage) {
                        bg.setColor(Color.parseColor("#2563EB"));
                        btnPage.setTextColor(Color.WHITE);
                    } else {
                        bg.setColor(Color.parseColor("#F1F5F9"));
                        bg.setStroke((int) (1 * getResources().getDisplayMetrics().density), Color.parseColor("#CBD5E1"));
                        btnPage.setTextColor(Color.parseColor("#1E293B"));
                    }
                    btnPage.setBackground(bg);

                    btnPage.setOnClickListener(v -> {
                        if (currentPage != pageNum) {
                            currentPage = pageNum;
                            updatePagination();
                            rvUsersList.scrollToPosition(0);
                        }
                    });

                    layoutPageNumbers.addView(btnPage);
                }
            }
        } else {
            layoutPaginationControls.setVisibility(View.GONE);
        }
    }
}
