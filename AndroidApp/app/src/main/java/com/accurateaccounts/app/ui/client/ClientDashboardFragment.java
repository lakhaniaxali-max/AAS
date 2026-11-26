package com.accurateaccounts.app.ui.client;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.accurateaccounts.app.R;
import com.accurateaccounts.app.api.ApiClient;
import com.accurateaccounts.app.models.ClientDocumentData;
import com.accurateaccounts.app.ui.common.FeedbackDialogHelper;
import com.accurateaccounts.app.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClientDashboardFragment extends Fragment {

    private static final long SLIDE_INTERVAL_MS = 5000L;

    private SessionManager sessionManager;
    private TextView tvWelcomeTitle;
    private TextView tvCompanyName;
    private ViewPager2 vpSlider;
    private LinearLayout layoutDots;

    private final int[] bannerImages = new int[]{
            R.drawable.client_slide_1,
            R.drawable.client_slide_2,
            R.drawable.client_slide_3,
            R.drawable.client_slide_4
    };

    private final Handler sliderHandler = new Handler(Looper.getMainLooper());
    private final Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (vpSlider != null && bannerImages.length > 0) {
                int next = (vpSlider.getCurrentItem() + 1) % bannerImages.length;
                vpSlider.setCurrentItem(next, true);
                sliderHandler.postDelayed(this, SLIDE_INTERVAL_MS);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_client_dashboard, container, false);

        sessionManager = new SessionManager(requireContext());

        tvWelcomeTitle = view.findViewById(R.id.tvClientWelcomeTitle);
        tvCompanyName = view.findViewById(R.id.tvClientCompanyName);
        vpSlider = view.findViewById(R.id.vpClientSlider);
        layoutDots = view.findViewById(R.id.layoutSliderDots);
        ImageButton btnPrev = view.findViewById(R.id.btnSliderPrev);
        ImageButton btnNext = view.findViewById(R.id.btnSliderNext);
        MaterialButton btnFeedback = view.findViewById(R.id.btnClientFeedback);

        // Styled Welcome Header with Golden/Yellow "AAS Organization"
        String welcomeHtml = "Welcome to <font color='#FACC15'>AAS Organization</font>";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tvWelcomeTitle.setText(Html.fromHtml(welcomeHtml, Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvWelcomeTitle.setText(Html.fromHtml(welcomeHtml));
        }

        String username = sessionManager.getUsername();
        String firstName = sessionManager.getFirstName();
        String initialDisplay = (firstName != null && !firstName.isEmpty()) ? firstName : username;
        if (initialDisplay != null && !initialDisplay.isEmpty()) {
            tvCompanyName.setText(initialDisplay);
        }

        // Setup 4-Photo Auto-Sliding Carousel with Smooth Zoom/Slide Transition
        ClientBannerAdapter adapter = new ClientBannerAdapter(bannerImages);
        vpSlider.setAdapter(adapter);
        vpSlider.setOffscreenPageLimit(1);

        // Smooth Sliding + Depth/Zoom-Out PageTransformer
        vpSlider.setPageTransformer((page, position) -> {
            int pageWidth = page.getWidth();
            if (position < -1) {
                page.setAlpha(0f);
            } else if (position <= 1) {
                float scaleFactor = Math.max(0.88f, 1 - Math.abs(position) * 0.12f);
                float horzMargin = pageWidth * (1 - scaleFactor) / 2f;
                if (position < 0) {
                    page.setTranslationX(horzMargin / 2f);
                } else {
                    page.setTranslationX(-horzMargin / 2f);
                }
                page.setScaleX(scaleFactor);
                page.setScaleY(scaleFactor);
                page.setAlpha(Math.max(0.65f, 1 - Math.abs(position) * 0.35f));
            } else {
                page.setAlpha(0f);
            }
        });

        setupDots(0);

        vpSlider.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                setupDots(position);
                // Reset 5-second auto-slide timer on swipe
                sliderHandler.removeCallbacks(sliderRunnable);
                sliderHandler.postDelayed(sliderRunnable, SLIDE_INTERVAL_MS);
            }
        });

        // Navigation Chevrons
        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> {
                if (vpSlider != null && bannerImages.length > 0) {
                    int prev = (vpSlider.getCurrentItem() - 1 + bannerImages.length) % bannerImages.length;
                    vpSlider.setCurrentItem(prev, true);
                    sliderHandler.removeCallbacks(sliderRunnable);
                    sliderHandler.postDelayed(sliderRunnable, SLIDE_INTERVAL_MS);
                }
            });
        }

        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                if (vpSlider != null && bannerImages.length > 0) {
                    int next = (vpSlider.getCurrentItem() + 1) % bannerImages.length;
                    vpSlider.setCurrentItem(next, true);
                    sliderHandler.removeCallbacks(sliderRunnable);
                    sliderHandler.postDelayed(sliderRunnable, SLIDE_INTERVAL_MS);
                }
            });
        }

        // Bottom-Right Floating Feedback Button -> Opens Writing Pad Popup
        if (btnFeedback != null) {
            btnFeedback.setOnClickListener(v ->
                    FeedbackDialogHelper.showWriteFeedbackPopup(getContext(), sessionManager.getClientId(), null)
            );
        }

        loadClientWelcomeName();

        return view;
    }

    private void setupDots(int activeIndex) {
        if (getContext() == null || layoutDots == null) return;
        layoutDots.removeAllViews();

        float density = getResources().getDisplayMetrics().density;
        int activeWidth = (int) (22 * density);
        int dotHeight = (int) (8 * density);
        int inactiveSize = (int) (8 * density);
        int margin = (int) (4 * density);

        for (int i = 0; i < bannerImages.length; i++) {
            View dot = new View(getContext());
            boolean isActive = (i == activeIndex);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    isActive ? activeWidth : inactiveSize,
                    dotHeight
            );
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dotHeight / 2f);
            bg.setColor(isActive ? Color.parseColor("#2563EB") : Color.parseColor("#CBD5E1"));
            dot.setBackground(bg);

            layoutDots.addView(dot);
        }
    }

    private void loadClientWelcomeName() {
        int clientId = sessionManager.getClientId();
        if (clientId <= 0) clientId = 1;

        ApiClient.getService().getClientData(clientId).enqueue(new Callback<ClientDocumentData>() {
            @Override
            public void onResponse(Call<ClientDocumentData> call, Response<ClientDocumentData> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    String company = response.body().getCompanyName();
                    if (company != null && !company.isEmpty()) {
                        tvCompanyName.setText(company);
                    }
                }
            }

            @Override
            public void onFailure(Call<ClientDocumentData> call, Throwable t) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        sliderHandler.removeCallbacks(sliderRunnable);
        sliderHandler.postDelayed(sliderRunnable, SLIDE_INTERVAL_MS);
    }

    @Override
    public void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        sliderHandler.removeCallbacks(sliderRunnable);
    }
}