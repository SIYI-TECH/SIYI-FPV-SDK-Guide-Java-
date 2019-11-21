package com.siyi.imagetransmission.contract.parser;

import com.siyi.imagetransmission.contract.protocol.RemoteControlProtocol;
import com.siyi.imagetransmission.log.Logcat;
import com.siyi.imagetransmission.utils.ByteUtil;


/**
 * Created by zhuzhipeng on 2018/2/11.
 */

public class RemoteControlParser extends BaseParser {
    private static final String TAG = "RemoteControlParser";

    public RemoteControlParser() {

    }


    @Override
    public synchronized void parse(final byte[] protocol) {
        int cmdId = protocol[RemoteControlProtocol.CMDID_START_INDEX] & 0xff;
        int len = protocol[RemoteControlProtocol.DATA_LEN_START_INDEX] & 0xff
                | (protocol[RemoteControlProtocol.DATA_LEN_START_INDEX + 1] << 8 & 0xff00); //长度占两个字节

        //Logcat.d(TAG, "protocol len: " + protocol.length + ", data length: " + len + ", protocol: " + ByteUtil.byteArray2Str(protocol));

        byte[] data = ByteUtil.subBytes(protocol, RemoteControlProtocol.DATA_START_INDEX, len);

        //Logcat.d(TAG, "datalen>>>>>>>: " + data.length + "data = " + ByteUtil.byteArray2Str(data));

//        int seq = (protocol[5] & 0xff) | (protocol[6] << 8 & 0xff00);
//        if (mSeq != 0) {
//            updateLossCount(seq);
//        }
//        mSeq = seq;
        //todo 解析到一条协议后需要增加ACK
        if (len != data.length) {
            Logcat.e(TAG, "length from protocol not equals to data real length");
            return;
        }
        switch (cmdId) {
            case RemoteControlProtocol.Command.ID.IMAGE_DATA:
                parseImage(data);
                break;
            default:
                break;
        }
    }



    /**
     * 解析图传视频数据
     *
     * @param data data 数据
     */
    private void parseImage(byte[] data) {
        Logcat.d(TAG, "data = " + ByteUtil.byteArray2Str(data));
        int len = data.length;
        byte[] imageData = ByteUtil.subBytes(data, 0, len - 2);
        //Logcat.d(TAG, "image data = " + ByteUtil.byteArray2Str(imageData));
        Logcat.d(TAG, "data len: " + len + "， image len: " + imageData.length);
        int seq = (data[len-2] & 0xff) | (data[len-1] << 8 & 0xff00);
        if (mSeq != 0) {
            updateLossCount(seq);
        }
        mSeq = seq;
        //todo 解码视频数据
        if (mDecoder != null) {
            mDecoder.decode(imageData);
        }

    }

    /**
     * 更新丢包数量
     * @param seq
     */
    private void updateLossCount(int seq) {
        Logcat.d(TAG, "seq: " + seq + ", mSeq: " + mSeq);
        if (seq < mSeq) {
            mLossCount += 0xFFFF - mSeq;
        } else {
            mLossCount += (seq - mSeq - 1);
            if (mDecoder != null) {
                mDecoder.updateLossCount(mLossCount);
            }
        }

    }



    private long mSeq = 0;

    private long mLossCount = 0;
    public long getLossCount() {
        return mLossCount;
    }


}
