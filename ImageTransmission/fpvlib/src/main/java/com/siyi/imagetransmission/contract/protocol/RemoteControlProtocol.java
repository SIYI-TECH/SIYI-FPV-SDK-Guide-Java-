package com.siyi.imagetransmission.contract.protocol;

import android.support.annotation.IntDef;

import com.siyi.imagetransmission.log.Logcat;
import com.siyi.imagetransmission.utils.ByteUtil;


/**
 * Created by zhuzhipeng on 2018/3/7.
 * protocol contract
 *
 * 字段	    索引	字节大小	内容说明
 * STX	    0	       2	    0x5566为起始标志
 * CTRL	    2	       1	    0：need_ack  当前数据包是否需要ack
 *                              1：ack_pack  此包是否为ack包
 *                              2-7：预留
 * Data_len	3	       2	    数据域字节长度    低字节在前
 * SEQ	    5	       2	    帧的序列,范围(0~65535)  低字节在前
 * CMD_ID	7	       1	    命令ID
 * DATA	    8	     Data_len	数据
 * CRC16	           2	    整个数据包的CRC16校验   低字节在前
 *
 *
 */
public class RemoteControlProtocol extends BaseProtocol {
    private static final String TAG = "RemoteControlProtocol";

    /**
     *  整包校验值错误
     */
    public static final int WRONG_PROTOCOL_ENCRYPT = -1;
    /**
     * 有效的协议数据
     */
    public static final int VALID_PROTOCOL = 1;

    /**
     * 整包CRC16时，整包校验位长度
     */
    public static final int CHECK_SUM_CRC_LEN = 2;
    /**
     * 整包数据中数据段长度起始位
     */
    public static final int DATA_LEN_START_INDEX = 3;
    /**
     * 数据段采用2个字节表示数据段长度
     */
    public static final int DATA_SIZE_LEN = 2;

    /**
     * 整包数据中CMDID起始位
     */
    public static final int CMDID_START_INDEX = 7;

    /**
     * 整包数据中数据段起始位
     */
    public static final int DATA_START_INDEX = 8;


    /**
     * 协议头
     */
    private Head mHead;
    /**
     * 协议命令字段
     */
    private Command mCommand;
    /**
     * 协议数据内容
     */
    public byte[] mData;




    public RemoteControlProtocol() {
        mHead = new Head();
        mCommand = new Command();
        mData = null;
    }

    public void setHead(Head head) {
        mHead = head;
    }

    public boolean isNeedAck() {
        if (mHead != null) {
            return mHead.isNeedAck();
        }
        return false;
    }

    public Head getHead() {
        return mHead;
    }

    @Override
    public BaseProtocol createProtocol() {
        return null;
    }

    public void setCommand(Command command) {
        mCommand = command;
    }

    public Command getCommand() {
        return mCommand;
    }

    public void setData(byte[] data) {
        mData = data;
    }

    @Override
    public void setSeq(int seq) {
        if (mCommand != null) {
            mCommand.setSeq(seq);
        } else {
            Logcat.d(TAG, "Protocol command = null!");
        }
    }

    public byte[] getFullData() {
        return genFullData(mHead, mCommand, mData);
    }

    @Override
    public byte[] genFullData(byte[] data) {
        return genFullData(mHead, mCommand, data);
    }

    /**
     * 协议头字段
     *              |<-----------heard------------->|
     * 字段        | STX        CTRL	 Data_LEN	|
     * len        | 2           1        2          |
     *
     * STX:    0x5566
     * CTRL:
     * 0：need_ack  需要ack
     * 1：ack_pack  此包是ack包
     * 2-7：预留
     * Data_LEN：
     * 为此包数据域的长度（低字节在前）
     *
     */
    public static class Head {
        public static final int STX = 0x5566;
        /**
         * 头包字节长度
         */
        public static final int HEAD_LEN = 5;
        /**
         * 是否需要ACK
         */
        private boolean mNeedAck = false;
        /**
         * 此数据包是否是ACK包
         */
        private boolean mIsAck = false;
        /**
         * 数据段长度
         */
        private int mDataLen;

        public Head() {
            mNeedAck = false;
            mIsAck = false;
            mDataLen = 0;
        }
        /**
         * 设置数据位长度
         * @param len
         */
        public void setDataLen(int len) {
            mDataLen = len;
        }

        public void setNeedAck(boolean needAck){
            mNeedAck = needAck;
        }

        public void setAck(boolean ack) {
            mIsAck = ack;
        }

        public boolean isNeedAck() {
            return mNeedAck;
        }

        public boolean isAck() {
            return mIsAck;
        }

        public byte[] toBytes() {
            byte[] head = new byte[HEAD_LEN];
            head[0] = (byte) (STX & 0xff);
            head[1] = (byte) ((STX >>> 8) & 0xff);
            int ctrl = 0;
            if (mNeedAck) {
                ctrl |= 0x01;
            }
            if (mIsAck) {
                ctrl |= 0x02;
            }
            head[2] = (byte) (ctrl & 0xff);
            head[3] = (byte) (0xff & mDataLen);
            head[HEAD_LEN - 1] = (byte) (0xff & (mDataLen >>> 8));
            return head;
        }

        /**
         * 校验类型
         */
        public static class Verify {
            /**
             * 固定校验位，头校验位默认为0x55，尾校验位0x66
             */
            public static final int BUILT_IN = 0;
            /**
             * Cksum  效验和 1个字节，
             */
            public static final int CHECK_SUM = 1;
            /**
             * 头部CRC8效验，整个数据包CRC16
             */
            public static final int CRC = 2;

            public static boolean isValidVerify(int value) {
                return value == BUILT_IN || value == CHECK_SUM || value == CRC;
            }

            @IntDef({BUILT_IN, CHECK_SUM, CRC})
            @interface Type {}
        }


        /**
         * 加密方式
         */
        public static class Encrypt {
            /**
             * 不加密
             */
            public static final int NONE = 0;
            /**
             * 异或加密
             */
            public static final int XOR = 1;

            @IntDef({NONE, XOR})
            @interface Type{}

        }

        public static class Version {
            /**
             * V0版本，精简版本，只有CMD ID，用于高频数据传输
             */
            public static final int VER0 = 0;

            /**
             * 1-	V1版本  包含SEQ、CMD ID
             */
            public static final int VER1 = 1;

            /**
             * 2-	V2版本，大部分数据用于此协议版本传输
             */
            public static final int VER2 = 2;

            public static boolean isValidVersion(int version) {
                return version == VER0 || version == VER1 || version == VER2;
            }

            @IntDef({VER0, VER1, VER2})
            @interface Type{}
        }

    }

    /**
     * 协议命令字段
     * |<-----CMD--->|
     * |SEQ	  CMD_ID|
     * |2     1    |
     *
     * SEQ:
     * 帧的序列,范围(0~65535)（低字节在前）
     * CMD_ID:
     * 此包为上面所述功能命令集中的具体命令ID
     */
    public static class Command {

        public interface ID{
            /**
             * 图传数据
             */
            int IMAGE_DATA = 0x23;
        }

        public static final int CMD_LEN = 3;
        /**CMD_ID*/
        private int mCmdId = 0;
        /**帧序列，用于标识是否有丢帧*/
        private int mSeq = 0;

        public int getCmdId() {
            return mCmdId;
        }

        public long getSeq() {
            return mSeq;
        }

        public void setCmdId(int cmdId) {
            mCmdId = cmdId;
        }

        public void setSeq(int seq) {
            //Logcat.d(TAG, "seq = " + seq);
            mSeq = seq;
        }

        public byte[] toBytes() {
            byte[] bytes = new byte[CMD_LEN];
            bytes[0] = (byte) (mSeq & 0xff);
            bytes[1] = (byte) ((mSeq >>> 8) & 0xff);
            bytes[CMD_LEN-1] = (byte) (mCmdId & 0xff);
            return bytes;
        }

    }


    private static byte[] genFullData(Head head, Command command, byte[] data) {
        byte[] headBytes = head.toBytes();
        byte[] cmdBytes = command.toBytes();
        byte[] fullData;
        int len;
        if (data != null) {
            len = headBytes.length + cmdBytes.length + data.length + 2; // 整个包校验值占两个字节
            fullData = new byte[len];
            System.arraycopy(data, 0, fullData, headBytes.length + cmdBytes.length, data.length);
        } else {
            len = headBytes.length + cmdBytes.length + 2;
            fullData = new byte[len];
        }

        System.arraycopy(headBytes, 0, fullData, 0, headBytes.length);
        System.arraycopy(cmdBytes, 0, fullData, headBytes.length, cmdBytes.length);
        int packCheck = calculateCRC16(fullData, len -2);
        fullData[len -2] = (byte) (packCheck & 0xff);
        fullData[len -1] = (byte) (packCheck >>>8 & 0xff);
        return fullData;
    }



    /**
     * 生成心跳包协议对象
     * @param protocol wrote protocol
     * @return updated protocol
     */
    public static BaseProtocol genHeartBeat(RemoteControlProtocol protocol) {
        // 更新协议头
        Head head = protocol.getHead();
        head.setAck(false);
        head.setNeedAck(false);
        head.setDataLen(0);
        protocol.setHead(head);
        // 更新协议command
        Command command = protocol.getCommand();
        protocol.setCommand(command);
        // 更新协议数据段
        protocol.setData(null);
        return protocol;
    }

    /**
     * 生成请求设备信息协议对象
     * @param protocol wrote protocol
     * @return updated protocol
     */
    public static BaseProtocol genRequestDeviceInfo(RemoteControlProtocol protocol) {
        // 更新协议头
        Head head = protocol.getHead();
        head.setAck(false);
        head.setNeedAck(true);
        head.setDataLen(0);
        protocol.setHead(head);
        // 更新协议command
        Command command = protocol.getCommand();
        protocol.setCommand(command);
        // 更新协议数据段
        protocol.setData(null);
        return protocol;
    }


    /**
     * 生成图传设置协议
     * @param protocol
     * @return
     */
    public static BaseProtocol genRequestImage(RemoteControlProtocol protocol) {
        //更新协议头
        Head head = protocol.getHead();
        head.setAck(false);
        head.setNeedAck(true);
        head.setDataLen(0);
        protocol.setHead(head);

        // 更新协议command
        Command command = protocol.getCommand();
        command.setCmdId(Command.ID.IMAGE_DATA);
        protocol.setCommand(command);
        protocol.setData(null);
        return protocol;
    }


    /**
     * 判断协议数据是否合法
     * @param buff 原始协议数据
     * @return 判断结果
     */
    public static int checkProtocolValid(byte[] buff) {
        // 整包数据校验值是否正确
        byte[] dataVerify = ByteUtil.subBytes(buff, buff.length - 2, 2);
        int dataEntry = ((dataVerify[1] & 0xff) << 8) | (dataVerify[0] & 0xff);
        int calculate = BaseProtocol.calculateCRC16(buff, buff.length - 2);
        if (dataEntry != calculate) {
            Logcat.e(TAG, "dataEntry from protocol not equal: " + dataEntry + ", calculate value: " + calculate);
            return WRONG_PROTOCOL_ENCRYPT;
        }
        return VALID_PROTOCOL;
    }

}
