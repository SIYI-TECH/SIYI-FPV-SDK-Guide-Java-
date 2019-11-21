package com.siyi.imagetransmission.utils;

import android.hardware.usb.UsbDevice;

import com.siyi.imagetransmission.driver.UsbId;
import com.siyi.imagetransmission.log.Logcat;

/**
 * Created by zhuzp on 2019/4/18
 */
public class DriverUtil {
    private static final String TAG = "DriverUtil";

    /**
     * check if is stm32 usb device
     * @param device
     * @return
     */
    public static boolean isStm32Device(UsbDevice device) {
        int pid = device.getProductId();
        int vid = device.getVendorId();
        if (pid == UsbId.PRODUCT_STM32 && vid == UsbId.VENDOR_STM32) {
            return true;
        }
        Logcat.e(TAG, "not match device: " + device);
        return false;
    }
}
