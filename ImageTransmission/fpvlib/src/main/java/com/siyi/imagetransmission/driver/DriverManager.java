package com.siyi.imagetransmission.driver;

import android.hardware.usb.UsbDevice;

import java.util.Map;

/**
 * Created by zhuzp on 2019/3/5
 */
public class DriverManager {
    private DriverManager() {

    }

    private static class ManagerHolder {
        static final DriverManager INSTANCE = new DriverManager();
    }

    public static DriverManager getInstance() {
        return ManagerHolder.INSTANCE;
    }

    public ISerialDriver getSeiralDirver(UsbDevice device) {
        int pid = device.getProductId();
        int vid = device.getVendorId();
        Map<Integer, int[]> cdcMap = CdcAcmSerialDriver.getSupportedDevices();
        for (int vendorId : cdcMap.keySet()) {
            if (vid == vendorId) {
                int[] pids = cdcMap.get(vendorId);
                int len = pids.length;
                for (int i = 0; i < len; i++) {
                    if (pids[i] == pid) {
                        return new CdcAcmSerialDriver(device);
                    }
                }
            }
        }
        return null;
    }

}
