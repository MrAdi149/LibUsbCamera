package com.example1.utils;


import android.os.Handler;
import android.os.HandlerThread;

public class HandlerThreadHandler {
    private HandlerThread handlerThread;
    private Handler handler;

    public static Handler createHandler(final String name){
        HandlerThread thread = new HandlerThread(name);
        thread.start();
        return new Handler(thread.getLooper());
    }

    public Handler getHandler() {
        return handler;
    }

    public void stopHandler() {
        if (handlerThread != null) {
            handlerThread.quitSafely();
            try {
                handlerThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            handlerThread = null;
            handler = null;
        }
    }
}
