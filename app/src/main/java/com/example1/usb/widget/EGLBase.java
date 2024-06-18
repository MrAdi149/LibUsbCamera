package com.example1.usb.widget;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;

public class EGLBase {
    private EGLDisplay mEGLDisplay;
    private EGLContext mEGLContext;
    private EGLConfig mEGLConfig;

    public static EGLBase createFrom(EGLContext sharedContext, boolean withDepthBuffer, boolean isRecordable) {
        return new EGLBase(sharedContext, withDepthBuffer, isRecordable);
    }

    private EGLBase(EGLContext sharedContext, boolean withDepthBuffer, boolean isRecordable) {
        init(sharedContext, withDepthBuffer, isRecordable);
    }

    private void init(EGLContext sharedContext, boolean withDepthBuffer, boolean isRecordable) {
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("Unable to get EGL14 display");
        }

        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            throw new RuntimeException("Unable to initialize EGL14");
        }

        int[] attribList = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_NONE
        };

        int[] numConfigs = new int[1];
        EGLConfig[] configs = new EGLConfig[1];
        if (!EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length, numConfigs, 0)) {
            throw new IllegalArgumentException("Unable to find RGB8888 / EGLConfig");
        }
        mEGLConfig = configs[0];

        int[] contextAttribs = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };

        mEGLContext = EGL14.eglCreateContext(mEGLDisplay, mEGLConfig, sharedContext, contextAttribs, 0);
        if (mEGLContext == null) {
            throw new RuntimeException("Failed to create EGL context");
        }
    }

    public IEglSurface createFromSurface(SurfaceTexture surface) {
        return new EglSurface(mEGLDisplay, mEGLConfig, surface, mEGLContext);
    }

    public void release() {
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
            EGL14.eglTerminate(mEGLDisplay);
        }
        mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        mEGLContext = EGL14.EGL_NO_CONTEXT;
    }

    public static class EglSurface implements IEglSurface {
        private final EGLDisplay mEGLDisplay;
        private final EGLSurface mEGLSurface;
        private final EGLContext mEGLContext;

        public EglSurface(EGLDisplay eglDisplay, EGLConfig eglConfig, SurfaceTexture surface, EGLContext eglContext) {
            mEGLDisplay = eglDisplay;
            mEGLContext = eglContext;

            int[] surfaceAttribs = {EGL14.EGL_NONE};
            mEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, eglConfig, surface, surfaceAttribs, 0);
            if (mEGLSurface == null) {
                throw new RuntimeException("Failed to create EGL surface");
            }
        }

        @Override
        public void makeCurrent() {
            if (!EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
                throw new RuntimeException("Failed to make EGL context current");
            }
        }

        @Override
        public void swap() {
            EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurface);
        }

        @Override
        public void release() {
            EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface);
        }

        @Override
        public IContext getContext() {
            return null;
        }
    }

    public interface IEglSurface {
        void makeCurrent();
        void swap();
        void release();

        IContext getContext();
    }

    public interface IContext {
        // Define methods relevant to EGL context setup
        void createContext();
        void destroyContext();
        // Other relevant methods
    }
}
