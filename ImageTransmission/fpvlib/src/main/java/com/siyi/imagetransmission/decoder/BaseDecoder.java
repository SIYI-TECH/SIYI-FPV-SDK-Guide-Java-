package com.siyi.imagetransmission.decoder;

import android.view.Surface;
import android.view.SurfaceView;

/**
 * author: shymanzhu
 * data: on 2019/3/23
 */
public class BaseDecoder {



    /**
     * 将视频数据字节流解码
     * @param videoData 视频数据字节流
     */
    public void decode(byte[] videoData) {

    }

    /**
     * 停止解码
     */
    public void stopDecode() {

    }

    /**
     * 更新Surface
     * @param surface
     */
    public void onSurfaceCreate(Surface surface) {

    }

    public void onSurfaceDestroy(Surface surface) {

    }

    /**
     * 更新丢包数量
     * @param count
     */
    public void updateLossCount(long count) {

    }

}
