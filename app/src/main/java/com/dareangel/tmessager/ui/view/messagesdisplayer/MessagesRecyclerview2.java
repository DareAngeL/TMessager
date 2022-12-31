package com.dareangel.tmessager.ui.view.messagesdisplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.dareangel.tmessager.data.model.interfaces.IPullToLoadMoreListener;

public class MessagesRecyclerview2 extends RecyclerView {

    private View mPullToLoadMoreView;
    private IPullToLoadMoreListener mPullToLoadMoreListener;

    private boolean mIsOverScrolling = false;
    private float pointY = -1f;

    public MessagesRecyclerview2(@NonNull Context context) {
        super(context);
    }

    public MessagesRecyclerview2(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MessagesRecyclerview2(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            pointY = e.getY();
        }

        if (e.getAction() == MotionEvent.ACTION_MOVE) {
            // *** THIS IS TO FULFILL THE OVER SCROLLING EFFECT OF EDGE EFFECT FACTORY ***
            // If the recyclerview is over scrolling no matter which part it over scrolls, either at
            // the top or at the bottom, and if the user decided to scroll to the opposite of over scrolling
            // value, this code block will slowly return the recyclerview's original Y translation
            // which is 0 translation depending on the scroll input of the user.
            if (mIsOverScrolling)
            {
                if (TOP_OVERSCROLLING(e.getY())) {
                    getMessagesLayoutManager().canScroll(false);
                    final float movedDistance = e.getY() - pointY;

                    if (getTranslationY() > 0) {
                        final float translation = getTranslationY() - Math.abs(movedDistance) * 0.8f;
                        setTranslationY(translation);
                        mPullToLoadMoreView.setTranslationY(translation);
                        mPullToLoadMoreListener.onPulling(
                                getTranslationY() / MessagesRecyclerViewOverScrollEffect.MAX_PULL_VALUE
                        );
                    } else {
                        getMessagesLayoutManager().canScroll(true);
                    }
                }

                if (BOTTOM_OVERSCROLLING(e.getY())) {
                    getMessagesLayoutManager().canScroll(false);
                    final float movedDistance = e.getY() - pointY;

                    if (getTranslationY() < 0) {
                        final float translation = getTranslationY() + Math.abs(movedDistance) * 0.8f;
                        setTranslationY(translation);
                    } else {
                        getMessagesLayoutManager().canScroll(true);
                    }
                }
            }

            pointY = e.getY();
        }

        return super.onTouchEvent(e);
    }

    /**
     * Determine if the over scrolling is happening at the bottom
     */
    private boolean BOTTOM_OVERSCROLLING(float y) { return firstItemPosition() != 0 && y >= pointY; }

    /**
     * Determine if the over scrolling is happening at the top
     */
    private boolean TOP_OVERSCROLLING(float y) { return firstItemPosition() == 1 && y <= pointY; }

    /**
     * See the first item visible in the recyclerview
     */
    private int firstItemPosition() { return getMessagesLayoutManager().findFirstCompletelyVisibleItemPosition(); }

    public void loadMoreData() {
        getMessageAdapter().lazyLoadCallback().onLoadMoreData();
    }

    public void setIsOverScrolling(boolean mIsOverScrolling) {
        this.mIsOverScrolling = mIsOverScrolling;
    }

    public View getPullToLoadMoreView() {
        return mPullToLoadMoreView;
    }

    public void setPullToLoadMoreView(View mPullToLoadMoreView) {
        this.mPullToLoadMoreView = mPullToLoadMoreView;
    }

    public MessagesRecyclerviewLayoutManager getMessagesLayoutManager() {
        return (MessagesRecyclerviewLayoutManager) getLayoutManager();
    }

    public MessagesAdapter getMessageAdapter() {
        return (MessagesAdapter) getAdapter();
    }

    public IPullToLoadMoreListener getPullToLoadMoreListener() {
        return mPullToLoadMoreListener;
    }

    public void setPullToLoadMoreListener(IPullToLoadMoreListener mPullToLoadMoreListener) {
        this.mPullToLoadMoreListener = mPullToLoadMoreListener;
    }
}
