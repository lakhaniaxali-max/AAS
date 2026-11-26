package com.accurateaccounts.app.ui.profile;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.accurateaccounts.app.R;
import com.accurateaccounts.app.api.ApiClient;
import com.accurateaccounts.app.models.User;
import com.accurateaccounts.app.ui.admin.AdminDocumentsFragment;
import com.accurateaccounts.app.ui.admin.ManageUsersFragment;
import com.accurateaccounts.app.ui.client.ClientDocumentsFragment;
import com.accurateaccounts.app.ui.landing.LandingActivity;
import com.accurateaccounts.app.ui.main.MainActivity;
import com.accurateaccounts.app.ui.tasks.TasksFragment;
import com.accurateaccounts.app.utils.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private SessionManager sessionManager;

    private TextView tvInitial;
    private TextView tvFullName;
    private TextView tvUsername;
    private TextView tvRoleBadge;
    private TextView tvDetailsCardTitle;

    private LinearLayout layoutCompanyRow;
    private TextView tvProfileCompany;

    private LinearLayout layoutContactRow;
    private TextView tvProfileContactPerson;

    private TextView tvProfileEmail;
    private TextView tvProfilePhone;

    private LinearLayout layoutGstinRow;
    private TextView tvProfileGstin;

    private LinearLayout layoutPanRow;
    private TextView tvProfilePan;

    private LinearLayout layoutRoleDetailRow;
    private TextView tvProfileRoleDetail;

    private MaterialCardView cardAdminUserSection;
    private MaterialCardView cardEmployeeSection;
    private MaterialCardView cardClientSection;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        sessionManager = new SessionManager(requireContext());

        tvInitial = view.findViewById(R.id.tvProfileInitial);
        tvFullName = view.findViewById(R.id.tvProfileFullName);
        tvUsername = view.findViewById(R.id.tvProfileUsername);
        tvRoleBadge = view.findViewById(R.id.tvProfileRoleBadge);
        tvDetailsCardTitle = view.findViewById(R.id.tvDetailsCardTitle);

        layoutCompanyRow = view.findViewById(R.id.layoutCompanyRow);
        tvProfileCompany = view.findViewById(R.id.tvProfileCompany);

        layoutContactRow = view.findViewById(R.id.layoutContactRow);
        tvProfileContactPerson = view.findViewById(R.id.tvProfileContactPerson);

        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfilePhone = view.findViewById(R.id.tvProfilePhone);

        layoutGstinRow = view.findViewById(R.id.layoutGstinRow);
        tvProfileGstin = view.findViewById(R.id.tvProfileGstin);

        layoutPanRow = view.findViewById(R.id.layoutPanRow);
        tvProfilePan = view.findViewById(R.id.tvProfilePan);

        layoutRoleDetailRow = view.findViewById(R.id.layoutRoleDetailRow);
        tvProfileRoleDetail = view.findViewById(R.id.tvProfileRoleDetail);

        cardAdminUserSection = view.findViewById(R.id.cardAdminUserSection);
        cardEmployeeSection = view.findViewById(R.id.cardEmployeeSection);
        cardClientSection = view.findViewById(R.id.cardClientSection);

        MaterialButton btnManageUsers = view.findViewById(R.id.btnProfileOpenManageUsers);
        MaterialButton btnOpenTasks = view.findViewById(R.id.btnProfileOpenTasks);
        MaterialButton btnOpenVault = view.findViewById(R.id.btnProfileOpenVault);
        MaterialButton btnLogout = view.findViewById(R.id.btnProfileLogout);

        // 1. Initial render from SessionManager
        renderProfileData();

        // 2. Fetch fresh live profile details from DB using auth_id
        fetchFreshProfileData();

        // Listeners
        if (btnManageUsers != null) {
            btnManageUsers.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).loadFragment(new ManageUsersFragment(), true);
                }
            });
        }

        if (btnOpenTasks != null) {
            btnOpenTasks.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).loadFragment(new TasksFragment(), false);
                }
            });
        }

        if (btnOpenVault != null) {
            btnOpenVault.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    String role = sessionManager.getUserRole();
                    if ("client".equalsIgnoreCase(role)) {
                        ((MainActivity) getActivity()).loadFragment(new ClientDocumentsFragment(), false);
                    } else {
                        ((MainActivity) getActivity()).loadFragment(new AdminDocumentsFragment(), false);
                    }
                }
            });
        }

        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());

        return view;
    }

    private void showLogoutConfirmationDialog() {
        if (getContext() == null) return;

        BottomSheetDialog bottomSheet = new BottomSheetDialog(requireContext(), com.google.android.material.R.style.Theme_Design_BottomSheetDialog);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_logout, null);
        bottomSheet.setContentView(dialogView);

        if (bottomSheet.getWindow() != null) {
            View sheetView = bottomSheet.getWindow().findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheetView != null) {
                sheetView.setBackgroundResource(android.R.color.transparent);
            }
        }

        TextView tvLogoutUserAccount = dialogView.findViewById(R.id.tvLogoutUserAccount);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelLogout);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirmLogout);

        String displayName = sessionManager.getFirstName();
        if (sessionManager.getLastName() != null && !sessionManager.getLastName().trim().isEmpty()) {
            displayName = (displayName != null ? displayName : "") + " " + sessionManager.getLastName();
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = sessionManager.getUsername();
        }
        tvLogoutUserAccount.setText(displayName.trim() + " (@" + sessionManager.getUsername() + ")");

        btnCancel.setOnClickListener(v -> bottomSheet.dismiss());

        btnConfirm.setOnClickListener(v -> {
            bottomSheet.dismiss();
            sessionManager.logout();
            Intent intent = new Intent(getActivity(), LandingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        bottomSheet.show();
    }

    private void renderProfileData() {
        String username = sessionManager.getUsername();
        String role = sessionManager.getUserRole();
        String firstName = sessionManager.getFirstName();
        String lastName = sessionManager.getLastName();
        String email = sessionManager.getEmail();
        String phone = sessionManager.getPhone();
        String company = sessionManager.getCompanyName();
        String contact = sessionManager.getContactPerson();
        String gstin = sessionManager.getGstin();
        String pan = sessionManager.getPanNumber();

        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setCornerRadius(20);

        if ("client".equalsIgnoreCase(role)) {
            // CLIENT PROFILE
            String mainTitle = (company != null && !company.trim().isEmpty()) ? company : username;
            tvFullName.setText(mainTitle);

            String sub = "@" + username;
            if (contact != null && !contact.trim().isEmpty()) {
                sub = contact + " • @" + username;
            }
            tvUsername.setText(sub);

            if (mainTitle != null && !mainTitle.trim().isEmpty()) {
                tvInitial.setText(mainTitle.substring(0, 1).toUpperCase());
            } else {
                tvInitial.setText("🏢");
            }
            tvInitial.setBackgroundColor(Color.parseColor("#D97706"));

            tvRoleBadge.setText("🏢 Corporate Client");
            tvRoleBadge.setTextColor(Color.parseColor("#B45309"));
            badgeBg.setColor(Color.parseColor("#FEF3C7"));

            tvDetailsCardTitle.setText("🏢 Corporate Entity & Account Details");

            layoutCompanyRow.setVisibility(View.VISIBLE);
            tvProfileCompany.setText(!mainTitle.isEmpty() ? mainTitle : "Not Provided");

            layoutContactRow.setVisibility(View.VISIBLE);
            tvProfileContactPerson.setText((contact != null && !contact.isEmpty()) ? contact : (firstName + " " + lastName).trim());

            layoutGstinRow.setVisibility((gstin != null && !gstin.isEmpty()) ? View.VISIBLE : View.GONE);
            tvProfileGstin.setText(gstin);

            layoutPanRow.setVisibility((pan != null && !pan.isEmpty()) ? View.VISIBLE : View.GONE);
            tvProfilePan.setText(pan);

            layoutRoleDetailRow.setVisibility(View.GONE);

            cardAdminUserSection.setVisibility(View.GONE);
            cardEmployeeSection.setVisibility(View.GONE);
            cardClientSection.setVisibility(View.VISIBLE);

        } else if ("employee".equalsIgnoreCase(role)) {
            // EMPLOYEE / STAFF PROFILE
            String fullName = (firstName + " " + lastName).trim();
            if (fullName.isEmpty()) fullName = username;
            tvFullName.setText(fullName);
            tvUsername.setText("Accounting Staff • @" + username);

            if (!fullName.isEmpty()) {
                tvInitial.setText(fullName.substring(0, 1).toUpperCase());
            } else {
                tvInitial.setText("💼");
            }
            tvInitial.setBackgroundColor(Color.parseColor("#059669"));

            tvRoleBadge.setText("💼 Staff Member");
            tvRoleBadge.setTextColor(Color.parseColor("#047857"));
            badgeBg.setColor(Color.parseColor("#ECFDF5"));

            tvDetailsCardTitle.setText("👤 Staff Member Credentials");

            layoutCompanyRow.setVisibility(View.GONE);
            layoutContactRow.setVisibility(View.GONE);
            layoutGstinRow.setVisibility(View.GONE);
            layoutPanRow.setVisibility(View.GONE);

            layoutRoleDetailRow.setVisibility(View.VISIBLE);
            tvProfileRoleDetail.setText("Accounts Review, Auditing & Tasks");

            cardAdminUserSection.setVisibility(View.GONE);
            cardEmployeeSection.setVisibility(View.VISIBLE);
            cardClientSection.setVisibility(View.GONE);

        } else {
            // ADMIN PROFILE
            String fullName = (firstName + " " + lastName).trim();
            if (fullName.isEmpty()) fullName = "System Administrator";
            tvFullName.setText(fullName);
            tvUsername.setText("Super Administrator • @" + username);

            if (!fullName.isEmpty()) {
                tvInitial.setText(fullName.substring(0, 1).toUpperCase());
            } else {
                tvInitial.setText("A");
            }
            tvInitial.setBackgroundColor(Color.parseColor("#2563EB"));

            tvRoleBadge.setText("👑 Administrator");
            tvRoleBadge.setTextColor(Color.parseColor("#1D4ED8"));
            badgeBg.setColor(Color.parseColor("#EFF6FF"));

            tvDetailsCardTitle.setText("👤 Administrator Credentials");

            layoutCompanyRow.setVisibility(View.GONE);
            layoutContactRow.setVisibility(View.GONE);
            layoutGstinRow.setVisibility(View.GONE);
            layoutPanRow.setVisibility(View.GONE);

            layoutRoleDetailRow.setVisibility(View.VISIBLE);
            tvProfileRoleDetail.setText("Full Portal & User Administration");

            cardAdminUserSection.setVisibility(View.VISIBLE);
            cardEmployeeSection.setVisibility(View.GONE);
            cardClientSection.setVisibility(View.GONE);
        }

        tvRoleBadge.setBackground(badgeBg);

        // Actual user email & phone (Fallback to sensible format if empty)
        if (email != null && !email.trim().isEmpty()) {
            tvProfileEmail.setText(email);
        } else {
            tvProfileEmail.setText(username + "@accurateaccounts.com");
        }

        if (phone != null && !phone.trim().isEmpty()) {
            tvProfilePhone.setText(phone);
        } else {
            tvProfilePhone.setText("+91 9876543210");
        }
    }

    private void fetchFreshProfileData() {
        int authId = sessionManager.getAuthId();
        if (authId <= 0) return;

        ApiClient.getService().getUserProfileByAuthId(authId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    User u = response.body();
                    String email = u.getEmail();
                    String phone = u.getPhone();
                    String company = u.getCompanyName();
                    String contact = u.getContactPerson();
                    String gstin = u.getGstin();
                    String pan = u.getPanNumber();

                    // Update session
                    sessionManager.updateProfileDetails(email, phone, company, contact, gstin, pan);

                    // Re-render UI with fresh live data
                    renderProfileData();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                // Keep displaying initial cached session data gracefully
            }
        });
    }
}
