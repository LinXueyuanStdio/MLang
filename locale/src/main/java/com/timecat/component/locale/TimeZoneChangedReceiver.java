package com.timecat.component.locale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.TimeZone;

/**
 * 接收时区改变
 * 广播接收器
 */
public class TimeZoneChangedReceiver extends BroadcastReceiver {
    private MLang mLang;

    public TimeZoneChangedReceiver(MLang mLang) {
        this.mLang = mLang;
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!TimeZone.getDefault().equals(mLang.formatterDayMonth.getTimeZone())) {
            mLang.recreateFormatters(context);
        }
//        mLang.runOnUIThread(new Runnable() {
//            @Override
//            public void run() {
//                if (!TimeZone.getDefault().equals(mLang.formatterDayMonth.getTimeZone())) {
//                    mLang.recreateFormatters(context);
//                }
//            }
//        });
    }
}
