package com.accurateaccounts.app.ui.common;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.accurateaccounts.app.R;
import com.accurateaccounts.app.models.DocumentDownloadResponse;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class DocumentViewerHelper {

    public static void showDocumentViewerPopup(Context context, DocumentDownloadResponse doc) {
        if (context == null || doc == null) return;

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_document_viewer, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.94);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ImageButton btnBack = view.findViewById(R.id.btnDocViewerBack);
        TextView tvTitle = view.findViewById(R.id.tvDocViewerTitle);
        TextView tvBadge = view.findViewById(R.id.tvDocViewerBadge);
        LinearLayout layoutPdfPages = view.findViewById(R.id.layoutPdfPagesContainer);
        ImageView ivImage = view.findViewById(R.id.ivDocViewerImage);
        TextView tvText = view.findViewById(R.id.tvDocViewerText);
        MaterialButton btnDownload = view.findViewById(R.id.btnDocViewerDownload);

        tvTitle.setText(doc.getTitle() != null ? doc.getTitle() : "Document");
        btnBack.setOnClickListener(v -> dialog.dismiss());

        String mime = doc.getMimeType() != null ? doc.getMimeType().toLowerCase() : "text/plain";

        if (doc.isBase64() && mime.equals("application/pdf")) {
            tvBadge.setText("PDF Document • Scroll pages");
            btnDownload.setText("⬇ Download PDF");
            renderPdfPages(context, doc.getContent(), layoutPdfPages, tvText);
        } else if (doc.isBase64() && mime.startsWith("image/")) {
            tvBadge.setText(mime.equals("image/png") ? "PNG Image" : "JPEG Image");
            btnDownload.setText("⬇ Download Image");
            try {
                byte[] decoded = Base64.decode(doc.getContent(), Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                if (bmp != null) {
                    ivImage.setImageBitmap(bmp);
                    ivImage.setVisibility(View.VISIBLE);
                } else {
                    tvText.setText("Unable to preview image. Tap Download to save.");
                    tvText.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                tvText.setText("Unable to preview image. Tap Download to save.");
                tvText.setVisibility(View.VISIBLE);
            }
        } else {
            tvBadge.setText("Document Report");
            btnDownload.setText("⬇ Download to Device");
            tvText.setText(doc.getContent() != null ? doc.getContent() : "");
            tvText.setVisibility(View.VISIBLE);
        }

        btnDownload.setOnClickListener(v -> saveDocumentToDownloads(context, doc));

        dialog.show();
    }

    private static void renderPdfPages(Context context, String base64Pdf, LinearLayout container, TextView fallbackText) {
        File tempFile = null;
        ParcelFileDescriptor pfd = null;
        PdfRenderer renderer = null;
        try {
            byte[] pdfBytes = Base64.decode(base64Pdf, Base64.DEFAULT);
            tempFile = File.createTempFile("aas_preview_", ".pdf", context.getCacheDir());
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(pdfBytes);
            }

            pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY);
            renderer = new PdfRenderer(pfd);
            int pageCount = renderer.getPageCount();

            container.removeAllViews();
            container.setVisibility(View.VISIBLE);

            int targetWidth = Math.max(800, (int) (context.getResources().getDisplayMetrics().widthPixels * 0.88f));

            for (int i = 0; i < pageCount; i++) {
                PdfRenderer.Page page = renderer.openPage(i);
                int bmpWidth = targetWidth;
                int bmpHeight = Math.round((float) page.getHeight() * bmpWidth / Math.max(1, page.getWidth()));

                Bitmap bmp = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bmp);
                canvas.drawColor(Color.WHITE);
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();

                ImageView pageView = new ImageView(context);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                lp.bottomMargin = (int) (10 * context.getResources().getDisplayMetrics().density);
                pageView.setLayoutParams(lp);
                pageView.setAdjustViewBounds(true);
                pageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                pageView.setBackgroundColor(Color.WHITE);
                pageView.setElevation(4f);
                pageView.setImageBitmap(bmp);
                container.addView(pageView);

                if (pageCount > 1) {
                    TextView tvPage = new TextView(context);
                    tvPage.setText("Page " + (i + 1) + " of " + pageCount);
                    tvPage.setTextSize(11f);
                    tvPage.setTextColor(Color.parseColor("#64748B"));
                    tvPage.setPadding(0, 0, 0, (int) (12 * context.getResources().getDisplayMetrics().density));
                    container.addView(tvPage);
                }
            }
        } catch (Exception e) {
            container.setVisibility(View.GONE);
            fallbackText.setText("PDF ready to download. Tap 'Download PDF' below to open it on your device.");
            fallbackText.setVisibility(View.VISIBLE);
        } finally {
            try { if (renderer != null) renderer.close(); } catch (Exception ignored) {}
            try { if (pfd != null) pfd.close(); } catch (Exception ignored) {}
            try { if (tempFile != null) tempFile.delete(); } catch (Exception ignored) {}
        }
    }

    public static void saveDocumentToDownloads(Context context, DocumentDownloadResponse doc) {
        if (context == null || doc == null) return;

        try {
            String mime = doc.getMimeType() != null ? doc.getMimeType().toLowerCase() : "application/pdf";
            String ext = ".pdf";
            if (mime.equals("image/png")) ext = ".png";
            else if (mime.equals("image/jpeg") || mime.equals("image/jpg")) ext = ".jpg";
            else if (mime.equals("application/pdf")) ext = ".pdf";
            else ext = ".txt";

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

                Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
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

            Toast.makeText(context, "Saved to Downloads/" + fileName, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(context, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}

