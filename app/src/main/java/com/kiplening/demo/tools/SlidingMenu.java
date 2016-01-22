package com.kiplening.demo.tools;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

/**
 * Created by MOON on 1/22/2016.
 */
public class SlidingMenu extends ViewGroup {
    // ===========================================================
    // Constants
    // ===========================================================

    private View mRightView;
    private View mLeftView;
    private ScrollRunnable mScrollRunnable;
    private int mTouchSlop;

    // 记录按下位置，用于判断当前滚动时向左还是向右
    private int mInterceptMotionX = 0;

    // 记录一次移动位置，用于计算移动偏移量
    private int mLastX;

    // ===========================================================
    // Fields
    // ===========================================================

    // ===========================================================
    // Constructors
    // ===========================================================

    public SlidingMenu(Context context) {
        super(context);
        initSlidingMenu(context);
    }

    public SlidingMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        initSlidingMenu(context);
    }

    public SlidingMenu(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initSlidingMenu(context);
    }

    // ===========================================================
    // Public Methods
    // ===========================================================

    public void addLeftView(View leftView) {
        mLeftView = leftView;
        // 由于Touch分发机制，即使右侧视图盖住当前视图
        // 只要VISIBLE状态，都会先接收到Touch Event
        mLeftView.setVisibility(View.INVISIBLE);
        addView(leftView);
    }

    /**
     * 提供右侧显示视图
     *
     * @param rightView
     */
    public void addRightView(View rightView) {
        mRightView = rightView;

        addView(rightView);
    }

    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // ===========================================================

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        if (!changed) {
            return;
        }

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);

            int measuredWidth = childView.getMeasuredWidth();
            int measuredHeight = childView.getMeasuredHeight();

            childView.layout(l, 0, l + measuredWidth, measuredHeight);
        }
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        final int actioin = ev.getAction();
        final int x = (int) ev.getX();

        switch (actioin) {
            case MotionEvent.ACTION_DOWN:
                mInterceptMotionX = x;
                break;

            case MotionEvent.ACTION_MOVE:
                final int deltaX = x - mInterceptMotionX;
                final int distance = Math.abs(deltaX);
                // 点击区域必须在右侧视图，因为仅右侧视图可移动
                // 横向移动超过一定距离，可以自己根据需求改动
                if ( canSliding(ev) && distance > mTouchSlop * 2) {

                    // 置为初始值
                    mLastX = x;
                    if (mScrollRunnable != null) {
                        mScrollRunnable.endScroll();
                        mScrollRunnable = null;
                    }

                    // 拦截Touch Event 交由当前ViewGruop onTouchEvent处理
                    return true;
                }

                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mInterceptMotionX = 0;
                break;
        }

        return false;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        final int x = (int) event.getX();

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:

                scrollIfNeed(x);
                return true;

            case MotionEvent.ACTION_UP:

                autoScrollIfNeed(x);
                break;
        }

        return false;
    }




    // ===========================================================
    // Private Methods
    // ===========================================================

    private void initSlidingMenu(Context context) {
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
    }

    /**
     * 当前手指点击位置是否在右侧视图区域内
     *
     * @param event
     * @return true 可以滚动
     */
    private boolean canSliding(MotionEvent event) {

        final int scrolledXInt = (int) (event.getX() + getScrollX());
        final int scrolledYInt = (int) (event.getY() + getScrollY());

        Rect frame = new Rect();
        mRightView.getHitRect(frame);
        if (frame.contains(scrolledXInt, scrolledYInt)) {
            return true;
        }

        return false;
    }


    private void scrollIfNeed(final int x) {
        // 计算与上次的偏移量
        int deltaX = x - mLastX;

        // 减少移动次数
        if (x != mLastX) {
            // 显示
            if (mLeftView.getVisibility() != View.VISIBLE) {
                mLeftView.setVisibility(View.VISIBLE);
            }

            int l = mRightView.getLeft();
            int t = mRightView.getTop();
            int b = mRightView.getBottom();

            // 右侧视图的滑动区域，只能在左侧视图范围内滑动
            int rightViewLeft = Math.max(mLeftView.getLeft(), l + deltaX);
            rightViewLeft = Math.min(mLeftView.getRight(), rightViewLeft);

            // 控制随手指滑动
            mRightView.layout(rightViewLeft, t, rightViewLeft + mRightView.getWidth(), b);
        }

        // 滑动到最左侧
        if (mRightView.getLeft() == mLeftView.getLeft()) {
            mLeftView.setVisibility(View.INVISIBLE);
        }

        // 记录当前值供下次计算
        mLastX = x;
    }


    private void autoScrollIfNeed(final int x) {
        mScrollRunnable = new ScrollRunnable();

        // 用于判断滑动方向
        final int deltaX = x - mInterceptMotionX;
        // x轴向右是依次递增与手指落下点差值，小于0说明是手指向左滑动
        boolean moveLeft = deltaX <= 0;

        // 滑动距离超过左侧视图一半，才会沿着手指方向滚动
        final int distance = Math.abs(deltaX);
        if (distance < mLeftView.getWidth() / 2) {
            // 从哪来回哪去
            moveLeft = !moveLeft;
        }

        // 启动自动滚动
        mScrollRunnable.startScroll(moveLeft);
    }


    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    private class ScrollRunnable implements Runnable {
        // 滚动辅助类，提供起始位置，移动偏移，移动总时间，可以获取每次滚动距离
        private Scroller mScroller = new Scroller(getContext());

        @Override
        public void run() {
            final Scroller scroller = mScroller;
            // 计算滚动偏移，返回是否可以接着滚动
            boolean more = scroller.computeScrollOffset();
            // 计算后获取需要滚动到的位置
            final int x = scroller.getCurrX();

            if (more) {
                // 与手动滚动调用的方法相同
                scrollIfNeed(x);
                // 当前子线程已经执行完，但是需要接着滚动
                // 所以把当前Runnable再次添加到消息队列中
                post(this);
            } else {
                // 不需要滚动
                endScroll();
            }

        }


        private void startScroll(boolean moveLeft) {
            // 滚动前设置初始值
            mLastX = mRightView.getLeft();

            int dx = 0;

            // 计算移动总距离
            if (moveLeft) {
                // 当前到左视图左侧边界距离
                dx = mLeftView.getLeft() - mRightView.getLeft();
            } else {
                // 到右侧边界
                dx = mLeftView.getRight() - mRightView.getLeft();
            }

            // 开始滚动
            mScroller.startScroll(mRightView.getLeft(), 0, dx, 0, 300);
            // 把当前Runnable添加到消息队列中
            post(this);
        }

        private void endScroll() {
            // 从消息队列中把当前Runnable删除，即停止滚动
            removeCallbacks(this);
        }

    }


}
