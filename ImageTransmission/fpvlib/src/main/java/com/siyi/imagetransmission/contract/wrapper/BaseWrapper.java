package com.siyi.imagetransmission.contract.wrapper;

import com.siyi.imagetransmission.contract.parser.BaseParser;
import com.siyi.imagetransmission.contract.parser.RemoteControlParser;
import com.siyi.imagetransmission.contract.protocol.BaseProtocol;
import com.siyi.imagetransmission.driver.UsbSerialPort;
import com.siyi.imagetransmission.log.Logcat;
import com.siyi.imagetransmission.utils.ByteUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * author: shymanzhu
 * data: on 2018/11/4
 */
public abstract class BaseWrapper {
    private static final String TAG = "BaseWrapper";
    /**帧序号*/
    private int mSeq = 0;
    private BaseParser mBaseParser;
    private OutputStream mOutputStream;
    private InputStream mInputStream;

    private BlockingQueue<BaseProtocol> mWriteQueue = new LinkedBlockingDeque<>();
    /**已经写过的命令缓存到此队列，以便下次使用时从该队列中取出，然后改变Command的data重新利用*/
    private BlockingQueue<BaseProtocol> mWroteCmdQueue = new LinkedBlockingDeque<>();
    protected BlockingQueue<byte[]> mReadFullQueue = new LinkedBlockingQueue<>( 50 * 1024);

    private static final int WRITE_THREAD_WAIT_FULL_CMD_MS = 5;
    private static final int READ_THREAD_WAIT_FULL_CMD_MS = 5;
    public static final int READ_BUFF_LENGTH = 80 * 1024;
    private static final int READ_THREAD_IDLE_SLEEP_MS = 50;
    protected static final Object mAckSignal = new Object();
    private static final int WRITE_THREAD_WAIT_ACK_MS = 500;
    /**未收到ACK时最多发送的次数*/
    private static final int MAX_WRITE_RETRY_COUNT = 5;
    private boolean mStopWrite = false;

    private boolean mAcked = false;

    private boolean mStopRead = false;

    private boolean mStopProcess = false;

    /**
     * constructor
     * @param inputStream inputStream
     * @param outputStream outputStream
     */
    public BaseWrapper(BaseParser parser, InputStream inputStream, OutputStream outputStream) {
        mBaseParser = parser;
        if (mBaseParser == null) {
            mBaseParser = new RemoteControlParser();
        }
        mInputStream = inputStream;
        mOutputStream = outputStream;

        if (mInputStream != null) {
            new ReadThread().start();
            new ProcessThread().start();
        }
        if (mOutputStream != null) {
            new WriteThread().start();
        }
    }

    private UsbSerialPort mDriver;
    public BaseWrapper(BaseParser parser, UsbSerialPort driver, OutputStream outputStream) {
        mDriver = driver;
        mBaseParser = parser;
        mOutputStream = outputStream;
        if (mDriver != null) {
            new ReadThread().start();
            //new ProcessThread().start();
        }
        if (mOutputStream != null) {
            new WriteThread().start();
        }
    }

    /**
     * write command
     */
    public void write(BaseProtocol protocol) {
        try {
            mWriteQueue.put(protocol);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * get a protocol
     * @return wrote protocol from wrote queue
     */
    public BaseProtocol getWroteProtocol() {
        BaseProtocol command;
        try {
            command = mWroteCmdQueue.poll(WRITE_THREAD_WAIT_FULL_CMD_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            command = null;
        }
        if (command == null) {
            command = createProtocol();
        }
        return command;
    }

    /**
     * release
     */
    public void release() {
        try {
            if (mOutputStream != null) {
                mStopWrite = true;
                mOutputStream.close();
                mOutputStream = null;
            }
            mStopRead = true;
            if (mInputStream != null) {
                mInputStream.close();
                mInputStream = null;
            }
            mStopProcess = true;
            mSeq = 0;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 往socket写数据线程
     */
    private class WriteThread extends Thread {

        WriteThread() {
            super.setName("WriteThread");
        }

        @Override
        public void run() {
            super.run();
            while (!mStopWrite) {
                try {
                    BaseProtocol protocol = mWriteQueue.poll(WRITE_THREAD_WAIT_FULL_CMD_MS, TimeUnit.MILLISECONDS);
                    if (protocol == null)
                        continue;
                    protocol.setSeq(++mSeq);
                    sendData(protocol.getFullData());
//                    if (protocol.isNeedAck()) {
//                        mAcked = false;
//                        int retryCount = 1;
//                        while (!mAcked && retryCount <= MAX_WRITE_RETRY_COUNT) {
//                            synchronized (mAckSignal) {
//                                if (!mAcked) {
//                                    try {
//                                        mAckSignal.wait(WRITE_THREAD_WAIT_ACK_MS);
//                                    } catch (InterruptedException e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                            }
//
//                            // 没有ACK再次发送
//                            if (!mAcked) {
//                                protocol.setSeq(++mSeq);
//                                sendData(protocol.getFullData());
//                                Logcat.d(TAG, "retry send data, count = " + retryCount);
//                                retryCount++;
//                            }
//                        }
//                    }
                    mWroteCmdQueue.put(protocol);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * send to server
     *
     * @param data protocol full data
     */
    private void sendData(byte[] data) {
        Logcat.d(TAG, "send data: " + ByteUtil.byteArray2Str(data));
        if (mOutputStream != null) {
            try {
                mOutputStream.write(data);
                mOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Logcat.d(TAG, "mOutputStream = null");
        }
    }

    /**
     * 读取socket数据线程
     */
    private class ReadThread extends Thread {
        ReadThread() {
            super.setName("ReadThread");
        }

        byte[] mReadBuff = new byte[READ_BUFF_LENGTH];

        @Override
        public void run() {
            super.run();
            while (!mStopRead) {
                try {
/*                    if (mInputStream != null) {
                        int readLen = mInputStream.read(mReadBuff);
                        Logcat.d(TAG, "readLen >>>>>>> " + readLen);
                        if (readLen > 0) {
                            parseRawData(mReadBuff, readLen);
                        } else {
                            Thread.sleep(READ_THREAD_IDLE_SLEEP_MS);  // Unit is milliseconds
                        }
                    } else {
                        Logcat.d(TAG, "mInputStream = null!!!");
                    }*/
                    if (mDriver != null) {
                        int readLen = mDriver.read(mReadBuff, READ_THREAD_IDLE_SLEEP_MS);
                        long current = System.currentTimeMillis();
                        long diff = current - mCurrent;
                        mCurrent = current;
                        //Logcat.d(TAG, "diff >>>>>>> " + diff + ", command length: " + readLen);
                        if (readLen > 0) {
                            //parseRawData(mReadBuff, readLen);
                            mByteCount += readLen;
                            byte[] buff = ByteUtil.subBytes(mReadBuff, 0, readLen);
                            processCommand(buff);
                        } /*else {

                        }*/
                    } else {
                        Logcat.e(TAG, "mDriver = null!!!");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }

    /**
     * 将原始数据存入队列
     * @param buff
     */
    protected void putFullCommandForProcess(byte[] buff) {
        try {
            mReadFullQueue.put(buff);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class ProcessThread extends Thread {
        @Override
        public void run() {
            super.run();
            handleCmd();
        }
    }

    private void handleCmd() {
        while (!mStopProcess) {
            try {
                // 从队列中取出原始数组
                byte[] command = mReadFullQueue.poll(READ_THREAD_WAIT_FULL_CMD_MS, TimeUnit.MILLISECONDS);
                if (command != null) {
                    processCommand(command);
                }

                Thread.sleep(READ_THREAD_IDLE_SLEEP_MS);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //test
    private long mCurrent = 0;
    private long mByteCount = 0;

    /**
     *
     * @return
     */
    public long getByteCode() {
        long temp = mByteCount;
        mByteCount = 0;
        return temp;
    }

    /**
     * 创建Protocol对象
     * @return protocol对象
     */
    protected abstract BaseProtocol createProtocol();

    /**
     * @param buff 原始数据
     */
    protected abstract void processCommand(byte[] buff);



    protected abstract void parseRawData(byte[] buff, int len);

}
