package com.example1.usb.encoder;

import android.graphics.SurfaceTexture;
import android.opengl.EGLContext;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.Surface;

import com.example1.usb.widget.EGLBase;

public class RenderHandler extends Handler {

    private static final String TAG = "RenderHandler";
    private static final int MSG_EGL_READY = 1;
    private static final int MSG_EGL_DRAW = 2;
    private static final int MSG_EGL_RELEASE = 3;

    private final RenderThread mRenderThread;

    private RenderHandler(RenderThread renderThread) {
        mRenderThread = renderThread;
    }

    public static RenderHandler createHandler(String name) {
        RenderThread renderThread = new RenderThread(name);
        renderThread.start();
        return new RenderHandler(renderThread);
    }

    public void setEglContext(EGLBase.IContext sharedContext, int tex_id, Surface surface, boolean isRecordable) {
        sendMessage(obtainMessage(MSG_EGL_READY, new EglReadyParams(sharedContext, tex_id, surface, isRecordable)));
    }

    public void draw(float[] texMatrix) {
        sendMessage(obtainMessage(MSG_EGL_DRAW, texMatrix));
    }

    public void release() {
        sendEmptyMessage(MSG_EGL_RELEASE);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_EGL_READY:
                EglReadyParams params = (EglReadyParams) msg.obj;
                mRenderThread.initEGL(params.sharedContext, params.texId, params.surface, params.isRecordable);
                break;
            case MSG_EGL_DRAW:
                mRenderThread.drawFrame((float[]) msg.obj);
                break;
            case MSG_EGL_RELEASE:
                mRenderThread.releaseEGL();
                mRenderThread.quitSafely();
                break;
        }
    }

    private static class EglReadyParams {
        EGLBase.IContext sharedContext;
        int texId;
        Surface surface;
        boolean isRecordable;

        EglReadyParams(EGLBase.IContext sharedContext, int texId, Surface surface, boolean isRecordable) {
            this.sharedContext = sharedContext;
            this.texId = texId;
            this.surface = surface;
            this.isRecordable = isRecordable;
        }
    }

    private static class RenderThread extends HandlerThread {
        private EGLBase mEglBase;
        // Define other necessary variables for rendering

        RenderThread(String name) {
            super(name);
        }

        @Override
        protected void onLooperPrepared() {
            // Perform any initialization here
        }

        void initEGL(EGLBase.IContext sharedContext, int texId, Surface surface, boolean isRecordable) {
            mEglBase = EGLBase.createFrom((EGLContext) sharedContext, true, isRecordable);
            mEglBase.createFromSurface(new SurfaceTexture(texId));
            // Initialize EGL and other rendering components
        }

        void drawFrame(float[] texMatrix) {
            // Draw frame using OpenGL ES
        }

        void releaseEGL() {
            if (mEglBase != null) {
                mEglBase.release();
                mEglBase = null;
            }
            // Release other resources
        }
    }
}
