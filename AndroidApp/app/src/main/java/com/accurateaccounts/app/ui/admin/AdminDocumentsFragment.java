package com.accurateaccounts.app.ui.admin;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.accurateaccounts.app.R;
import com.accurateaccounts.app.api.ApiClient;
import com.accurateaccounts.app.models.ClientDocumentsResponse;
import com.accurateaccounts.app.models.DocumentDownloadResponse;
import com.accurateaccounts.app.models.SheetUploadRequest;
import com.accurateaccounts.app.models.User;
import com.accurateaccounts.app.ui.common.DocumentViewerHelper;
import androidx.core.widget.NestedScrollView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDocumentsFragment extends Fragment {

    // ================= VIEW 1: CLIENTS LIST =================
    private LinearLayout layoutClientsListView;
    private TextInputEditText etSearchClients;
    private TextView tvClientCountBadge;
    private ProgressBar pbLoadingClientsList;
    private LinearLayout layoutEmptyClientsState;
    private MaterialButton btnRetryLoadClients;
    private SwipeRefreshLayout swipeRefreshClientsList;
    private RecyclerView rvAdminClientsList;
    private ClientDocAdapter clientDocAdapter;
    private final List<User> allClients = new ArrayList<>();

    // ================= VIEW 2: CLIENT DOCS DETAIL =================
    private LinearLayout layoutClientDocsDetailView;
    private MaterialButton btnBackToClientsList;
    private TextView tvDetailTopTitle;
    private ProgressBar pbDetailDocsLoading;
    private SwipeRefreshLayout swipeRefreshClientDetailDocs;
    private NestedScrollView scrollClientDetailDocs;

    // Selected client header views
    private TextView tvDetailClientInitial, tvDetailClientName, tvDetailClientContact, tvDetailClientInfo;

    // KYC Identity Views (Aadhar & PAN)
    private TextView tvAdminAadharStatus, tvAdminAadharMeta, tvAdminPanStatus, tvAdminPanMeta;
    private LinearLayout layoutAdminAadharActions, layoutAdminPanActions;
    private MaterialButton btnAdminViewAadhar, btnAdminDownloadAadhar;
    private MaterialButton btnAdminViewPan, btnAdminDownloadPan;
    private int aadharDocId = -1, panDocId = -1;

    // Financial Statements (Balance Sheet & P&L)
    private TextView tvAdminBalanceSheetStatus, tvAdminBalanceSheetMeta, tvAdminProfitLossStatus, tvAdminProfitLossMeta;
    private LinearLayout layoutAdminBalanceSheetActions, layoutAdminProfitLossActions;
    private MaterialButton btnAdminViewBalanceSheet, btnAdminDownloadBalanceSheet, btnAdminDeleteBalanceSheet, btnAdminUploadBalanceSheet;
    private MaterialButton btnAdminViewProfitLoss, btnAdminDownloadProfitLoss, btnAdminDeleteProfitLoss, btnAdminUploadProfitLoss;
    private int balanceSheetDocId = -1, profitLossDocId = -1;

    // Other Documents
    private MaterialButton btnAdminOpenUploadOtherPopup;
    private LinearLayout layoutAdminOtherDocsContainer;
    private TextView tvAdminNoOtherDocs;

    // Current State
    private User selectedClient = null;
    private boolean isDetailViewActive = false;
    private String pendingFinancialSheetType = null;
    private OnBackPressedCallback detailBackCallback;

    // Activity Result Launchers
    private final ActivityResultLauncher<String[]> financialPdfPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null && pendingFinancialSheetType != null && selectedClient != null) {
                    uploadFinancialPdf(uri, pendingFinancialSheetType);
                }
            });

    // Dialog state for "Upload Other Document"
    private Uri pendingOtherFileUri = null;
    private Dialog activeOtherDocDialog = null;
    private final ActivityResultLauncher<String[]> otherFilePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null && activeOtherDocDialog != null && activeOtherDocDialog.isShowing()) {
                    pendingOtherFileUri = uri;
                    updateOtherDialogFilePreview(activeOtherDocDialog, uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_documents, container, false);

        initViews(view);
        setupListeners();
        setupBackNavigation();

        loadClients();

        return view;
    }

    private void initViews(View view) {
        // View 1
        layoutClientsListView = view.findViewById(R.id.layoutClientsListView);
        etSearchClients = view.findViewById(R.id.etSearchClients);
        tvClientCountBadge = view.findViewById(R.id.tvClientCountBadge);
        pbLoadingClientsList = view.findViewById(R.id.pbLoadingClientsList);
        layoutEmptyClientsState = view.findViewById(R.id.layoutEmptyClientsState);
        btnRetryLoadClients = view.findViewById(R.id.btnRetryLoadClients);
        swipeRefreshClientsList = view.findViewById(R.id.swipeRefreshClientsList);
        rvAdminClientsList = view.findViewById(R.id.rvAdminClientsList);

        rvAdminClientsList.setLayoutManager(new LinearLayoutManager(getContext()));
        clientDocAdapter = new ClientDocAdapter(allClients, this::onClientSelected);
        rvAdminClientsList.setAdapter(clientDocAdapter);

        // View 2
        layoutClientDocsDetailView = view.findViewById(R.id.layoutClientDocsDetailView);
        btnBackToClientsList = view.findViewById(R.id.btnBackToClientsList);
        tvDetailTopTitle = view.findViewById(R.id.tvDetailTopTitle);
        pbDetailDocsLoading = view.findViewById(R.id.pbDetailDocsLoading);
        swipeRefreshClientDetailDocs = view.findViewById(R.id.swipeRefreshClientDetailDocs);
        scrollClientDetailDocs = view.findViewById(R.id.scrollClientDetailDocs);

        tvDetailClientInitial = view.findViewById(R.id.tvDetailClientInitial);
        tvDetailClientName = view.findViewById(R.id.tvDetailClientName);
        tvDetailClientContact = view.findViewById(R.id.tvDetailClientContact);
        tvDetailClientInfo = view.findViewById(R.id.tvDetailClientInfo);

        // KYC
        tvAdminAadharStatus = view.findViewById(R.id.tvAdminAadharStatus);
        tvAdminAadharMeta = view.findViewById(R.id.tvAdminAadharMeta);
        layoutAdminAadharActions = view.findViewById(R.id.layoutAdminAadharActions);
        btnAdminViewAadhar = view.findViewById(R.id.btnAdminViewAadhar);
        btnAdminDownloadAadhar = view.findViewById(R.id.btnAdminDownloadAadhar);

        tvAdminPanStatus = view.findViewById(R.id.tvAdminPanStatus);
        tvAdminPanMeta = view.findViewById(R.id.tvAdminPanMeta);
        layoutAdminPanActions = view.findViewById(R.id.layoutAdminPanActions);
        btnAdminViewPan = view.findViewById(R.id.btnAdminViewPan);
        btnAdminDownloadPan = view.findViewById(R.id.btnAdminDownloadPan);

        // Financial
        tvAdminBalanceSheetStatus = view.findViewById(R.id.tvAdminBalanceSheetStatus);
        tvAdminBalanceSheetMeta = view.findViewById(R.id.tvAdminBalanceSheetMeta);
        layoutAdminBalanceSheetActions = view.findViewById(R.id.layoutAdminBalanceSheetActions);
        btnAdminViewBalanceSheet = view.findViewById(R.id.btnAdminViewBalanceSheet);
        btnAdminDownloadBalanceSheet = view.findViewById(R.id.btnAdminDownloadBalanceSheet);
        btnAdminDeleteBalanceSheet = view.findViewById(R.id.btnAdminDeleteBalanceSheet);
        btnAdminUploadBalanceSheet = view.findViewById(R.id.btnAdminUploadBalanceSheet);

        tvAdminProfitLossStatus = view.findViewById(R.id.tvAdminProfitLossStatus);
        tvAdminProfitLossMeta = view.findViewById(R.id.tvAdminProfitLossMeta);
        layoutAdminProfitLossActions = view.findViewById(R.id.layoutAdminProfitLossActions);
        btnAdminViewProfitLoss = view.findViewById(R.id.btnAdminViewProfitLoss);
        btnAdminDownloadProfitLoss = view.findViewById(R.id.btnAdminDownloadProfitLoss);
        btnAdminDeleteProfitLoss = view.findViewById(R.id.btnAdminDeleteProfitLoss);
        btnAdminUploadProfitLoss = view.findViewById(R.id.btnAdminUploadProfitLoss);

        // Other
        btnAdminOpenUploadOtherPopup = view.findViewById(R.id.btnAdminOpenUploadOtherPopup);
        layoutAdminOtherDocsContainer = view.findViewById(R.id.layoutAdminOtherDocsContainer);
        tvAdminNoOtherDocs = view.findViewById(R.id.tvAdminNoOtherDocs);
    }

    private void setupListeners() {
        // Search filter
        etSearchClients.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                clientDocAdapter.filter(s != null ? s.toString() : "");
                tvClientCountBadge.setText("Clients (" + clientDocAdapter.getFilteredCount() + ")");
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Swipe refresh for list
        swipeRefreshClientsList.setOnRefreshListener(this::loadClients);
        btnRetryLoadClients.setOnClickListener(v -> loadClients());

        // Back button from detail view
        btnBackToClientsList.setOnClickListener(v -> showClientsListView());

        // Swipe refresh for detail
        swipeRefreshClientDetailDocs.setOnRefreshListener(() -> {
            if (selectedClient != null) {
                loadDocumentsForClient(selectedClient.getClientId());
            } else {
                swipeRefreshClientDetailDocs.setRefreshing(false);
            }
        });

        if (scrollClientDetailDocs != null) {
            swipeRefreshClientDetailDocs.setOnChildScrollUpCallback((parent, child) -> scrollClientDetailDocs.canScrollVertically(-1));
        }

        // KYC Actions (Admin can only View & Download client KYC; only client can delete)
        btnAdminViewAadhar.setOnClickListener(v -> {
            if (aadharDocId > 0) fetchDoc("important", aadharDocId, false);
        });
        btnAdminDownloadAadhar.setOnClickListener(v -> {
            if (aadharDocId > 0) fetchDoc("important", aadharDocId, true);
        });

        btnAdminViewPan.setOnClickListener(v -> {
            if (panDocId > 0) fetchDoc("important", panDocId, false);
        });
        btnAdminDownloadPan.setOnClickListener(v -> {
            if (panDocId > 0) fetchDoc("important", panDocId, true);
        });

        // Financial Sheet Actions
        btnAdminViewBalanceSheet.setOnClickListener(v -> {
            if (balanceSheetDocId > 0) fetchDoc("important", balanceSheetDocId, false);
        });
        btnAdminDownloadBalanceSheet.setOnClickListener(v -> {
            if (balanceSheetDocId > 0) fetchDoc("important", balanceSheetDocId, true);
        });
        btnAdminDeleteBalanceSheet.setOnClickListener(v -> {
            if (balanceSheetDocId > 0) confirmDeleteDoc("important", balanceSheetDocId, "Balance Sheet");
        });
        btnAdminUploadBalanceSheet.setOnClickListener(v -> {
            pendingFinancialSheetType = "Balance Sheet";
            financialPdfPickerLauncher.launch(new String[]{"application/pdf"});
        });

        btnAdminViewProfitLoss.setOnClickListener(v -> {
            if (profitLossDocId > 0) fetchDoc("important", profitLossDocId, false);
        });
        btnAdminDownloadProfitLoss.setOnClickListener(v -> {
            if (profitLossDocId > 0) fetchDoc("important", profitLossDocId, true);
        });
        btnAdminDeleteProfitLoss.setOnClickListener(v -> {
            if (profitLossDocId > 0) confirmDeleteDoc("important", profitLossDocId, "Profit & Loss Statement");
        });
        btnAdminUploadProfitLoss.setOnClickListener(v -> {
            pendingFinancialSheetType = "Profit and Loss";
            financialPdfPickerLauncher.launch(new String[]{"application/pdf"});
        });

        // Other Docs Popup Button
        btnAdminOpenUploadOtherPopup.setOnClickListener(v -> showUploadOtherDocPopup());
    }

    private void setupBackNavigation() {
        detailBackCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                showClientsListView();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), detailBackCallback);
    }

    // =========================================================================
    // CLIENT SELECTION & VIEW SWITCHING
    // =========================================================================

    private void onClientSelected(User client) {
        selectedClient = client;
        isDetailViewActive = true;
        if (detailBackCallback != null) {
            detailBackCallback.setEnabled(true);
        }

        layoutClientsListView.setVisibility(View.GONE);
        layoutClientDocsDetailView.setVisibility(View.VISIBLE);

        // Populate client profile header
        String displayName = client.getDisplayName();
        tvDetailTopTitle.setText(displayName);
        tvDetailClientName.setText(displayName);
        tvDetailClientInitial.setText(displayName != null && !displayName.trim().isEmpty()
                ? displayName.trim().substring(0, 1).toUpperCase() : "C");

        String contact = client.getContactPerson();
        tvDetailClientContact.setText(contact != null && !contact.trim().isEmpty()
                ? "Contact: " + contact.trim() : "Client Account (@" + client.getUsername() + ")");

        StringBuilder sb = new StringBuilder();
        if (client.getEmail() != null && !client.getEmail().trim().isEmpty()) {
            sb.append(client.getEmail().trim());
        }
        if (client.getPhone() != null && !client.getPhone().trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append(client.getPhone().trim());
        }
        tvDetailClientInfo.setText(sb.length() > 0 ? sb.toString() : "No contact registered");

        // Load documents for this client
        loadDocumentsForClient(client.getClientId());
    }

    private void showClientsListView() {
        isDetailViewActive = false;
        selectedClient = null;
        if (detailBackCallback != null) {
            detailBackCallback.setEnabled(false);
        }

        layoutClientDocsDetailView.setVisibility(View.GONE);
        layoutClientsListView.setVisibility(View.VISIBLE);

        // Refresh list to update any document badges/counts
        loadClients();
    }

    // =========================================================================
    // DATA FETCHING: ALL CLIENTS
    // =========================================================================

    private void loadClients() {
        pbLoadingClientsList.setVisibility(View.VISIBLE);
        layoutEmptyClientsState.setVisibility(View.GONE);

        ApiClient.getService().getAllClients().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                pbLoadingClientsList.setVisibility(View.GONE);
                swipeRefreshClientsList.setRefreshing(false);
                if (!isAdded()) return;

                allClients.clear();
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    allClients.addAll(response.body());
                    clientDocAdapter.setClients(allClients);
                    tvClientCountBadge.setText("Clients (" + allClients.size() + ")");
                    layoutEmptyClientsState.setVisibility(View.GONE);
                    rvAdminClientsList.setVisibility(View.VISIBLE);
                } else {
                    clientDocAdapter.setClients(allClients);
                    tvClientCountBadge.setText("Clients (0)");
                    layoutEmptyClientsState.setVisibility(View.VISIBLE);
                    rvAdminClientsList.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                pbLoadingClientsList.setVisibility(View.GONE);
                swipeRefreshClientsList.setRefreshing(false);
                if (!isAdded()) return;

                if (allClients.isEmpty()) {
                    layoutEmptyClientsState.setVisibility(View.VISIBLE);
                    rvAdminClientsList.setVisibility(View.GONE);
                }
                Toast.makeText(getContext(), "Failed to refresh clients: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =========================================================================
    // DATA FETCHING: DOCUMENTS FOR SELECTED CLIENT
    // =========================================================================

    private void loadDocumentsForClient(int clientId) {
        pbDetailDocsLoading.setVisibility(View.VISIBLE);

        ApiClient.getService().getClientDocuments(clientId).enqueue(new Callback<ClientDocumentsResponse>() {
            @Override
            public void onResponse(Call<ClientDocumentsResponse> call, Response<ClientDocumentsResponse> response) {
                pbDetailDocsLoading.setVisibility(View.GONE);
                swipeRefreshClientDetailDocs.setRefreshing(false);
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    renderClientDocuments(response.body());
                } else {
                    renderClientDocuments(null);
                    Toast.makeText(getContext(), "Failed to load documents", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ClientDocumentsResponse> call, Throwable t) {
                pbDetailDocsLoading.setVisibility(View.GONE);
                swipeRefreshClientDetailDocs.setRefreshing(false);
                if (!isAdded()) return;

                renderClientDocuments(null);
                Toast.makeText(getContext(), "Network error loading documents", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderClientDocuments(@Nullable ClientDocumentsResponse data) {
        ClientDocumentsResponse.DocRecord aadharDoc = null;
        ClientDocumentsResponse.DocRecord panDoc = null;
        ClientDocumentsResponse.DocRecord balanceDoc = null;
        ClientDocumentsResponse.DocRecord profitLossDoc = null;

        if (data != null && data.getImportantDocuments() != null) {
            for (ClientDocumentsResponse.DocRecord rec : data.getImportantDocuments()) {
                String type = rec.getDocType();
                if ("Aadhar Card".equalsIgnoreCase(type)) {
                    aadharDoc = rec;
                } else if ("PAN Card".equalsIgnoreCase(type)) {
                    panDoc = rec;
                } else if ("Balance Sheet".equalsIgnoreCase(type)) {
                    balanceDoc = rec;
                } else if ("Profit and Loss".equalsIgnoreCase(type)) {
                    profitLossDoc = rec;
                }
            }
        }

        // 1. AADHAR CARD (KYC - Uploaded by Client)
        if (aadharDoc != null && aadharDoc.hasFile()) {
            aadharDocId = aadharDoc.getDocId();
            tvAdminAadharStatus.setText("Uploaded ✓");
            tvAdminAadharStatus.setTextColor(Color.parseColor("#059669"));
            tvAdminAadharStatus.setBackgroundColor(Color.parseColor("#ECFDF5"));
            tvAdminAadharMeta.setText("Uploaded • " + formatDate(aadharDoc.getCreatedAt()));
            layoutAdminAadharActions.setVisibility(View.VISIBLE);
        } else {
            aadharDocId = -1;
            tvAdminAadharStatus.setText("Not Uploaded");
            tvAdminAadharStatus.setTextColor(Color.parseColor("#D97706"));
            tvAdminAadharStatus.setBackgroundColor(Color.parseColor("#FEF3C7"));
            tvAdminAadharMeta.setText("Not uploaded yet");
            layoutAdminAadharActions.setVisibility(View.GONE);
        }

        // 2. PAN CARD (KYC - Uploaded by Client)
        if (panDoc != null && panDoc.hasFile()) {
            panDocId = panDoc.getDocId();
            tvAdminPanStatus.setText("Uploaded ✓");
            tvAdminPanStatus.setTextColor(Color.parseColor("#059669"));
            tvAdminPanStatus.setBackgroundColor(Color.parseColor("#ECFDF5"));
            tvAdminPanMeta.setText("Uploaded • " + formatDate(panDoc.getCreatedAt()));
            layoutAdminPanActions.setVisibility(View.VISIBLE);
        } else {
            panDocId = -1;
            tvAdminPanStatus.setText("Not Uploaded");
            tvAdminPanStatus.setTextColor(Color.parseColor("#D97706"));
            tvAdminPanStatus.setBackgroundColor(Color.parseColor("#FEF3C7"));
            tvAdminPanMeta.setText("Not uploaded yet");
            layoutAdminPanActions.setVisibility(View.GONE);
        }

        // 3. BALANCE SHEET (Financial Sheet)
        if (balanceDoc != null && balanceDoc.hasFile()) {
            balanceSheetDocId = balanceDoc.getDocId();
            tvAdminBalanceSheetStatus.setText("PDF Ready ✓");
            tvAdminBalanceSheetStatus.setTextColor(Color.parseColor("#059669"));
            tvAdminBalanceSheetStatus.setBackgroundColor(Color.parseColor("#ECFDF5"));
            tvAdminBalanceSheetMeta.setText("Uploaded • " + formatDate(balanceDoc.getCreatedAt()));
            layoutAdminBalanceSheetActions.setVisibility(View.VISIBLE);
            btnAdminUploadBalanceSheet.setText("🔄 Replace PDF");
        } else {
            balanceSheetDocId = -1;
            tvAdminBalanceSheetStatus.setText("Pending");
            tvAdminBalanceSheetStatus.setTextColor(Color.parseColor("#64748B"));
            tvAdminBalanceSheetStatus.setBackgroundColor(Color.parseColor("#F1F5F9"));
            tvAdminBalanceSheetMeta.setText("Annual Statement (PDF)");
            layoutAdminBalanceSheetActions.setVisibility(View.GONE);
            btnAdminUploadBalanceSheet.setText("📤 Upload PDF");
        }

        // 4. PROFIT & LOSS STATEMENT (Financial Sheet)
        if (profitLossDoc != null && profitLossDoc.hasFile()) {
            profitLossDocId = profitLossDoc.getDocId();
            tvAdminProfitLossStatus.setText("PDF Ready ✓");
            tvAdminProfitLossStatus.setTextColor(Color.parseColor("#059669"));
            tvAdminProfitLossStatus.setBackgroundColor(Color.parseColor("#ECFDF5"));
            tvAdminProfitLossMeta.setText("Uploaded • " + formatDate(profitLossDoc.getCreatedAt()));
            layoutAdminProfitLossActions.setVisibility(View.VISIBLE);
            btnAdminUploadProfitLoss.setText("🔄 Replace PDF");
        } else {
            profitLossDocId = -1;
            tvAdminProfitLossStatus.setText("Pending");
            tvAdminProfitLossStatus.setTextColor(Color.parseColor("#64748B"));
            tvAdminProfitLossStatus.setBackgroundColor(Color.parseColor("#F1F5F9"));
            tvAdminProfitLossMeta.setText("Performance Statement (PDF)");
            layoutAdminProfitLossActions.setVisibility(View.GONE);
            btnAdminUploadProfitLoss.setText("📤 Upload PDF");
        }

        // 5. OTHER CLIENT DOCUMENTS
        layoutAdminOtherDocsContainer.removeAllViews();
        List<ClientDocumentsResponse.DocRecord> otherList = data != null ? data.getOtherDocuments() : null;
        int otherCount = 0;

        if (otherList != null) {
            for (ClientDocumentsResponse.DocRecord other : otherList) {
                if (other.hasFile()) {
                    addOtherDocCard(other);
                    otherCount++;
                }
            }
        }

        tvAdminNoOtherDocs.setVisibility(otherCount == 0 ? View.VISIBLE : View.GONE);
    }

    private void addOtherDocCard(ClientDocumentsResponse.DocRecord other) {
        if (getContext() == null) return;
        View card = LayoutInflater.from(getContext()).inflate(R.layout.item_client_sheet, layoutAdminOtherDocsContainer, false);

        TextView tvIcon = card.findViewById(R.id.tvSheetIcon);
        TextView tvName = card.findViewById(R.id.tvSheetName);
        TextView tvMeta = card.findViewById(R.id.tvSheetMeta);
        TextView tvStatus = card.findViewById(R.id.tvSheetStatus);
        LinearLayout layoutActions = card.findViewById(R.id.layoutSheetActions);
        MaterialButton btnView = card.findViewById(R.id.btnSheetView);
        MaterialButton btnDownload = card.findViewById(R.id.btnSheetDownload);

        boolean isPdf = "application/pdf".equalsIgnoreCase(other.getMimeType());
        tvIcon.setText(isPdf ? "📄" : "🖼️");
        tvName.setText(other.getDocName());
        tvMeta.setText("Uploaded • " + formatDate(other.getCreatedAt()) + " (" + (isPdf ? "PDF" : "Image") + ")");

        tvStatus.setText(isPdf ? "PDF" : "IMAGE");
        tvStatus.setTextColor(Color.parseColor(isPdf ? "#DC2626" : "#2563EB"));
        tvStatus.setBackgroundColor(Color.parseColor(isPdf ? "#FEF2F2" : "#EFF6FF"));

        btnView.setText("👁 View");
        btnDownload.setText("⬇ Download");

        btnView.setOnClickListener(v -> fetchDoc("other", other.getDocId(), false));
        btnDownload.setOnClickListener(v -> fetchDoc("other", other.getDocId(), true));

        // Use the pre-aligned Delete button inside layoutSheetActions (weightSum=3)
        MaterialButton btnDelete = card.findViewById(R.id.btnSheetDelete);
        if (btnDelete != null) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> confirmDeleteDoc("other", other.getDocId(), other.getDocName()));
        }

        layoutAdminOtherDocsContainer.addView(card);
    }

    private void confirmDeleteDoc(String source, int docId, String title) {
        if (getContext() == null || docId <= 0) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Delete " + title)
                .setMessage("Are you sure you want to delete '" + title + "' for this client?")
                .setPositiveButton("Delete", (dialog, which) -> executeDeleteDoc(source, docId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeDeleteDoc(String source, int docId) {
        if (selectedClient == null) return;
        pbDetailDocsLoading.setVisibility(View.VISIBLE);

        ApiClient.getService().deleteDocument(source, docId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                pbDetailDocsLoading.setVisibility(View.GONE);
                if (!isAdded()) return;

                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Document deleted successfully", Toast.LENGTH_SHORT).show();
                    loadDocumentsForClient(selectedClient.getClientId());
                } else {
                    Toast.makeText(getContext(), "Failed to delete document", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                pbDetailDocsLoading.setVisibility(View.GONE);
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Network error deleting document", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =========================================================================
    // UPLOAD: FINANCIAL STATEMENTS (BALANCE SHEET & P&L)
    // =========================================================================

    private void uploadFinancialPdf(Uri uri, String docType) {
        if (getContext() == null || selectedClient == null) return;

        ContentResolver resolver = requireContext().getContentResolver();
        String mimeType = resolver.getType(uri);
        if (mimeType != null && !mimeType.equalsIgnoreCase("application/pdf")) {
            Toast.makeText(getContext(), "Only PDF files are allowed for " + docType + "!", Toast.LENGTH_LONG).show();
            return;
        }

        pbDetailDocsLoading.setVisibility(View.VISIBLE);

        try (InputStream is = resolver.openInputStream(uri)) {
            if (is == null) {
                pbDetailDocsLoading.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Unable to open PDF file", Toast.LENGTH_SHORT).show();
                return;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) baos.write(buf, 0, n);
            byte[] bytes = baos.toByteArray();

            if (bytes.length == 0) {
                pbDetailDocsLoading.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Selected file is empty", Toast.LENGTH_SHORT).show();
                return;
            }

            String dataUri = "data:application/pdf;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
            SheetUploadRequest req = new SheetUploadRequest(selectedClient.getClientId(), docType, docType, dataUri, "application/pdf");

            ApiClient.getService().uploadPdfSheet(req).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    pbDetailDocsLoading.setVisibility(View.GONE);
                    if (!isAdded()) return;

                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), docType + " uploaded successfully! Client can now view it.", Toast.LENGTH_LONG).show();
                        loadDocumentsForClient(selectedClient.getClientId());
                    } else {
                        Toast.makeText(getContext(), "Upload failed (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    pbDetailDocsLoading.setVisibility(View.GONE);
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Network error uploading PDF: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            pbDetailDocsLoading.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Error reading PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // POPUP / DIALOG: UPLOAD OTHER DOCUMENT
    // =========================================================================

    private void showUploadOtherDocPopup() {
        if (getContext() == null || selectedClient == null) return;

        pendingOtherFileUri = null;
        Dialog dialog = new Dialog(getContext());
        activeOtherDocDialog = dialog;

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_upload_other_doc);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvSubtitle = dialog.findViewById(R.id.tvUploadPopupSubtitle);
        tvSubtitle.setText("For client: " + selectedClient.getDisplayName());

        TextInputLayout tilDocName = dialog.findViewById(R.id.tilOtherDocName);
        TextInputEditText etDocName = dialog.findViewById(R.id.etOtherDocName);
        MaterialButton btnChooseFile = dialog.findViewById(R.id.btnChooseOtherFile);
        LinearLayout layoutPreview = dialog.findViewById(R.id.layoutSelectedFilePreview);
        ProgressBar pbUpload = dialog.findViewById(R.id.pbUploadOtherProgress);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancelOtherUpload);
        MaterialButton btnSubmit = dialog.findViewById(R.id.btnSubmitOtherUpload);

        btnChooseFile.setOnClickListener(v -> {
            otherFilePickerLauncher.launch(new String[]{"application/pdf", "image/png", "image/jpeg", "image/jpg"});
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            String docName = etDocName.getText() != null ? etDocName.getText().toString().trim() : "";
            if (docName.isEmpty()) {
                tilDocName.setError("Document name is required");
                return;
            }
            tilDocName.setError(null);

            if (pendingOtherFileUri == null) {
                Toast.makeText(getContext(), "Please choose a file to upload", Toast.LENGTH_SHORT).show();
                return;
            }

            // Perform upload
            performOtherDocUpload(dialog, docName, pendingOtherFileUri, pbUpload, btnSubmit, btnCancel);
        });

        dialog.show();
    }

    private void updateOtherDialogFilePreview(Dialog dialog, Uri uri) {
        if (dialog == null || !dialog.isShowing()) return;

        LinearLayout layoutPreview = dialog.findViewById(R.id.layoutSelectedFilePreview);
        TextView tvIcon = dialog.findViewById(R.id.tvSelectedFileIcon);
        TextView tvName = dialog.findViewById(R.id.tvSelectedFileName);
        TextView tvSize = dialog.findViewById(R.id.tvSelectedFileSize);
        MaterialButton btnChoose = dialog.findViewById(R.id.btnChooseOtherFile);

        ContentResolver cr = requireContext().getContentResolver();
        String mimeType = cr.getType(uri);
        boolean isPdf = mimeType != null && mimeType.equalsIgnoreCase("application/pdf");

        String fileName = "selected_document";
        long fileSize = 0;

        try (Cursor cursor = cr.query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (nameIdx >= 0) fileName = cursor.getString(nameIdx);
                if (sizeIdx >= 0) fileSize = cursor.getLong(sizeIdx);
            }
        } catch (Exception ignored) {}

        layoutPreview.setVisibility(View.VISIBLE);
        tvIcon.setText(isPdf ? "📄" : "🖼️");
        tvName.setText(fileName);
        tvSize.setText(String.format("Ready to upload (%.1f KB)", fileSize / 1024.0));
        btnChoose.setText("🔄 Change File");
    }

    private void performOtherDocUpload(Dialog dialog, String docName, Uri uri, ProgressBar pb, MaterialButton btnSubmit, MaterialButton btnCancel) {
        if (selectedClient == null) return;

        pb.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);
        btnCancel.setEnabled(false);

        try {
            ContentResolver cr = requireContext().getContentResolver();
            String mimeType = cr.getType(uri);
            if (mimeType == null || mimeType.isEmpty()) {
                mimeType = "application/pdf";
            }

            InputStream is = cr.openInputStream(uri);
            if (is == null) {
                pb.setVisibility(View.GONE);
                btnSubmit.setEnabled(true);
                btnCancel.setEnabled(true);
                Toast.makeText(getContext(), "Could not read file", Toast.LENGTH_SHORT).show();
                return;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) baos.write(buf, 0, n);
            byte[] bytes = baos.toByteArray();
            is.close();

            if (bytes.length == 0) {
                pb.setVisibility(View.GONE);
                btnSubmit.setEnabled(true);
                btnCancel.setEnabled(true);
                Toast.makeText(getContext(), "Selected file is empty", Toast.LENGTH_SHORT).show();
                return;
            }

            String dataUri = "data:" + mimeType + ";base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
            SheetUploadRequest req = new SheetUploadRequest(selectedClient.getClientId(), null, docName, dataUri, mimeType);

            ApiClient.getService().uploadPdfSheet(req).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    pb.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    btnCancel.setEnabled(true);
                    if (!isAdded()) return;

                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), "'" + docName + "' uploaded successfully! Client can view it now.", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                        loadDocumentsForClient(selectedClient.getClientId());
                    } else {
                        Toast.makeText(getContext(), "Upload failed (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    pb.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    btnCancel.setEnabled(true);
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Network error uploading file: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            pb.setVisibility(View.GONE);
            btnSubmit.setEnabled(true);
            btnCancel.setEnabled(true);
            Toast.makeText(getContext(), "Error preparing upload: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // VIEW & DOWNLOAD HELPER
    // =========================================================================

    private void fetchDoc(String source, int docId, boolean downloadDirectly) {
        pbDetailDocsLoading.setVisibility(View.VISIBLE);
        ApiClient.getService().downloadDocument(source, docId).enqueue(new Callback<DocumentDownloadResponse>() {
            @Override
            public void onResponse(Call<DocumentDownloadResponse> call, Response<DocumentDownloadResponse> response) {
                pbDetailDocsLoading.setVisibility(View.GONE);
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    if (downloadDirectly) {
                        DocumentViewerHelper.saveDocumentToDownloads(getContext(), response.body());
                    } else {
                        DocumentViewerHelper.showDocumentViewerPopup(getContext(), response.body());
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to retrieve document", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DocumentDownloadResponse> call, Throwable t) {
                pbDetailDocsLoading.setVisibility(View.GONE);
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Network error retrieving document", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatDate(String isoDate) {
        if (isoDate == null) return "Recent";
        if (isoDate.length() >= 10) return isoDate.substring(0, 10);
        return isoDate;
    }
}
