package com.locale.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import com.locale.lib.AbsLangAction;
import com.locale.lib.MLang;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/10/24
 * @description 隔离层
 * @usage 直接使用 MyLang.xxx()
 */
public class MyLang {
    public static void init(@NonNull Context applicationContext) {
        MLang.action = new AbsLangAction() {
            @Override
            public long getTimeFromServer() {
                return System.currentTimeMillis();
            }

            @Override
            public File getFilesDirFixed(Context context) {
                return MLang.getFilesDirFixed(context, "/data/data/com.locale.ui/files");
            }

            @Override
            public void runOnUIThread(Runnable runnable) {
                MyApplication.applicationHandler.post(runnable);
            }

            @Override
            public void saveLanguageKeyInLocal(String language) {
                SharedPreferences preferences = getContext().getSharedPreferences("language_locale", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("language", language);
                editor.apply();
            }

            @Nullable
            @Override
            public String loadLanguageKeyInLocal() {
                SharedPreferences preferences = getContext().getSharedPreferences("language_locale", Context.MODE_PRIVATE);
                return preferences.getString("language", null);
            }

            @Override
            public void langpack_getDifference(String lang_pack, String lang_code, int from_version, @NonNull GetDifferenceCallback callback) {

            }

            @Override
            public void langpack_getLanguages(@NonNull GetLanguagesCallback callback) {

            }

            @Override
            public void langpack_getLangPack(String lang_code, @NonNull GetLangPackCallback callback) {

            }
        };
        try {
            MLang.getInstance(applicationContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void onConfigurationChanged(@NonNull Context applicationContext, @NonNull Configuration newConfig) {
        MLang.getInstance(applicationContext).onDeviceConfigurationChange(applicationContext, newConfig);
    }

    public static Context getContext() {
        return MyApplication.applicationContext;
    }

    public static MLang getInstance() {
        return MLang.getInstance(getContext());
    }

    public static String getSystemLocaleStringIso639() {
        return MLang.getSystemLocaleStringIso639(getContext());
    }

    public static String getLocaleStringIso639() {
        return MLang.getLocaleStringIso639(getContext());
    }

    public static String getLocaleAlias(String code) {
        return MLang.getLocaleAlias(code);
    }

    public static String getCurrentLanguageName() {
        return MLang.getCurrentLanguageName(getContext());
    }

    public static String getServerString(String key) {
        return MLang.getServerString(getContext(), key);
    }

    public static String getString(String key, int res) {
        return MLang.getString(getContext(), key, res);
    }

    public static String getString(String key) {
        return MLang.getString(getContext(), key);
    }

    public static String getPluralString(String key, int plural) {
        return MLang.getPluralString(getContext(), key, plural);
    }

    public static String formatPluralString(String key, int plural) {
        return MLang.formatPluralString(getContext(), key, plural);
    }

    public static String formatPluralStringComma(String key, int plural) {
        return MLang.formatPluralStringComma(getContext(), key, plural);
    }

    public static String formatString(String key, int res, Object... args) {
        return MLang.formatString(getContext(), key, res, args);
    }

    public static String formatTTLString(int ttl) {
        return MLang.formatTTLString(getContext(), ttl);
    }

    public static String formatStringSimple(String string, Object... args) {
        return MLang.formatStringSimple(getContext(), string, args);
    }

    public static String formatCallDuration(int duration) {
        return MLang.formatCallDuration(getContext(), duration);
    }

    public static String formatDateChat(long date) {
        return MLang.formatDateChat(getContext(), date);
    }

    public static String formatDateChat(long date, boolean checkYear) {
        return MLang.formatDateChat(getContext(), date, checkYear);
    }

    public static String formatDate(long date) {
        return MLang.formatDate(getContext(), date);
    }

    public static String formatDateAudio(long date, boolean shortFormat) {
        return MLang.formatDateAudio(getContext(), date, shortFormat);
    }

    public static String formatDateCallLog(long date) {
        return MLang.formatDateCallLog(getContext(), date);
    }

    public static String formatLocationUpdateDate(long date) {
        return MLang.formatLocationUpdateDate(getContext(), date);
    }

    public static String formatLocationLeftTime(int time) {
        return MLang.formatLocationLeftTime(time);
    }

    public static String formatDateOnline(long date) {
        return MLang.formatDateOnline(getContext(), date);
    }

    public static boolean isRTLCharacter(char ch) {
        return MLang.isRTLCharacter(ch);
    }

    public static String formatSectionDate(long date) {
        return MLang.formatSectionDate(getContext(), date);
    }

    public static String formatDateForBan(long date) {
        return MLang.formatDateForBan(getContext(), date);
    }

    public static String stringForMessageListDate(long date) {
        return MLang.stringForMessageListDate(getContext(), date);
    }

    public static String formatShortNumber(int number, int[] rounded) {
        return MLang.formatShortNumber(number, rounded);
    }

    public static void resetImperialSystemType() {
        MLang.resetImperialSystemType();
    }

    public static String formatDistance(float distance) {
        return MLang.formatDistance(getContext(), distance);
    }
}
