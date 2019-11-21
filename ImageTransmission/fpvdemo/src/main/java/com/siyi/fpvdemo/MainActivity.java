package com.siyi.fpvdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.siyi.imagetransmission.connection.UsbConnectionManager;
import com.siyi.imagetransmission.log.Logcat;
import com.siyi.imagetransmission.utils.DriverUtil;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private SurfaceView mSurfaceView;
    private UsbConnectionManager mUsbConnectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mUsbConnectionManager = UsbConnectionManager.getInstance(this);
        checkUsbConnectState();
        initView();
    }

    private void initView() {
        mSurfaceView = findViewById(R.id.surface);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mUsbConnectionManager.onSurfaceCreate(holder.getSurface());
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mUsbConnectionManager.onSurfaceDestroy(holder.getSurface());
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUsbConnectionManager.release();
    }

    /**
     * 判断USB连接状态
     */
    private void checkUsbConnectState() {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        int deviceCount = deviceList.size();
        if (deviceCount > 0) {
            for (UsbDevice device : deviceList.values()) {
                if (DriverUtil.isStm32Device(device)) {
                    mUsbConnectionManager.onUsbAttached(device);
                    break;
                }
            }
        } else {
            Logcat.d(TAG, "no usb device attached");
        }
    }


}
