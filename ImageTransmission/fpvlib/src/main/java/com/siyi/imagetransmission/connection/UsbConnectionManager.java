package com.siyi.imagetransmission.connection;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.view.Surface;
import android.widget.Toast;

import com.siyi.imagetransmission.contract.parser.BaseParser;
import com.siyi.imagetransmission.contract.parser.RemoteControlParser;
import com.siyi.imagetransmission.contract.protocol.BaseProtocol;
import com.siyi.imagetransmission.contract.wrapper.BaseWrapper;
import com.siyi.imagetransmission.contract.wrapper.RemoteControlWrapper;
import com.siyi.imagetransmission.decoder.BaseDecoder;
import com.siyi.imagetransmission.decoder.SoftDecoder;
import com.siyi.imagetransmission.driver.DriverManager;
import com.siyi.imagetransmission.driver.ISerialDriver;
import com.siyi.imagetransmission.driver.SerialInputOutputManager;
import com.siyi.imagetransmission.driver.TestDriver;
import com.siyi.imagetransmission.driver.UsbSerialPort;
import com.siyi.imagetransmission.lib.R;
import com.siyi.imagetransmission.log.Logcat;
import com.siyi.imagetransmission.utils.DriverUtil;

import java.io.IOException;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by zhuzp on 2019/2/26
 *
 */
public class UsbConnectionManager extends BaseConnectManager {
    private static final String TAG = "UsbConnectionManager";

    /**
     * 请求usb权限的action
     */
    private static final String ACTION_REQUEST_DEVICE_PERMISSION = "com.siyi.image.ACTION_USB_PERMISSION";
    private static final int REQUEST_CODE_DEVICE_PERMISSION = 1000;

    private UsbManager mUsbManager;
    private Context mContext;
    private SerialInputOutputManager mSerialIoManager;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private BaseWrapper mWrapper;
    private BaseParser mParser;

    private PipedOutputStream mWriteOutputStream;

    private BaseDecoder mDecoder;
    private static volatile UsbConnectionManager sInstance = null;


    private UsbConnectionManager(Context context) {
        mContext = context;
        mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        mParser = new RemoteControlParser();
        mWriteOutputStream = new PipedOutputStream();
        initDecoder();
        mParser.setDecoder(mDecoder);
        registerReceiver();
    }

    public static UsbConnectionManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (UsbConnectionManager.class) {
                if (sInstance == null) {
                    sInstance = new UsbConnectionManager(context);
                }
            }
        }
        return sInstance;
    }

    private void initDecoder() {
        mDecoder = new SoftDecoder();
    }

    @Override
    public void onUsbAttached(UsbDevice usbDevice) {
        super.onUsbAttached(usbDevice);
        if (mDecoder == null) {
            initDecoder();
        }
        //step 1 判断是否有访问权限，如果没有权限则申请权限；
        if (checkPermission(usbDevice)) {
            connectDevice(usbDevice);
        }
    }

    public void onSurfaceCreate(Surface surface) {
        mDecoder.onSurfaceCreate(surface);
        //test();
    }

    public void onSurfaceDestroy(Surface surface) {
        if (mDecoder != null) {
            mDecoder.onSurfaceDestroy(surface);
        }
    }


    @Override
    public void onUsbDetached(UsbDevice usbDevice) {
        super.onUsbDetached(usbDevice);
        if (mDecoder != null) {
            mDecoder.stopDecode();
        }
        if (mWrapper != null) {
            mWrapper.release();
        }
        stopIoManager();
        mDecoder = null;
    }


    private boolean checkPermission(UsbDevice device) {
        if (!mUsbManager.hasPermission(device)) {
            // 增加PendingIntent的实现；
            registerPermissionReceiver();
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, REQUEST_CODE_DEVICE_PERMISSION,
            new Intent(ACTION_REQUEST_DEVICE_PERMISSION), PendingIntent.FLAG_UPDATE_CURRENT);
            mUsbManager.requestPermission(device, pendingIntent);
            return false;
        }
        return true;
    }


    /**
     * 注册权限广播
     */
    private void registerPermissionReceiver() {
        IntentFilter intentFilter = new IntentFilter(ACTION_REQUEST_DEVICE_PERMISSION);
        mContext.registerReceiver(mPermissionReceiver, intentFilter);
    }

    /**
     * 注销权限广播
     */
    private void unregisterPermissionReceiver() {
        try {
            mContext.unregisterReceiver(mPermissionReceiver);
        } catch (IllegalArgumentException e) {
            Logcat.e(TAG, e.getMessage());
        }
    }


    private void connectDevice(UsbDevice device) {
        Logcat.d(TAG, "device: " + device);
        //step 2，打开USB设备；
        ISerialDriver serialDriver = DriverManager.getInstance().getSeiralDirver(device);
        if (serialDriver != null) {
            List<UsbSerialPort> list = serialDriver.getPorts();
            UsbSerialPort usbSerialPort = list.get(0);
            UsbDeviceConnection connection = mUsbManager.openDevice(device);
            try {
                usbSerialPort.open(connection);
                usbSerialPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

            } catch (IOException e) {
                e.printStackTrace();
                try {
                    usbSerialPort.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                return;
            }
            startIoManager(usbSerialPort);

        } else {
            Logcat.e(TAG, "serial driver is null");
        }

    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Logcat.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            try {
                mSerialIoManager.getDriver().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mSerialIoManager = null;
        }
    }

    private void startIoManager(UsbSerialPort sPort) {
        if (sPort != null) {
            Logcat.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
            //mExecutor.submit(mSerialIoManager);
            mWrapper = new RemoteControlWrapper(mParser, sPort, mWriteOutputStream);

        }
    }

    private SerialInputOutputManager.Listener mListener = new SerialInputOutputManager.Listener() {
        @Override
        public void onNewData(byte[] data) {
            //Logcat.d(TAG, "data: " + ByteUtil.byteArray2Str(data));

            long current = System.currentTimeMillis();
            long diff = current - mCurrent;
            //Logcat.d(TAG, "diff >>>>>>>> " + diff + ", length: " + data.length);
            mCurrent = current;

        }

        @Override
        public void onRunError(Exception e) {

        }
    };


    private void write(BaseProtocol protocol) {
        mWrapper.write(protocol);
    }

    public void release() {
        if (mDecoder != null) {
            mDecoder.stopDecode();
            mDecoder = null;
        }
        if (mWrapper != null) {
            mWrapper.release();
            mWrapper = null;
        }
        if (mParser != null) {

            mParser.release();
            mParser = null;
        }
        stopIoManager();
        unregisterReceiver();
        unregisterPermissionReceiver();
        sInstance = null;
    }

    /**
     * 注册广播
     */
    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        mContext.registerReceiver(mReceiver, intentFilter);
    }

    /**
     * 注销广播
     */
    private void unregisterReceiver() {
        try {
            mContext.unregisterReceiver(mReceiver);
        } catch (Exception e) {
            Logcat.e(TAG, e.getLocalizedMessage());
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                //
                if (DriverUtil.isStm32Device(device)) {
                    onUsbAttached(device);
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)){
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (DriverUtil.isStm32Device(device)) {
                    onUsbDetached(device);
                }
            }

        }
    };

    private BroadcastReceiver mPermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_REQUEST_DEVICE_PERMISSION.equals(action)) {
                boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,
                        false);
                if (granted) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    connectDevice(device);
                } else {
                    //
                    Toast.makeText(mContext,  R.string.usb_granted_failed, Toast.LENGTH_SHORT).show();
                }
            }
        }
    };


    //test
    private long mCurrent;

    private void test() {
        mWrapper = new RemoteControlWrapper(mParser, new TestDriver(), mWriteOutputStream);
    }


    public long getByteCount() {
        if (mWrapper != null) {
            return mWrapper.getByteCode();
        }

        return 0;
    }

    public long getLossCount() {
        if (mWrapper != null) {
            return mParser.getLossCount();
        }
        return 0;
    }


}
