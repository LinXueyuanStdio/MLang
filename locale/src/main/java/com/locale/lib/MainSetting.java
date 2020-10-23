package com.locale.lib;

import android.content.SharedPreferences;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/10/23
 * @description null
 * @usage null
 */
public class MainSetting {
    public static SharedPreferences getGlobalMainSettings() {
        return null;
    }
    public static int distanceSystemType = 0;
    public static boolean USE_CLOUD_STRINGS = false;
    public static long getTimeFromServer() {
        return System.currentTimeMillis();
    }
}
