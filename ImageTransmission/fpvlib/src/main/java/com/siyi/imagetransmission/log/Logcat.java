package com.siyi.imagetransmission.log;

import android.util.Log;

/**
 * Created by zhuzp on 2019/2/14
 */
public class Logcat {
    private static boolean sLogEnable = true;
    public static void d(String tag, String msg) {
        if (sLogEnable) {
            Log.d(tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (sLogEnable) {
            Log.i(tag, msg);
        }
    }

    public static void v(String tag, String msg) {
        if (sLogEnable) {
            Log.v(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (sLogEnable) {
            Log.e(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (sLogEnable) {
            Log.w(tag, msg);
        }
    }

    public static void printStackByException() {
        if (sLogEnable) {
            String str = null;
            try {
                str.length();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

}
