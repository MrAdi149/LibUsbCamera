package org.easydarwin.sw;

public class JNIUtil {

    static {
        System.loadLibrary("Utils");
    }

    public static void yV12ToYUV420P(byte[ ] buffer, int width, int height){
        callMethod("YV12ToYUB420P",null, buffer, width, height);
    }

    public static void nV21To420SP(byte[ ] buffer, int width, int height){
        callMethod("NV21To420SP",null, buffer, width, height);
    }

    public static void rotateMatrix(byte[ ] data, int offset, int width, int height, int degree){
        callMethod("RotateByMatrix", null, data, offset, width, height, degree);
    }

    public static void rotateShortMatrix(byte[] data, int offset, int width, int height, int degree) {
        callMethod("RotateShortMatrix", null, data, offset, width, height, degree);
    }

    private static native void callMethod(String methodName, Object[] returnValue, Object... params);
}
