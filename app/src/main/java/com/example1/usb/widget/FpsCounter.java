package com.example1.usb.widget;

public class FpsCounter {
    private long mStartTime = 0L;
    private int mFramesCount = 0;
    private float mTotalFps = 0.0f;
    private int mUpdateCount = 0;

    public void count() {
        mFramesCount++;
    }

    public void reset() {
        mStartTime = System.currentTimeMillis();
        mFramesCount = 0;
    }

    public float getFps() {
        if(mStartTime == 0L) {
            reset();
            return 0.0f;
        }
        long elapsed = System.currentTimeMillis() - mStartTime;
        if (elapsed == 0) {
            return 0.0f;
        }
        return (float) mFramesCount / elapsed * 1000.0f;
    }

    public void update() {
        float currentFps = getFps();
        mTotalFps += currentFps;
        mUpdateCount++;
        reset(); // Reset for the next FPS calculation
    }

    public float getTotalFps() {
        if (mUpdateCount == 0) {
            return 0.0f;
        }
        return mTotalFps / mUpdateCount;
    }

}