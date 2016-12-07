package com.aspsine.swipetoloadlayout;

import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ScrollerCompat;
import android.util.Log;
import android.view.View;

/**
 * DdToLoadLayout2 scroll helper
 * dy > 0 means scroll up (content down)
 * dy < 0 means scroll down (content up)
 * Created by wsl on 16-11-30.
 */

final class DdScrollHelper {

    private static final int AUTO_SCROLL_DURATION = 500;

    interface Listener {
        void onOffsetUpdate(int offset);
    }

    private View mLayout;

    private Listener mListener;

    private ScrollerCompat mAutoScroller;
    private AutoScrollRunnable mAutoScrollRunnable;

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    /**
     * View must be DdToLoadLayout
     * @param view
     */
    public DdScrollHelper(View view) {
        mLayout = view;
    }

    /**
     *
     * @param dy
     * @param minOffset
     * @param maxOffset
     * @return
     */
    final int scroll(int dy, int minOffset, int maxOffset) {
        Log.d("debug0", "scroll currOffset : " + getCurrentScrollOffset() + "---dy: " + dy);
        return scrollInternal(getCurrentScrollOffset() - dy, minOffset, maxOffset);
    }

    private int scrollInternal(int newOffset) {
        return scrollInternal(newOffset,
                Integer.MIN_VALUE, Integer.MAX_VALUE);
    }


    private int scrollInternal(int newOffset, int minOffset, int maxOffset) {
        final int curOffset = getCurrentScrollOffset();
        int consumed = 0;

        Log.d("debug1", "scrollInternal curOffset : " + curOffset);

        if (curOffset >= minOffset && curOffset <= maxOffset) {
            // If we have some scrolling range, and we're currently within the min and max
            // offsets, calculate a new offset
            newOffset = DdMathUtils.constrain(newOffset, minOffset, maxOffset);
            Log.d("debug1", "scrollInternal newOffset : " + newOffset);

            if (curOffset != newOffset) {
                mLayout.scrollTo(0, -newOffset);
                // Update how much dy we have consumed
                consumed = curOffset - newOffset;
                Log.d("debug1", "scrollInternal consumed : " + consumed);
            }

            dispatchOffsetUpdates();
        }

        return consumed;
    }

    final int getCurrentScrollOffset() {
        return -mLayout.getScrollY();
    }

    private void dispatchOffsetUpdates() {
        if(mListener != null) {
            mListener.onOffsetUpdate(getCurrentScrollOffset());
        }
    }

    final void abortAutoScroll(View layout) {
        if (mAutoScrollRunnable != null) {
            layout.removeCallbacks(mAutoScrollRunnable);
            mAutoScrollRunnable = null;
        }
        if(mAutoScroller != null) {
            if(!mAutoScroller.isFinished()) {
                mAutoScroller.abortAnimation();
            }
        }
    }

    /**
     *
     * return to origin or REFRESHING or LOAD_MORE state
     * @param layout DdToLoadLayout
     * @param dy offset
     * @return true means can auto scroll
     */
    final boolean autoScroll(View layout, int dy) {
        return autoScroll(layout, dy, AUTO_SCROLL_DURATION);
    }
    final boolean autoScroll(View layout, int dy, int duration) {
        if (mAutoScrollRunnable != null) {
            layout.removeCallbacks(mAutoScrollRunnable);
            mAutoScrollRunnable = null;
        }

        if (mAutoScroller == null) {
            mAutoScroller = ScrollerCompat.create(layout.getContext());
        }
        mAutoScroller.startScroll(0, getCurrentScrollOffset(), 0, dy, duration);
        if (mAutoScroller.computeScrollOffset()) {
            mAutoScrollRunnable = new AutoScrollRunnable(layout);
            ViewCompat.postOnAnimation(layout, mAutoScrollRunnable);
            return true;
        }
        return false;
    }

    private class AutoScrollRunnable implements Runnable {
        private final View mLayout;

        AutoScrollRunnable(View layout) {
            mLayout = layout;
        }

        @Override
        public void run() {
            if (mLayout != null && mAutoScroller != null && mAutoScroller.computeScrollOffset()) {
                scrollInternal(mAutoScroller.getCurrY());

                // Post ourselves so that we run on the next animation
                ViewCompat.postOnAnimation(mLayout, this);
            }
        }
    }
}