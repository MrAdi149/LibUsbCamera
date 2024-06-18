package com.example1.usb.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.example1.usb.encoder.IVideoEncoder;
import com.example1.usb.encoder.MediaVideoEncoder;
import com.example1.usb.encoder.MediaEncoder;

public class UVCCameraTextureView extends AspectRatioTextureView // API >= 14
        implements TextureView.SurfaceTextureListener, CameraViewInterface {

    private static final boolean DEBUG = true; // TODO set false on release
    private static final String TAG = "UVCCameraTextureView";

    private boolean mHasSurface;
    private RenderHandler mRenderHandler;
    private final Object mCaptureSync = new Object();
    private Bitmap mTempBitmap;
    private boolean mReqesutCaptureStillImage;
    private Callback mCallback;

    private final FpsCounter mFpsCounter = new FpsCounter();

    private float requestedAspectRatio;

    public UVCCameraTextureView(final Context context) {
        this(context, null, 0);
    }

    public UVCCameraTextureView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UVCCameraTextureView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        setSurfaceTextureListener(this);
    }

    @Override
    public void onResume() {
        if (DEBUG) Log.v(TAG, "onResume:");
        if (mHasSurface) {
            mRenderHandler = RenderHandler.createHandler(mFpsCounter, super.getSurfaceTexture(), getWidth(), getHeight());
        }
    }

    @Override
    public void setAspectRatio(float aspectRatio) {
        if(aspectRatio <= 0) return;
        requestedAspectRatio = aspectRatio;
        requestLayout();
    }

    @Override
    public void onPause() {
        if (DEBUG) Log.v(TAG, "onPause:");
        if (mRenderHandler != null) {
            mRenderHandler.release();
            mRenderHandler = null;
        }
        if (mTempBitmap != null) {
            mTempBitmap.recycle();
            mTempBitmap = null;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(final SurfaceTexture surface, final int width, final int height) {
        if (DEBUG) Log.i(TAG, "onSurfaceTextureAvailable:" + surface);
        if (mRenderHandler == null) {
            mRenderHandler = RenderHandler.createHandler(mFpsCounter, surface, width, height);
        } else {
            mRenderHandler.resize(width, height);
        }
        mHasSurface = true;
        if (mCallback != null) {
            mCallback.onSurfaceCreated(this, getSurface());
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(final SurfaceTexture surface, final int width, final int height) {
        if (DEBUG) Log.i(TAG, "onSurfaceTextureSizeChanged:" + surface);
        if (mRenderHandler != null) {
            mRenderHandler.resize(width, height);
        }
        if (mCallback != null) {
            mCallback.onSurfaceChanged(this, getSurface(), width, height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(final SurfaceTexture surface) {
        if (DEBUG) Log.i(TAG, "onSurfaceTextureDestroyed:" + surface);
        if (mRenderHandler != null) {
            mRenderHandler.release();
            mRenderHandler = null;
        }
        mHasSurface = false;
        if (mCallback != null) {
            mCallback.onSurfaceDestroy(this, getSurface());
        }
        if (mPreviewSurface != null) {
            mPreviewSurface.release();
            mPreviewSurface = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(final SurfaceTexture surface) {
        synchronized (mCaptureSync) {
            if (mReqesutCaptureStillImage) {
                mReqesutCaptureStillImage = false;
                if (mTempBitmap == null)
                    mTempBitmap = getBitmap();
                else
                    getBitmap(mTempBitmap);
                mCaptureSync.notifyAll();
            }
        }
    }

    @Override
    public boolean hasSurface() {
        return mHasSurface;
    }

    @Override
    public Bitmap captureStillImage(int width, int height) {
        synchronized (mCaptureSync) {
            mReqesutCaptureStillImage = true;
            try {
                mCaptureSync.wait();
            } catch (final InterruptedException e) {
                // Handle the exception
            }
            return mTempBitmap;
        }
    }

    @Override
    public SurfaceTexture getSurfaceTexture() {
        return mRenderHandler != null ? mRenderHandler.getPreviewTexture() : null;
    }

    private Surface mPreviewSurface;

    @Override
    public Surface getSurface() {
        if (DEBUG) Log.v(TAG, "getSurface:hasSurface=" + mHasSurface);
        if (mPreviewSurface == null) {
            final SurfaceTexture st = getSurfaceTexture();
            if (st != null) {
                mPreviewSurface = new Surface(st);
            }
        }
        return mPreviewSurface;
    }

    @Override
    public void setVideoEncoder(final IVideoEncoder encoder) {
        if (mRenderHandler != null)
            mRenderHandler.setVideoEncoder(encoder);
    }

    @Override
    public void setCallback(final Callback callback) {
        mCallback = callback;
    }

    public void resetFps() {
        mFpsCounter.reset();
    }

    public void updateFps() {
        mFpsCounter.update();
    }

    public float getFps() {
        return mFpsCounter.getFps();
    }

    public float getTotalFps() {
        return mFpsCounter.getTotalFps();
    }

    private static final class RenderHandler extends Handler implements SurfaceTexture.OnFrameAvailableListener {

        private static final int MSG_REQUEST_RENDER = 1;
        private static final int MSG_SET_ENCODER = 2;
        private static final int MSG_CREATE_SURFACE = 3;
        private static final int MSG_RESIZE = 4;
        private static final int MSG_TERMINATE = 9;

        private RenderThread mThread;
        private boolean mIsActive = true;
        private final FpsCounter mFpsCounter;

        public static final RenderHandler createHandler(final FpsCounter counter,
                                                        final SurfaceTexture surface, final int width, final int height) {

            final RenderThread thread = new RenderThread(counter, surface, width, height);
            thread.start();
            return thread.getHandler();
        }

        private RenderHandler(final FpsCounter counter, final RenderThread thread) {
            mThread = thread;
            mFpsCounter = counter;
        }

        public final void setVideoEncoder(final IVideoEncoder encoder) {
            if (DEBUG) Log.v(TAG, "setVideoEncoder:");
            if (mIsActive)
                sendMessage(obtainMessage(MSG_SET_ENCODER, encoder));
        }

        public final SurfaceTexture getPreviewTexture() {
            if (DEBUG) Log.v(TAG, "getPreviewTexture:");
            if (mIsActive) {
                synchronized (mThread.mSync) {
                    sendEmptyMessage(MSG_CREATE_SURFACE);
                    try {
                        mThread.mSync.wait();
                    } catch (final InterruptedException e) {
                        // Handle the exception
                    }
                    return mThread.mPreviewSurface;
                }
            } else {
                return null;
            }
        }

        public void resize(final int width, final int height) {
            if (DEBUG) Log.v(TAG, "resize:");
            if (mIsActive) {
                synchronized (mThread.mSync) {
                    sendMessage(obtainMessage(MSG_RESIZE, width, height));
                    try {
                        mThread.mSync.wait();
                    } catch (final InterruptedException e) {
                        // Handle the exception
                    }
                }
            }
        }

        public final void release() {
            if (DEBUG) Log.v(TAG, "release:");
            if (mIsActive) {
                mIsActive = false;
                removeMessages(MSG_REQUEST_RENDER);
                removeMessages(MSG_SET_ENCODER);
                sendEmptyMessage(MSG_TERMINATE);
            }
        }

        @Override
        public final void onFrameAvailable(final SurfaceTexture surfaceTexture) {
            if (mIsActive) {
                mFpsCounter.count();
                sendEmptyMessage(MSG_REQUEST_RENDER);
            }
        }

        @Override
        public final void handleMessage(final Message msg) {
            if (mThread == null) return;
            switch (msg.what) {
                case MSG_REQUEST_RENDER:
                    mThread.onDrawFrame();
                    break;
                case MSG_SET_ENCODER:
                    mThread.setEncoder((MediaEncoder) msg.obj);
                    break;
                case MSG_CREATE_SURFACE:
                    mThread.updatePreviewSurface();
                    break;
                case MSG_RESIZE:
                    mThread.resize(msg.arg1, msg.arg2);
                    break;
                case MSG_TERMINATE:
                    Looper.myLooper().quit();
                    mThread = null;
                    break;
                default:
                    super.handleMessage(msg);
            }
        }

        private static final class RenderThread extends Thread {
            private final Object mSync = new Object();
            private final SurfaceTexture mSurface;
            private RenderHandler mHandler;
            private EGLBase mEgl;
            private EGLBase.IEglSurface mEglSurface;
            private GLDrawer2D mDrawer;
            private int mTexId = -1;
            private SurfaceTexture mPreviewSurface;
            private final float[] mStMatrix = new float[16];
            private MediaEncoder mEncoder;
            private int mViewWidth, mViewHeight;
            private final FpsCounter mFpsCounter;

            public RenderThread(final FpsCounter fpsCounter, final SurfaceTexture surface, final int width, final int height) {
                mFpsCounter = fpsCounter;
                mSurface = surface;
                mViewWidth = width;
                mViewHeight = height;
                setName("RenderThread");
            }

            public RenderHandler getHandler() {
                if (DEBUG) Log.v(TAG, "RenderThread#getHandler:");
                synchronized (mSync) {
                    // create rendering thread
                    if (mHandler == null)
                        try {
                            mSync.wait();
                        } catch (final InterruptedException e) {
                            // Handle the exception
                        }
                }
                return mHandler;
            }

            public void resize(final int width, final int height) {
                if (((width > 0) && (width != mViewWidth)) || ((height > 0) && (height != mViewHeight))) {
                    mViewWidth = width;
                    mViewHeight = height;
                    updatePreviewSurface();
                } else {
                    synchronized (mSync) {
                        mSync.notifyAll();
                    }
                }
            }

            public final void updatePreviewSurface() {
                if (DEBUG) Log.i(TAG, "RenderThread#updatePreviewSurface:");
                synchronized (mSync) {
                    if (mPreviewSurface != null) {
                        if (DEBUG) Log.d(TAG, "updatePreviewSurface:release mPreviewSurface");
                        mPreviewSurface.setOnFrameAvailableListener(null);
                        mPreviewSurface.release();
                        mPreviewSurface = null;
                    }
                    mEglSurface.makeCurrent();
                    if (mTexId >= 0) {
                        mDrawer.deleteTex(mTexId);
                    }
                    // create texture and SurfaceTexture for input from camera
                    mTexId = mDrawer.initTex();
                    if (DEBUG) Log.v(TAG, "updatePreviewSurface:tex_id=" + mTexId);
                    mPreviewSurface = new SurfaceTexture(mTexId);
                    mPreviewSurface.setDefaultBufferSize(mViewWidth, mViewHeight);
                    mPreviewSurface.setOnFrameAvailableListener(mHandler);
                    // notify to caller thread that previewSurface is ready
                    mSync.notifyAll();
                }
            }

            public final void setEncoder(final MediaEncoder encoder) {
                if (DEBUG) Log.v(TAG, "RenderThread#setEncoder:encoder=" + encoder);
                if (encoder != null && (encoder instanceof MediaVideoEncoder)) {
                    ((MediaVideoEncoder) encoder).setEglContext(mEglSurface.getContext(), mTexId);
                }
                mEncoder = encoder;
            }

            public final void onDrawFrame() {
                mEglSurface.makeCurrent();
                // update texture(came from camera)
                mPreviewSurface.updateTexImage();
                // get texture matrix
                mPreviewSurface.getTransformMatrix(mStMatrix);
                // notify video encoder if it exist
                if (mEncoder != null) {
                    // notify to capturing thread that the camera frame is available.
                    if (mEncoder instanceof MediaVideoEncoder)
                        ((MediaVideoEncoder) mEncoder).frameAvailableSoon(mStMatrix);
                    else
                        mEncoder.frameAvailableSoon();
                }
                // draw to preview screen
                mDrawer.draw(mTexId, mStMatrix, 0);
                mEglSurface.swap();
            }

            @Override
            public final void run() {
                Log.d(TAG, getName() + " started");
                init();
                Looper.prepare();
                synchronized (mSync) {
                    mHandler = new RenderHandler(mFpsCounter, this);
                    mSync.notify();
                }

                Looper.loop();

                Log.d(TAG, getName() + " finishing");
                release();
                synchronized (mSync) {
                    mHandler = null;
                    mSync.notify();
                }
            }

            private final void init() {
                if (DEBUG) Log.v(TAG, "RenderThread#init:");
                // create EGLContext for this thread
                mEgl = EGLBase.createFrom(null, false, false);
                mEglSurface = mEgl.createFromSurface(mSurface);
                mEglSurface.makeCurrent();
                // create drawing object
                mDrawer = new GLDrawer2D(true);
            }

            private final void release() {
                if (DEBUG) Log.v(TAG, "RenderThread#release:");
                if (mDrawer != null) {
                    mDrawer.release();
                    mDrawer = null;
                }
                if (mPreviewSurface != null) {
                    mPreviewSurface.release();
                    mPreviewSurface = null;
                }
                if (mTexId >= 0) {
                    GLHelper.deleteTex(mTexId);
                    mTexId = -1;
                }
                if (mEglSurface != null) {
                    mEglSurface.release();
                    mEglSurface = null;
                }
                if (mEgl != null) {
                    mEgl.release();
                    mEgl = null;
                }
            }
        }
    }
}
