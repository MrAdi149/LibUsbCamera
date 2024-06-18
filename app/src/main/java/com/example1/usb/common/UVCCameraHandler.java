package com.example1.usb.common;

import android.app.Activity;

import com.example1.usb.UVCCamera;
import com.example1.usb.widget.CameraViewInterface;

public class UVCCameraHandler extends AbstractUVCCameraHandler {

    public static final UVCCameraHandler createHandler(
            final Activity parent, final CameraViewInterface cameraView,
            final int width, final int height) {

        return createHandler(parent, cameraView, 1, width, height, UVCCamera.FRAME_FORMAT_MJPEG, UVCCamera.DEFAULT_BANDWIDTH);
    }

    public static final UVCCameraHandler createHandler(
            final Activity parent, final CameraViewInterface cameraView,
            final int width, final int height, final float bandwidthFactor) {

        return createHandler(parent, cameraView, 1, width, height, UVCCamera.FRAME_FORMAT_MJPEG, bandwidthFactor);
    }

    public static final UVCCameraHandler createHandler(
            final Activity parent, final CameraViewInterface cameraView,
            final int encoderType, final int width, final int height) {

        return createHandler(parent, cameraView, encoderType, width, height, UVCCamera.FRAME_FORMAT_MJPEG, UVCCamera.DEFAULT_BANDWIDTH);
    }

    public static final UVCCameraHandler createHandler(
            final Activity parent, final CameraViewInterface cameraView,
            final int encoderType, final int width, final int height, final int format) {

        return createHandler(parent, cameraView, encoderType, width, height, format, UVCCamera.DEFAULT_BANDWIDTH);
    }

    public static final UVCCameraHandler createHandler(
            final Activity parent, final CameraViewInterface cameraView,
            final int encoderType, final int width, final int height, final int format, final float bandwidthFactor) {

        final CameraThread thread = new CameraThread(UVCCameraHandler.class, parent, cameraView, encoderType, width, height, format, bandwidthFactor);
        thread.start();
        return (UVCCameraHandler)thread.getHandler();
    }

    protected UVCCameraHandler(final CameraThread thread) {
        super(thread);
    }

    @Override
    public void startPreview(final Object surface) {
        super.startPreview(surface);
    }


    @Override
    public void captureStill(final String path,OnCaptureListener listener) {
        super.captureStill(path,listener);
    }

    @Override
    public void startCameraFoucs() {
        super.startCameraFoucs();
    }
}
