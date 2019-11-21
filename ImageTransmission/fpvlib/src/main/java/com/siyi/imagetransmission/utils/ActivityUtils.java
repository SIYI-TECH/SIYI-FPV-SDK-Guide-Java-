package com.siyi.imagetransmission.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;

/**
 * author: shymanzhu
 * data: on 2019/4/18
 */
public class ActivityUtils {

    /**
     * Launch activity
     * @param context
     * @param intent
     */
    public static void startActivity(Context context, Intent intent) {
        try {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

}
