package org.easydarwin.sw;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TxtOverlay {

    static {
        System.loadLibrary("TxtOverlay");
    }

    private static TxtOverlay instance;
    private final Context context;
    private long ctx;

    private TxtOverlay(Context context){
        this.context = context;
    }

    public static TxtOverlay getInstance(){
        if(instance == null){
            throw new IllegalArgumentException("please call install() before getInstance()");
        }
        return instance;
    }

    public static void install(Context context){
        if(instance == null){
            instance = new TxtOverlay(context.getApplicationContext());

            File youyuan = context.getFileStreamPath("SIMYOU.ttf");

            if(!youyuan.exists()){
                AssetManager am = context.getAssets();
                try{
                    InputStream is = am.open("zk/SIMYOU.ttf");
                    FileOutputStream os = context.openFileOutput("SIMYOU.ttf", Context.MODE_PRIVATE);

                    byte[] buffer = new byte[1024];
                    int len;
                    while((len = is.read(buffer)) != -1){
                        os.write(buffer, 0, len);
                    }
                    os.close();
                    is.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    public void init(int width, int height) {
        // For simplicity, let's assume initialization sets up a context or resources
        // This is a placeholder and depends on your actual overlay library implementation
        this.ctx = initializeOverlayLibrary(width, height);
    }

    public void release() {
        // For simplicity, let's assume releasing frees resources or closes handles
        // This is a placeholder and depends on your actual overlay library implementation
        releaseOverlayLibrary(this.ctx);
        this.ctx = 0; // Reset context
    }

    public void overlay(byte[] yuvData, String text) {
        // For simplicity, let's print the overlay text to console
        // Replace with your actual overlay logic (e.g., using JNI or library API)
        System.out.println("Overlaying text '" + text + "' on YUV data");

        // Example: NativeLibrary.overlayText(this.ctx, yuvData, text);
    }

    // Placeholder method to initialize your overlay library
    private native long initializeOverlayLibrary(int width, int height);

    // Placeholder method to release resources held by your overlay library
    private native void releaseOverlayLibrary(long ctx);
}
