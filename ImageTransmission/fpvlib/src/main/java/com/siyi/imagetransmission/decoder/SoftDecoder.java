package com.siyi.imagetransmission.decoder;

import android.view.Surface;

import com.siyi.imagetransmission.log.Logcat;



/**
 * 软解码流程
 * step1 初始化解码器{@link #initDecode() }
 * step2 设置surface {@link #onSurfaceCreate(Surface)}
 * step3 将数据给解码器解码{@link #decode(byte[])}
 * step4 解码结束时，通知解码器结束{@link #stopDecode()}
 */
public class SoftDecoder extends BaseDecoder {
    private static final String TAG = "SoftDecoder";
    private final Object mLock = new Object();
    static {
        System.loadLibrary("softdecoder");
    }

    public SoftDecoder() {
        int initResult = initDecode();
        Logcat.d(TAG, "initResult: " + initResult);
    }


    @Override
    public void decode(byte[] videoData) {
        super.decode(videoData);
        synchronized (mLock) {
            if (videoData == null || videoData.length <=0) {
                return;
            }
            int decodeResult = nativeDecode(videoData, videoData.length);
            Logcat.d(TAG, "decodeResult: " + decodeResult);
        }

    }

    @Override
    public void stopDecode() {
        super.stopDecode();
        nativeStopDecode();
    }

    @Override
    public void onSurfaceCreate(Surface surface) {
        super.onSurfaceCreate(surface);
        Logcat.d(TAG, "onSurfaceCreate.....");
        synchronized (mLock) {
            nativeUpdateSurface(surface);
        }
    }

    @Override
    public void onSurfaceDestroy(Surface surface) {
        super.onSurfaceDestroy(surface);
        Logcat.d(TAG, "onSurfaceDestroy.....");
        synchronized (mLock) {
            nativeOnSurfaceDestroy(surface);
        }
    }

    @Override
    public void updateLossCount(long count) {
        super.updateLossCount(count);
        nativeUpdateLossCount(count);
    }

    /**
     * 初始化解码器
     */
    private native int initDecode();

    /**
     * 设置Surface
     * @param surface
     */
    private native int nativeUpdateSurface(Surface surface);

    private native int nativeOnSurfaceDestroy(Surface surface);

    /**
     * 将视频数据丢给解码器解码
     * @param frameData
     * @return
     */
    private native int nativeDecode(byte[] frameData, int len);

    /**
     * 停止解码
     */
    private native void nativeStopDecode();

    /**
     * 更新丢包数量
     * @param count 丢包总数
     */
    private native void nativeUpdateLossCount(long count);


}
