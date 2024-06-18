package com.example1.usb.widget;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.view.Surface;

import com.example1.usb.encoder.IVideoEncoder;

public interface CameraViewInterface {

    void setAspectRatio(float aspectRatio);
    public interface Callback {
        public void onSurfaceCreated(CameraViewInterface view, Surface surface);
        public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height);
        public void onSurfaceDestroy(CameraViewInterface view, Surface surface);
    }
    public void onPause();
    public void onResume();
    public void setCallback(Callback callback);
    public SurfaceTexture getSurfaceTexture();
    public Surface getSurface();
    public boolean hasSurface();
    public void setVideoEncoder(final IVideoEncoder encoder);
    public Bitmap captureStillImage(int width,int height);
}
