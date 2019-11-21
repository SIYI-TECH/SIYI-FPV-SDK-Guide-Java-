package com.siyi.imagetransmission.contract.wrapper;

import com.siyi.imagetransmission.contract.parser.BaseParser;
import com.siyi.imagetransmission.contract.parser.RemoteControlParser;
import com.siyi.imagetransmission.contract.protocol.BaseProtocol;
import com.siyi.imagetransmission.contract.protocol.RemoteControlProtocol;
import com.siyi.imagetransmission.driver.UsbSerialPort;
import com.siyi.imagetransmission.log.Logcat;
import com.siyi.imagetransmission.utils.ByteUtil;

import java.io.InputStream;
import java.io.OutputStream;


/**
 * Created by shymanzhu
 * 遥控协议封装器
 */

public class RemoteControlWrapper extends BaseWrapper {
    private static final String TAG = "RemoteControlWrapper";
    private byte[] mParseRawData = null; // 缓存数据
    private BaseParser mParser;


    /**
     * constructor
     *
     * @param parser
     * @param inputStream
     * @param outputStream
     */
    public RemoteControlWrapper(BaseParser parser, InputStream inputStream, OutputStream outputStream) {
        super(parser, inputStream, outputStream);
        mParser = parser;
    }


    public RemoteControlWrapper(BaseParser parser, UsbSerialPort driver, OutputStream outputStream) {
        super(parser, driver, outputStream);
        mParser = parser;
    }

    @Override
    protected BaseProtocol createProtocol() {
        return new RemoteControlProtocol();
    }

    @Override
    protected void processCommand(byte[] buff) {
        //Logcat.d(TAG, "process command " + ByteUtil.byteArray2Str(buff) + ", len: " + buff.length);
        if (null == buff) {
            Logcat.d(TAG, "buff = null !!! ");
            return;
        }

        if (mParseRawData != null) {
            buff = ByteUtil.splice(mParseRawData, buff);
            mParseRawData = null;
        }
        packagingData(buff);
    }

    /**
     * 拼接数据，过来的多条数据才可能是一条完整的数据；
     * 也可能是一条数据包含多条协议数据，所以需要组包或者拆包。
     * @param buff 原始数据
     */
    private void packagingData(byte[] buff) {
        //Logcat.d(TAG, "packagingData " + ByteUtil.byteArray2Str(buff) + ", len: " + buff.length);

        if (buff.length >= RemoteControlProtocol.Head.HEAD_LEN +
                RemoteControlProtocol.Command.CMD_LEN + RemoteControlProtocol.CHECK_SUM_CRC_LEN) {
            byte[] head = ByteUtil.subBytes(buff, 0, RemoteControlProtocol.Head.HEAD_LEN);
            int stx = (head[0] << 8 & 0xff00) | (head[1] & 0xff);
            // step 1, 得到数据头，起始位是否正确
            if (stx == RemoteControlProtocol.Head.STX) {
                // step 2 判断是否Ack 包；
                boolean isAck = ((head[2] >>> 1) & 0x01) == 1;
                //Logcat.d(TAG, "isAck: " + isAck);
                //step 3, 得到数据位长度，判断数据长度是否完整
                int dataLen = (head[3] & 0xff) | (head[4] << 8 & 0xff00);
                //Logcat.d(TAG, "data len = " + dataLen);



                int fullDataLen = RemoteControlProtocol.Head.HEAD_LEN + RemoteControlProtocol.CHECK_SUM_CRC_LEN
                                + RemoteControlProtocol.Command.CMD_LEN + dataLen;
                int buffLen = buff.length;
                if (buffLen == fullDataLen) {
                    // 得到了一条数据长度完整的数据，
                    //Logcat.d(TAG, "get a full data: " + ByteUtil.byteArray2Str(buff));
                    if (RemoteControlProtocol.checkProtocolValid(buff)
                            == RemoteControlProtocol.VALID_PROTOCOL) {
                        mParser.parse(buff);
                    } else {
                        Logcat.e(TAG, "invalid protocol !!!");
                    }
                    mParseRawData = null;
                } else if (buffLen < fullDataLen){
                    // step 3 ,数据长度不完整，继续缓存
                    mParseRawData = buff;
                } else {
                    // step 4, 切包
                    byte[] protocol = ByteUtil.subBytes(buff, 0, fullDataLen); // 一条完整的协议
                    if (RemoteControlProtocol.checkProtocolValid(protocol)
                            == RemoteControlProtocol.VALID_PROTOCOL) {
                        mParser.parse(protocol);
                    } else {
                        Logcat.e(TAG, "invalid protocol from cut package !!!");
                    }
                    // step 5, 切完剩下的buffer重新判断
                    int leftLen = buffLen - fullDataLen;
                    byte[] left = ByteUtil.subBytes(buff, fullDataLen, leftLen);
                    packagingData(left);
                }
            } else {
                int headIndex = findHeadIndex(mParseRawData);
                Logcat.e(TAG, "wrong protocol, index:  "+ headIndex + ", package: "+ ByteUtil.byteArray2Str(buff));
                if (headIndex != -1) {
                    byte[] re = ByteUtil.subBytes(mParseRawData, headIndex,
                            mParseRawData.length - headIndex);
                    //packagingData();
                }
                mParseRawData = null;
            }

        } else { //数据长度不满最小长度时，先缓存。
            mParseRawData = buff;
        }

    }

    @Override
    protected void parseRawData(byte[] buff, int len) {
        if (null != buff && 0 != len) {
            putFullCommandForProcess(ByteUtil.subBytes(buff, 0, len)); // 读取线程什么都不做，直接丢给处理线程
        }
    }

    private int findHeadIndex(byte[] data) {
        int headIndex = -1;
        if (data != null) {
            int len = data.length;
            for (int i = 0; i < len; i+=2) { //head 为 0x5566
                Logcat.d(TAG, "head: " + ByteUtil.byteArray2Str(data, i, 2));
                int stx = (data[i] << 8 & 0xff00) | (data[i+1] & 0xff) ;
                if (stx == RemoteControlProtocol.Head.STX) {
                    headIndex = i;
                    break;
                }
            }

        }

        return headIndex;
    }


}
