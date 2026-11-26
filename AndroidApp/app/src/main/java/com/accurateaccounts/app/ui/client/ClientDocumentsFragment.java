package com.accurateaccounts.app.ui.client;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.accurateaccounts.app.R;
import com.accurateaccounts.app.api.ApiClient;
import com.accurateaccounts.app.models.ClientDocumentsResponse;
import com.accurateaccounts.app.models.DocumentDownloadResponse;
import com.accurateaccounts.app.models.KycUploadRequest;
import com.accurateaccounts.app.ui.common.DocumentViewerHelper;
import com.accurateaccounts.app.utils.SessionManager;
import com.accurateaccounts.app.utils.SmoothNestedScrollView;
import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClientDocumentsFragment extends Fragment {

    private SessionManager sessionManager;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar pbLoading;
    private LinearLayout layoutSheetsContainer;

    private TextView tvPanStatus, tvPanMeta, tvAadharStatus, tvAadharMeta;
    private MaterialButton btnUploadPan, btnViewPan, btnDeletePan, btnUploadAadhar, btnViewAadhar, btnDeleteAadhar;

    private String pendingUploadDocType = null;
    private int panDocId = -1;
    private int aadharDocId = -1;

    private final ActivityResultLauncher<String[]> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null && pendingUploadDocType != null) {
                    uploadSelectedKycImage(uri, pendingUploadDocType);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_client_documents, container, false);

        sessionManager = new SessionManager(requireContext());

        swipeRefresh = view.findViewById(R.id.swipeRefreshClientDocs);
        SmoothNestedScrollView scrollView = view.findViewById(R.id.scrollClientDocs);
        pbLoading = view.findViewById(R.id.pbDocsScreenLoading);
        layoutSheetsContainer = view.findViewById(R.id.layoutFinancialSheetsContainer);

        tvPanStatus = view.findViewById(R.id.tvPanStatus);
        tvPanMeta = view.findViewById(R.id.tvPanMeta);
        btnUploadPan = view.findViewById(R.id.btnUploadPan);
        btnViewPan = view.findViewById(R.id.btnViewPan);
        btnDeletePan = view.findViewById(R.id.btnDeletePan);

        tvAadharStatus = view.findViewById(R.id.tvAadharStatus);
        tvAadharMeta = view.findViewById(R.id.tvAadharMeta);
        btnUploadAadhar = view.findViewById(R.id.btnUploadAadhar);
        btnViewAadhar = view.findViewById(R.id.btnViewAadhar);
        btnDeleteAadhar = view.findViewById(R.id.btnDeleteAadhar);

        if (swipeRefresh != null && scrollView != null) {
            swipeRefresh.setOnChildScrollUpCallback((parent, child) -> scrollView.canScrollVertically(-1));
        }

        // Strict PNG / JPEG file picker for PAN Card & Aadhar Card
        btnUploadPan.setOnClickListener(v -> {
            pendingUploadDocType = "PAN Card";
            imagePickerLauncher.launch(new String[]{"image/png", "image/jpeg"});
        });

        btnUploadAadhar.setOnClickListener(v -> {
            pendingUploadDocType = "Aadhar Card";
            imagePickerLauncher.launch(new String[]{"image/png", "image/jpeg"});
        });

        btnViewPan.setOnClickListener(v -> {
            if (panDocId > 0) fetchAndHandleDocument("important", panDocId, false);
        });

        btnViewAadhar.setOnClickListener(v -> {
            if (aadharDocId > 0) fetchAndHandleDocument("important", aadharDocId, false);
        });

        btnDeletePan.setOnClickListener(v -> {
            if (panDocId > 0) confirmDeleteClientDoc("important", panDocId, "PAN Card");
        });

        btnDeleteAadhar.setOnClickListener(v -> {
            if (aadharDocId > 0) confirmDeleteClientDoc("important", aadharDocId, "Aadhar Card");
        });

        swipeRefresh.setOnRefreshListener(this::loadDocuments);
        loadDocuments();

        return view;
    }

    private void loadDocuments() {
        int clientId = sessionManager.getClientId();
        if (clientId <= 0) clientId = 1;

        pbLoading.setVisibility(View.VISIBLE);
        ApiClient.getService().getClientDocuments(clientId).enqueue(new Callback<ClientDocumentsResponse>() {
            @Override
            public void onResponse(Call<ClientDocumentsResponse> call, Response<ClientDocumentsResponse> response) {
                pbLoading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    renderDocuments(response.body());
                } else {
                    renderDocuments(null);
                    Toast.makeText(getContext(), "Failed to load documents", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ClientDocumentsResponse> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                if (!isAdded()) return;
                renderDocuments(null);
                Toast.makeText(getContext(), "Connection error loading documents", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderDocuments(@Nullable ClientDocumentsResponse data) {
        layoutSheetsContainer.removeAllViews();

        ClientDocumentsResponse.DocRecord balanceSheet = null;
        ClientDocumentsResponse.DocRecord profitLoss = null;
        ClientDocumentsResponse.DocRecord capitalAccount = null;
        ClientDocumentsResponse.DocRecord panDoc = null;
        ClientDocumentsResponse.DocRecord aadharDoc = null;

        if (data != null && data.getImportantDocuments() != null) {
            for (ClientDocumentsResponse.DocRecord rec : data.getImportantDocuments()) {
                String type = rec.getDocType();
                if ("Balance Sheet".equalsIgnoreCase(type)) {
                    balanceSheet = rec;
                } else if ("Profit and Loss".equalsIgnoreCase(type)) {
                    profitLoss = rec;
                } else if ("Capital Account".equalsIgnoreCase(type)) {
                    capitalAccount = rec;
                } else if ("PAN Card".equalsIgnoreCase(type)) {
                    panDoc = rec;
                } else if ("Aadhar Card".equalsIgnoreCase(type)) {
                    aadharDoc = rec;
                }
            }
        }

        // Must-have Financial Sheets (Balance Sheet & Profit and Loss) — View & Download ONLY
        addFinancialSheetCard("📊", "Balance Sheet", "important", balanceSheet);
        addFinancialSheetCard("📈", "Profit & Loss Sheet", "important", profitLoss);

        if (capitalAccount != null && capitalAccount.hasFile()) {
            addFinancialSheetCard("📑", "Capital Account", "important", capitalAccount);
        }

        // Any other sheets uploaded by Admin & Employees
        if (data != null && data.getOtherDocuments() != null) {
            for (ClientDocumentsResponse.DocRecord other : data.getOtherDocuments()) {
                addFinancialSheetCard("📄", other.getDocName(), "other", other);
            }
        }

        // Update PAN Card KYC status
        if (panDoc != null && panDoc.hasFile()) {
            panDocId = panDoc.getDocId();
            tvPanStatus.setText("Uploaded ✓");
            tvPanStatus.setTextColor(Color.parseColor("#059669"));
            tvPanStatus.setBackgroundColor(Color.parseColor("#ECFDF5"));
            tvPanMeta.setText("Uploaded on " + formatDate(panDoc.getCreatedAt()));
            btnUploadPan.setText("🔄 Re-upload PAN");
            btnViewPan.setVisibility(View.VISIBLE);
            btnDeletePan.setVisibility(View.VISIBLE);
        } else {
            panDocId = -1;
            tvPanStatus.setText("Not Uploaded");
            tvPanStatus.setTextColor(Color.parseColor("#D97706"));
            tvPanStatus.setBackgroundColor(Color.parseColor("#FEF3C7"));
            tvPanMeta.setText("Accepted format: PNG or JPEG");
            btnUploadPan.setText("📤 Upload PAN (PNG/JPEG)");
            btnViewPan.setVisibility(View.GONE);
            btnDeletePan.setVisibility(View.GONE);
        }

        // Update Aadhar Card KYC status
        if (aadharDoc != null && aadharDoc.hasFile()) {
            aadharDocId = aadharDoc.getDocId();
            tvAadharStatus.setText("Uploaded ✓");
            tvAadharStatus.setTextColor(Color.parseColor("#059669"));
            tvAadharStatus.setBackgroundColor(Color.parseColor("#ECFDF5"));
            tvAadharMeta.setText("Uploaded on " + formatDate(aadharDoc.getCreatedAt()));
            btnUploadAadhar.setText("🔄 Re-upload Aadhar");
            btnViewAadhar.setVisibility(View.VISIBLE);
            btnDeleteAadhar.setVisibility(View.VISIBLE);
        } else {
            aadharDocId = -1;
            tvAadharStatus.setText("Not Uploaded");
            tvAadharStatus.setTextColor(Color.parseColor("#D97706"));
            tvAadharStatus.setBackgroundColor(Color.parseColor("#FEF3C7"));
            tvAadharMeta.setText("Accepted format: PNG or JPEG");
            btnUploadAadhar.setText("📤 Upload Aadhar (PNG/JPEG)");
            btnViewAadhar.setVisibility(View.GONE);
            btnDeleteAadhar.setVisibility(View.GONE);
        }
    }

    private void addFinancialSheetCard(String icon, String displayTitle, String source, @Nullable ClientDocumentsResponse.DocRecord record) {
        if (getContext() == null) return;
        View card = LayoutInflater.from(getContext()).inflate(R.layout.item_client_sheet, layoutSheetsContainer, false);

        TextView tvIcon = card.findViewById(R.id.tvSheetIcon);
        TextView tvName = card.findViewById(R.id.tvSheetName);
        TextView tvMeta = card.findViewById(R.id.tvSheetMeta);
        TextView tvStatus = card.findViewById(R.id.tvSheetStatus);
        LinearLayout layoutActions = card.findViewById(R.id.layoutSheetActions);
        MaterialButton btnView = card.findViewById(R.id.btnSheetView);
        MaterialButton btnDownload = card.findViewById(R.id.btnSheetDownload);
        MaterialButton btnDelete = card.findViewById(R.id.btnSheetDelete);

        tvIcon.setText(icon);
        tvName.setText(displayTitle);

        boolean available = (record != null && record.hasFile());
        if (available) {
            boolean isPdf = "application/pdf".equalsIgnoreCase(record.getMimeType());
            tvStatus.setText(isPdf ? "PDF Ready" : "Available");
            tvStatus.setTextColor(Color.parseColor("#059669"));
            tvStatus.setBackgroundColor(Color.parseColor("#ECFDF5"));
            tvMeta.setText((isPdf ? "PDF uploaded by Admin / Staff • " : "Uploaded by Admin / Staff • ") + formatDate(record.getCreatedAt()));
            layoutActions.setVisibility(View.VISIBLE);

            btnView.setText(isPdf ? "👁 View PDF" : "👁 View");
            btnDownload.setText(isPdf ? "⬇ Download PDF" : "⬇ Download");

            final int docId = record.getDocId();
            btnView.setOnClickListener(v -> fetchAndHandleDocument(source, docId, false));
            btnDownload.setOnClickListener(v -> fetchAndHandleDocument(source, docId, true));

            // Clients cannot delete financial sheets or other documents (Admin/Staff only)
            if (btnDelete != null) {
                btnDelete.setVisibility(View.GONE);
            }
            if (layoutActions != null) {
                layoutActions.setWeightSum(2.0f);
            }
        } else {
            tvStatus.setText("Pending");
            tvStatus.setTextColor(Color.parseColor("#D97706"));
            tvStatus.setBackgroundColor(Color.parseColor("#FEF3C7"));
            tvMeta.setText("Will be available once uploaded by Admin / Employee");
            layoutActions.setVisibility(View.GONE);
            if (btnDelete != null) {
                btnDelete.setVisibility(View.GONE);
            }
        }

        layoutSheetsContainer.addView(card);
    }

    private void confirmDeleteClientDoc(String source, int docId, String title) {
        if (getContext() == null || docId <= 0) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Delete " + title)
                .setMessage("Are you sure you want to delete '" + title + "'?")
                .setPositiveButton("Delete", (dialog, which) -> executeDeleteClientDoc(source, docId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeDeleteClientDoc(String source, int docId) {
        pbLoading.setVisibility(View.VISIBLE);
        ApiClient.getService().deleteDocument(source, docId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                pbLoading.setVisibility(View.GONE);
                if (!isAdded()) return;

                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Document deleted successfully", Toast.LENGTH_SHORT).show();
                    loadDocuments();
                } else {
                    Toast.makeText(getContext(), "Failed to delete document", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Connection error deleting document", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadSelectedKycImage(Uri uri, String docType) {
        if (getContext() == null) return;

        ContentResolver resolver = requireContext().getContentResolver();
        String mimeType = resolver.getType(uri);
        if (mimeType == null) mimeType = "image/jpeg";

        if (!mimeType.equalsIgnoreCase("image/png")
                && !mimeType.equalsIgnoreCase("image/jpeg")
                && !mimeType.equalsIgnoreCase("image/jpg")) {
            Toast.makeText(getContext(), "Only PNG or JPEG images are allowed!", Toast.LENGTH_LONG).show();
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);
        try (InputStream is = resolver.openInputStream(uri)) {
            if (is == null) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Could not read selected image", Toast.LENGTH_SHORT).show();
                return;
            }

            Bitmap original = BitmapFactory.decodeStream(is);
            if (original == null) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Invalid image file. Please select a valid PNG or JPEG.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Resize cleanly if larger than 1600px on longest side for fast, crisp uploads
            int maxDim = 1600;
            Bitmap bitmap = original;
            if (original.getWidth() > maxDim || original.getHeight() > maxDim) {
                float scale = Math.min((float) maxDim / original.getWidth(), (float) maxDim / original.getHeight());
                int newW = Math.round(original.getWidth() * scale);
                int newH = Math.round(original.getHeight() * scale);
                bitmap = Bitmap.createScaledBitmap(original, newW, newH, true);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean isPng = mimeType.equalsIgnoreCase("image/png");
            bitmap.compress(isPng ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 85, baos);
            String base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
            String finalMime = isPng ? "image/png" : "image/jpeg";
            String dataUri = "data:" + finalMime + ";base64," + base64;

            int clientId = sessionManager.getClientId();
            if (clientId <= 0) clientId = 1;

            KycUploadRequest req = new KycUploadRequest(clientId, docType, dataUri, finalMime);
            ApiClient.getService().uploadKycDocument(req).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    pbLoading.setVisibility(View.GONE);
                    if (!isAdded()) return;

                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), docType + " uploaded successfully!", Toast.LENGTH_SHORT).show();
                        loadDocuments();
                    } else {
                        Toast.makeText(getContext(), "Upload failed (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    pbLoading.setVisibility(View.GONE);
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Network error during upload", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            pbLoading.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Failed to process image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchAndHandleDocument(String source, int docId, boolean downloadDirectly) {
        pbLoading.setVisibility(View.VISIBLE);
        ApiClient.getService().downloadDocument(source, docId).enqueue(new Callback<DocumentDownloadResponse>() {
            @Override
            public void onResponse(Call<DocumentDownloadResponse> call, Response<DocumentDownloadResponse> response) {
                pbLoading.setVisibility(View.GONE);
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    DocumentDownloadResponse doc = response.body();
                    if (downloadDirectly) {
                        DocumentViewerHelper.saveDocumentToDownloads(getContext(), doc);
                    } else {
                        DocumentViewerHelper.showDocumentViewerPopup(getContext(), doc);
                    }
                } else {
                    Toast.makeText(getContext(), "Document could not be retrieved", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DocumentDownloadResponse> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Network error fetching document", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDocumentViewerPopup(DocumentDownloadResponse doc) {
        if (getContext() == null) return;

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_document_viewer, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ImageButton btnBack = view.findViewById(R.id.btnDocViewerBack);
        TextView tvTitle = view.findViewById(R.id.tvDocViewerTitle);
        ImageView ivImage = view.findViewById(R.id.ivDocViewerImage);
        TextView tvText = view.findViewById(R.id.tvDocViewerText);
        MaterialButton btnDownload = view.findViewById(R.id.btnDocViewerDownload);

        tvTitle.setText(doc.getTitle() != null ? doc.getTitle() : "Document");
        btnBack.setOnClickListener(v -> dialog.dismiss());

        String mime = doc.getMimeType() != null ? doc.getMimeType() : "text/plain";
        if (doc.isBase64() && mime.startsWith("image/")) {
            try {
                byte[] decoded = Base64.decode(doc.getContent(), Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                if (bmp != null) {
                    ivImage.setImageBitmap(bmp);
                    ivImage.setVisibility(View.VISIBLE);
                    tvText.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                tvText.setText("Unable to preview image. Please tap Download.");
                tvText.setVisibility(View.VISIBLE);
            }
        } else {
            tvText.setText(doc.getContent() != null ? doc.getContent() : "");
            tvText.setVisibility(View.VISIBLE);
            ivImage.setVisibility(View.GONE);
        }

        btnDownload.setOnClickListener(v -> saveDocumentToDownloads(doc));

        dialog.show();
    }

    private void saveDocumentToDownloads(DocumentDownloadResponse doc) {
        if (getContext() == null) return;

        try {
            String mime = doc.getMimeType() != null ? doc.getMimeType() : "text/plain";
            String ext = ".txt";
            if (mime.equalsIgnoreCase("image/png")) ext = ".png";
            else if (mime.equalsIgnoreCase("image/jpeg") || mime.equalsIgnoreCase("image/jpg")) ext = ".jpg";
            else if (mime.equalsIgnoreCase("application/pdf")) ext = ".pdf";

            String safeName = (doc.getTitle() != null ? doc.getTitle() : "Document")
                    .replaceAll("[^a-zA-Z0-9._-]", "_");
            String fileName = "AAS_" + safeName + "_" + System.currentTimeMillis() + ext;

            byte[] fileBytes = doc.isBase64()
                    ? Base64.decode(doc.getContent(), Base64.DEFAULT)
                    : (doc.getContent() != null ? doc.getContent().getBytes(StandardCharsets.UTF_8) : new byte[0]);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, mime);
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream os = requireContext().getContentResolver().openOutputStream(uri)) {
                        if (os != null) os.write(fileBytes);
                    }
                }
            } else {
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!dir.exists()) dir.mkdirs();
                File outFile = new File(dir, fileName);
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    fos.write(fileBytes);
                }
            }

            Toast.makeText(getContext(), "Downloaded to Downloads/" + fileName, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String formatDate(String iso) {
        if (iso != null && iso.length() >= 10) {
            return iso.substring(0, 10);
        }
        return iso != null ? iso : "";
    }
}

