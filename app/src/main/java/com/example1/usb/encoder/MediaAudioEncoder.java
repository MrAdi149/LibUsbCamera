package com.example1.usb.encoder;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MediaAudioEncoder extends MediaEncoder implements IAudioEncoder {
    private static final boolean DEBUG = true;    // TODO set false on release
    private static final String TAG = "MediaAudioEncoder";

    private static final String MIME_TYPE = "audio/mp4a-latm";
    public static final int SAMPLE_RATE = 44100;    // 44.1[KHz] is only setting guaranteed to be available on all devices.
    private static final int BIT_RATE = 64000;
    public static final int SAMPLES_PER_FRAME = 1024;    // AAC, bytes/frame/channel
    public static final int FRAMES_PER_BUFFER = 25;    // AAC, frame/buffer/sec

    private AudioThread mAudioThread = null;

    public MediaAudioEncoder(final MediaMuxerWrapper muxer, final MediaEncoderListener listener) {
        super(muxer, listener);
    }

    @Override
    protected void prepare() throws IOException {
        if (DEBUG) Log.v(TAG, "prepare:");
        mTrackIndex = -1;
        mMuxerStarted = mIsEOS = false;
        // prepare MediaCodec for AAC encoding of audio data from inernal mic.
        final MediaCodecInfo audioCodecInfo = selectAudioCodec(MIME_TYPE);
        if (audioCodecInfo == null) {
            Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
            return;
        }
        if (DEBUG) Log.i(TAG, "selected codec: " + audioCodecInfo.getName());

        final MediaFormat audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, 1);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
//		audioFormat.setLong(MediaFormat.KEY_MAX_INPUT_SIZE, inputFile.length());
//      audioFormat.setLong(MediaFormat.KEY_DURATION, (long)durationInMs );
        if (DEBUG) Log.i(TAG, "format: " + audioFormat);
        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
        if (DEBUG) Log.i(TAG, "prepare finishing");
        if (mListener != null) {
            try {
                mListener.onPrepared(this);
            } catch (final Exception e) {
                Log.e(TAG, "prepare:", e);
            }
        }
    }

    @Override
    protected void startRecording() {
        super.startRecording();
        // create and execute audio capturing thread using internal mic
        if (mAudioThread == null) {
            mAudioThread = new AudioThread(mAudioThread.context);
            mAudioThread.start();
        }
    }

    @Override
    protected void release() {
        mAudioThread = null;
        super.release();
    }

    private static final int[] AUDIO_SOURCES = new int[]{
            MediaRecorder.AudioSource.DEFAULT,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.CAMCORDER,
    };

    private class AudioThread extends Thread {
        private Context context;

        public AudioThread(Context context) {
            this.context = context;
        }

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
            int cnt = 0;
            final int minBufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            int bufferSize = SAMPLES_PER_FRAME * FRAMES_PER_BUFFER;
            if (bufferSize < minBufferSize)
                bufferSize = ((minBufferSize / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;
            final ByteBuffer buf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME).order(ByteOrder.nativeOrder());
            AudioRecord audioRecord = null;

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                for (final int src : AUDIO_SOURCES) {
                    try {
                        audioRecord = new AudioRecord(src,
                                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
                        if (audioRecord != null) {
                            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                                audioRecord.release();
                                audioRecord = null;
                            }
                        }
                    } catch (final Exception e) {audioRecord = null;
                    }
                    if (audioRecord != null) {
                        break;
                    }
                }
            } else {
                // Handle the case where permission is not granted, e.g., request permission
                Log.e(TAG, "Audio recording permission denied");
                return; // Exit the thread if no permission
            }

            if (audioRecord != null) {
                try {
                    if (mIsCapturing) {
                        if (DEBUG) Log.v(TAG, "AudioThread:start audio recording");
                        int readBytes;
                        audioRecord.startRecording();
                        try {
                            for (; mIsCapturing && !mRequestStop && !mIsEOS; ) {
                                buf.clear();
                                try {
                                    readBytes = audioRecord.read(buf, SAMPLES_PER_FRAME);
                                } catch (final Exception e) {
                                    break;
                                }
                                if (readBytes > 0) {
                                    buf.position(readBytes);
                                    buf.flip();
                                    encode(buf, readBytes, getPTSUs());
                                    frameAvailableSoon();
                                    cnt++;
                                }
                            }
                            if (cnt > 0) {
                                frameAvailableSoon();
                            }
                        } finally {
                            audioRecord.stop();
                        }
                    }
                } catch (final Exception e) {
                    Log.e(TAG, "AudioThread#run", e);
                } finally {
                    audioRecord.release();
                }
            }

            if (cnt == 0) {
                for (int i = 0; mIsCapturing && (i < 5); i++) {
                    buf.position(SAMPLES_PER_FRAME);
                    buf.flip();
                    try {
                        encode(buf, SAMPLES_PER_FRAME, getPTSUs());
                        frameAvailableSoon();
                    } catch (final Exception e) {
                        break;
                    }
                    synchronized (this) {
                        try {
                            wait(50);
                        } catch (final InterruptedException e) {
                            // Handle interruption
                        }
                    }
                }
            }
            if (DEBUG) Log.v(TAG, "AudioThread:finished");
        }}

    private static MediaCodecInfo selectAudioCodec(final String mimeType) {
        if (DEBUG) Log.v(TAG, "selectAudioCodec:");

        MediaCodecInfo result = null;
        // get the list of available codecs
        final int numCodecs = MediaCodecList.getCodecCount();
        LOOP:	for (int i = 0; i < numCodecs; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {	// skipp decoder
                continue;
            }
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (DEBUG) Log.i(TAG, "supportedType:" + codecInfo.getName() + ",MIME=" + types[j]);
                if (types[j].equalsIgnoreCase(mimeType)) {
                    if (result == null) {
                        result = codecInfo;
                        break LOOP;
                    }
                }
            }
        }
        return result;
    }

}