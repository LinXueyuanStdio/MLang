package com.timecat.component.locale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.TimeZone;

import androidx.annotation.NonNull;

/**
 * 接收时区改变
 * 广播接收器
 */
public class TimeZoneChangedReceiver extends BroadcastReceiver {
    private MLang mLang;

    public TimeZoneChangedReceiver(@NonNull MLang mLang) {
        this.mLang = mLang;
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        mLang.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (!mLang.formatterDayMonth.getTimeZone().equals(TimeZone.getDefault())) {
                    mLang.recreateFormatters(context);
                }
            }
        });
    }
}
