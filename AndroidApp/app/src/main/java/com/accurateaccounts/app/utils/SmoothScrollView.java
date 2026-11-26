package com.accurateaccounts.app.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class SmoothScrollView extends ScrollView {

    public SmoothScrollView(Context context) {
        super(context);
        init();
    }

    public SmoothScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SmoothScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setVerticalScrollBarEnabled(true);
        setScrollbarFadingEnabled(false);
        setOverScrollMode(OVER_SCROLL_ALWAYS);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_SCROLL) {
            float vScroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            if (vScroll != 0) {
                // Smooth wheel scrolling: move by delta
                smoothScrollBy(0, (int) (-vScroll * 220));
                return true;
            }
        }
        return super.dispatchGenericMotionEvent(event);
    }
}

