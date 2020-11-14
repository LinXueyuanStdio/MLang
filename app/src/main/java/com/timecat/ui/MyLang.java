package com.timecat.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import com.timecat.component.locale.LangAction;
import com.timecat.component.locale.MLang;
import com.timecat.component.locale.Util;

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

    private static File filesDir = getFilesDirFixed(getContext());

    private static LangAction action = new MyLangAction();

    public static void init(@NonNull Context applicationContext) {
        try {
            getInstance(applicationContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveLanguageKeyInLocal(String language) {
        SharedPreferences preferences = getContext().getSharedPreferences("language_locale", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("language", language);
        editor.apply();
    }

    @Nullable
    public static String loadLanguageKeyInLocal() {
        SharedPreferences preferences = getContext().getSharedPreferences("language_locale", Context.MODE_PRIVATE);
        return preferences.getString("language", null);
    }

    public static void onConfigurationChanged(@NonNull Configuration newConfig) {
        getInstance().onDeviceConfigurationChange(getContext(), newConfig);
    }

    public static Context getContext() {
        return MyApplication.applicationContext;
    }

    public static MLang getInstance() {
        return getInstance(getContext());
    }

    public static MLang getInstance(Context context) {
        return MLang.getInstance(context, filesDir, action);
    }

    public static File getFilesDirFixed(Context context) {
        return Util.getFilesDirFixed(context, "/data/data/com.locale.ui/files");
    }

    public static String getSystemLocaleStringIso639() {
        return getInstance().getSystemLocaleStringIso639(getContext());
    }

    public static String getLocaleStringIso639() {
        return getInstance().getLocaleStringIso639(getContext());
    }

    public static String getLocaleAlias(String code) {
        return MLang.getLocaleAlias(code);
    }

    public static String getCurrentLanguageName() {
        return getInstance().getCurrentLanguageName(getContext());
    }

    public static String getServerString(String key) {
        return getInstance().getServerString(getContext(), key);
    }

    public static String getString(String key, int res) {
        return getInstance().getString(getContext(), key, res);
    }

    public static String getString(String key) {
        return getInstance().getString(getContext(), key);
    }

    public static String getPluralString(String key, int plural) {
        return getInstance().getPluralString(getContext(), key, plural);
    }

    public static String formatPluralString(String key, int plural) {
        return getInstance().formatPluralString(getContext(), key, plural);
    }

    public static String formatPluralStringComma(String key, int plural) {
        return getInstance().formatPluralStringComma(getContext(), key, plural);
    }

    public static String formatString(String key, int res, Object... args) {
        return getInstance().formatString(getContext(), key, res, args);
    }

    public static String formatTTLString(int ttl) {
        return getInstance().formatTTLString(getContext(), ttl);
    }

    public static String formatStringSimple(String string, Object... args) {
        return getInstance().formatStringSimple(getContext(), string, args);
    }

    public static String formatCallDuration(int duration) {
        return getInstance().formatCallDuration(getContext(), duration);
    }

    public static String formatDateChat(long date) {
        return getInstance().formatDateChat(getContext(), date);
    }

    public static String formatDateChat(long date, boolean checkYear) {
        return getInstance().formatDateChat(getContext(), date, checkYear);
    }

    public static String formatDate(long date) {
        return getInstance().formatDate(getContext(), date);
    }

    public static String formatDateAudio(long date, boolean shortFormat) {
        return getInstance().formatDateAudio(getContext(), date, shortFormat);
    }

    public static String formatDateCallLog(long date) {
        return getInstance().formatDateCallLog(getContext(), date);
    }

    public static String formatLocationUpdateDate(long date, long timeFromServer) {
        return getInstance().formatLocationUpdateDate(getContext(), date, timeFromServer);
    }

    public static String formatLocationLeftTime(int time) {
        return MLang.formatLocationLeftTime(time);
    }

    public static String formatDateOnline(long date) {
        return getInstance().formatDateOnline(getContext(), date);
    }

    public static boolean isRTLCharacter(char ch) {
        return MLang.isRTLCharacter(ch);
    }

    public static String formatSectionDate(long date) {
        return getInstance().formatSectionDate(getContext(), date);
    }

    public static String formatDateForBan(long date) {
        return getInstance().formatDateForBan(getContext(), date);
    }

    public static String stringForMessageListDate(long date) {
        return getInstance().stringForMessageListDate(getContext(), date);
    }

    public static String formatShortNumber(int number, int[] rounded) {
        return MLang.formatShortNumber(number, rounded);
    }

    public static void resetImperialSystemType() {
        MLang.resetImperialSystemType();
    }

    public static String formatDistance(float distance) {
        return getInstance().formatDistance(getContext(), distance);
    }
}
