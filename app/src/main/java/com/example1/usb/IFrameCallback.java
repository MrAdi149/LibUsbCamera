package com.example1.usb;

import java.nio.ByteBuffer;

public interface IFrameCallback {

    public void onFrame(ByteBuffer frame);
}