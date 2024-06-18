package com.example1.usb.widget;
import android.opengl.GLES20;

public class GLDrawer2D {
    private int mProgram;
    private int mTexId;

    public GLDrawer2D(boolean isExternalOES) {
        init(isExternalOES);
    }

    private void init(boolean isExternalOES) {
        String vertexShaderSrc = "uniform mat4 uMVPMatrix;\n" +
                "attribute vec4 aPosition;\n" +
                "attribute vec2 aTexCoord;\n" +
                "varying vec2 vTexCoord;\n" +
                "void main() {\n" +
                "    gl_Position = uMVPMatrix * aPosition;\n" +
                "    vTexCoord = aTexCoord;\n" +
                "}";
        String fragmentShaderSrc = isExternalOES ?
                "#extension GL_OES_EGL_image_external : require\n" +
                        "precision mediump float;\n" +
                        "uniform samplerExternalOES sTexture;\n" +
                        "varying vec2 vTexCoord;\n" +
                        "void main() {\n" +
                        "    gl_FragColor = texture2D(sTexture, vTexCoord);\n" +
                        "}" :
                "precision mediump float;\n" +
                        "uniform sampler2D sTexture;\n" +
                        "varying vec2 vTexCoord;\n" +
                        "void main() {\n" +
                        "    gl_FragColor = texture2D(sTexture, vTexCoord);\n" +
                        "}";

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderSrc);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSrc);

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);
    }

    public int initTex() {
        int[] texIds = new int[1];
        GLES20.glGenTextures(1, texIds, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texIds[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        mTexId = texIds[0];
        return mTexId;
    }

    public void draw(int texId, float[] mvpMatrix, int offset) {
        GLES20.glUseProgram(mProgram);
        // Setup vertex data, texture data, etc.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
        // Draw the texture using the shader program
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    public void deleteTex(int texId) {
        GLES20.glDeleteTextures(1, new int[]{texId}, 0);
    }

    public void release() {
        if (mProgram != 0) {
            GLES20.glDeleteProgram(mProgram);
            mProgram = 0;
        }
    }

    private int loadShader(int type, String shaderSrc) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderSrc);
        GLES20.glCompileShader(shader);
        return shader;
    }
}

