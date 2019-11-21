package com.siyi.imagetransmission.driver;

import android.hardware.usb.UsbDevice;

import java.util.List;

/**
 * Created by zhuzp on 2019/3/5
 */
public interface ISerialDriver {

    /**
     * Returns the raw {@link UsbDevice} backing this port.
     *
     * @return the device
     */
    public UsbDevice getDevice();

    /**
     * Returns all available ports for this device. This list must have at least
     * one entry.
     *
     * @return the ports
     */
    public List<UsbSerialPort> getPorts();
}
