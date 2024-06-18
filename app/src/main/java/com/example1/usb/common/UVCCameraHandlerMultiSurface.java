package com.example1.usb.common;

import android.app.Activity;

import com.example1.usb.UVCCamera;
import com.example1.usb.encoder.RenderHandler;
import com.example1.usb.widget.CameraViewInterface;

public class UVCCameraHandlerMultiSurface extends AbstractUVCCameraHandler {


    protected UVCCameraHandlerMultiSurface(CameraThread thread) {
        super(thread);
    }

    public static final UVCCameraHandlerMultiSurface createHandler(
           final Activity parent, final CameraViewInterface cameraView,
           final int width, final int height    ){
       return createHandler(parent, cameraView, 1, width, height, UVCCamera.FRAME_FORMAT_MJPEG, UVCCamera.DEFAULT_BANDWIDTH);
   }

   public static final UVCCameraHandlerMultiSurface createHandler(
           final Activity parent, final CameraViewInterface cameraView,
           final int width, final int height, final float bandWidhtFactor){

       return createHandler(parent, cameraView, 1,width, height, UVCCamera.FRAME_FORMAT_MJPEG, bandWidhtFactor);
   }

   public static final UVCCameraHandlerMultiSurface createHandler(
           final Activity parent, final CameraViewInterface cameraView,
           final int encoderType, final int width, final int height){

       return createHandler(parent, cameraView, encoderType, width, height, UVCCamera.FRAME_FORMAT_MJPEG, UVCCamera.DEFAULT_BANDWIDTH);
   }

   public static final UVCCameraHandlerMultiSurface createHandler(
           final Activity parent, final CameraViewInterface cameraView,
           final int encoderType, final int width, final int height, final int format, final float bandwidthFactor){


       final CameraThread thread = new CameraThread(UVCCameraHandlerMultiSurface.class, parent, cameraView, encoderType, width, height, format, bandwidthFactor);
       thread.start();
       return (UVCCameraHandlerMultiSurface)thread.getHandler();
   }


}
