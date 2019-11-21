package com.siyi.imagetransmission.utils;

import java.util.Arrays;

/**
 * Created by zhuzhipeng on 2018/3/9.
 */

public class ByteUtil {
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    private ByteUtil(){}

    /**
     *
     * @param bytes
     * @return
     */
    public static String byteArray2Str(byte[] bytes) {
        if (bytes == null){
            return null;
        }
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);

    }

    /**
     *
     * @param bytes
     * @param start
     * @param len
     * @return
     */
    public static String  byteArray2Str(byte[] bytes, int start, int len) {
        byte[] data = Arrays.copyOfRange(bytes, start, start + len);
        return byteArray2Str(data);
    }

    /**
     *
     * 返回字节数组子字节
     * @param src 原数组
     * @param start 起始位
     * @param len 长度
     * @return
     */
    public static byte[] subBytes(byte[] src, int start, int len) {
        return Arrays.copyOfRange(src, start, start + len);
    }

    public static byte[] subBytesStartEnd(byte[] src, int start, int end) {
        return Arrays.copyOfRange(src, start, end);
    }

    /**
     * 拼接数组
     * @param src1 源数组1
     * @param src2 源数组2
     * @return 拼接后的数组
     */
    public static byte[] splice(byte[] src1, byte[] src2){
        if (src1 == null || src2 == null) {
            throw new IllegalArgumentException("source byte can't be null !");
        }
        int len1 = src1.length;
        int len2 = src2.length;
        byte[] copy = new byte[len1 + len2];
        System.arraycopy(src1, 0, copy, 0, len1);
        System.arraycopy(src2, 0, copy, len1, len2);
        return copy;
    }

    public static byte[] copy(byte[] source) {
        if (source == null) {
            return null;
        }
        byte[] dest = new byte[source.length];
        System.arraycopy(source, 0, dest, 0, source.length);
        return dest;
    }

}
