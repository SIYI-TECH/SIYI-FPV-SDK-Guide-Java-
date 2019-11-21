package com.siyi.imagetransmission.driver;

import android.hardware.usb.UsbDeviceConnection;
import android.os.Environment;


import com.siyi.imagetransmission.log.Logcat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by zhuzp on 2019/5/5
 * 通过读取H264文件模拟USB收到视频流
 */
public class TestDriver implements UsbSerialPort {
    private static final String TAG = "TestDriver";
    private static final String TEST_FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/1111.h264";;

    private RandomAccessFile mFileInputStream;

    public TestDriver() {
        try {
            mFileInputStream = new RandomAccessFile(TEST_FILE_PATH, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    @Override
    public ISerialDriver getDriver() {
        return null;
    }

    @Override
    public int getPortNumber() {
        return 0;
    }

    @Override
    public String getSerial() {
        return null;
    }

    @Override
    public void open(UsbDeviceConnection connection) throws IOException {

    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public int read(byte[] dest, int timeoutMillis) throws IOException {
        if (mFileInputStream != null) {
            try {
                Thread.sleep(timeoutMillis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try{
                int len = mFileInputStream.read(dest);
                if (len < 0) {
                    mFileInputStream.seek(0);
                    return 0;
                }
                return len;
            } catch (Exception e) {
                Logcat.e(TAG, e.getLocalizedMessage());
                return 0;
            }

        }
        return 0;
    }

    @Override
    public int write(byte[] src, int timeoutMillis) throws IOException {
        return 0;
    }

    @Override
    public void setParameters(int baudRate, int dataBits, int stopBits, int parity) throws IOException {

    }

    @Override
    public boolean getCD() throws IOException {
        return false;
    }

    @Override
    public boolean getCTS() throws IOException {
        return false;
    }

    @Override
    public boolean getDSR() throws IOException {
        return false;
    }

    @Override
    public boolean getDTR() throws IOException {
        return false;
    }

    @Override
    public void setDTR(boolean value) throws IOException {

    }

    @Override
    public boolean getRI() throws IOException {
        return false;
    }

    @Override
    public boolean getRTS() throws IOException {
        return false;
    }

    @Override
    public void setRTS(boolean value) throws IOException {

    }

    @Override
    public boolean purgeHwBuffers(boolean flushRX, boolean flushTX) throws IOException {
        return false;
    }
}
