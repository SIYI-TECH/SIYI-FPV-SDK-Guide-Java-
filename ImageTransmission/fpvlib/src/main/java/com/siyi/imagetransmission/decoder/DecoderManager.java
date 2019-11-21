package com.siyi.imagetransmission.decoder;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;

/**
 * author: shymanzhu
 * data: on 2019-05-08
 */
public class DecoderManager {
//    BaseDecoder mDecoder;
//    public BaseDecoder getDecoder() {
//        //step1 判断是否支持硬解
//        MediaCodecList codecList = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
//        MediaCodecInfo[] infos = codecList.getCodecInfos();
//        for (MediaCodecInfo info : infos) {
//            if(info.isEncoder()){
//                continue;
//            }
//            String[] types = info.getSupportedTypes();
//            for (String type: types) {
//                if (MediaFormat.MIMETYPE_VIDEO_AVC.equals(type)) {
//                    mDecoder = new MainDecoder();
//
//                    return mDecoder;
//                }
//            }
//        }
//
//        mDecoder = new SoftDecoder();
//
//        return mDecoder;
//    }


}
