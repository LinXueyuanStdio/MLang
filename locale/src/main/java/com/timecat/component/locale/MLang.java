package com.timecat.component.locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Xml;

import com.timecat.component.locale.R;
import com.timecat.component.locale.model.LangPackDifference;
import com.timecat.component.locale.model.LangPackLanguage;
import com.timecat.component.locale.model.LangPackString;
import com.timecat.component.locale.time.FastDateFormat;

import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/10/23
 * @description null
 * @usage null
 * 本地化，多语言
 *
 * 如何使用：
 * 1. 一个新语言的增加和删除
 *
 * 2. 使用
 *   MLang.getString("PaymentShippingName", R.string.PaymentShippingName)
 * 3. 监听系统语言的变化
 * class MyApplication extends Application {
 *     @Override
 *     public void onConfigurationChanged(Configuration newConfig) {
 *         super.onConfigurationChanged(newConfig);
 *         try {
 *             LocaleController.getInstance().onDeviceConfigurationChange(newConfig);
 *             AndroidUtilities.checkDisplaySize(applicationContext, newConfig);
 *         } catch (Exception e) {
 *             e.printStackTrace();
 *         }
 *     }
 *   }
 */
public class MLang {

    static final int QUANTITY_OTHER = 0x0000;
    static final int QUANTITY_ZERO = 0x0001;
    static final int QUANTITY_ONE = 0x0002;
    static final int QUANTITY_TWO = 0x0004;
    static final int QUANTITY_FEW = 0x0008;
    static final int QUANTITY_MANY = 0x0010;

    public static boolean isRTL = false;
    public static int nameDisplayOrder = 1;
    public static boolean is24HourFormat = false;
    public FastDateFormat formatterDay;
    public FastDateFormat formatterWeek;
    public FastDateFormat formatterDayMonth;
    public FastDateFormat formatterYear;
    public FastDateFormat formatterYearMax;
    public FastDateFormat formatterStats;
    public FastDateFormat formatterBannedUntil;
    public FastDateFormat formatterBannedUntilThisYear;
    public FastDateFormat chatDate;
    public FastDateFormat chatFullDate;
    public FastDateFormat formatterScheduleDay;
    public FastDateFormat formatterScheduleYear;
    public FastDateFormat[] formatterScheduleSend = new FastDateFormat[6];

    private HashMap<String, PluralRules> allRules = new HashMap<>();

    private Locale currentLocale;
    private Locale systemDefaultLocale;
    private PluralRules currentPluralRules;
    private LocaleInfo currentLocaleInfo;
    private HashMap<String, String> localeValues = new HashMap<>();
    private String languageOverride;
    private boolean changingConfiguration = false;
    private boolean reloadLastFile;

    private String currentSystemLocale;

    private HashMap<String, String> currencyValues;
    private HashMap<String, String> translitChars;
    private HashMap<String, String> ruTranslitChars;

    /**
     * 时区
     */
    private class TimeZoneChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            runOnUIThread(() -> {
                if (!formatterDayMonth.getTimeZone().equals(TimeZone.getDefault())) {
                    MLang.getInstance(context).recreateFormatters(context);
                }
            });
        }
    }

    /**
     * 需要控制语言版本所需的所有配置项都在这里了
     */
    public static class LocaleInfo {

        public String name;
        public String nameEnglish;
        public String shortName;
        public String pathToFile;
        public String baseLangCode;
        public String pluralLangCode;
        public boolean isRtl;
        public int version;
        public int baseVersion;
        public boolean builtIn;//是否是内置支持的语言
        public int serverIndex;

        public String getSaveString() {
            String langCode = baseLangCode == null ? "" : baseLangCode;
            String pluralCode = TextUtils.isEmpty(pluralLangCode) ? shortName : pluralLangCode;
            return name + "|" + nameEnglish + "|" + shortName + "|" + pathToFile + "|" + version + "|" + langCode + "|" + pluralLangCode + "|" + (isRtl ? 1 : 0) + "|" + baseVersion + "|" + serverIndex;
        }

        public static LocaleInfo createWithString(String string) {
            if (string == null || string.length() == 0) {
                return null;
            }
            String[] args = string.split("\\|");
            LocaleInfo localeInfo = null;
            if (args.length >= 4) {
                localeInfo = new LocaleInfo();
                localeInfo.name = args[0];
                localeInfo.nameEnglish = args[1];
                localeInfo.shortName = args[2].toLowerCase();
                localeInfo.pathToFile = args[3];
                if (args.length >= 5) {
                    localeInfo.version = parseInt(args[4]);
                }
                localeInfo.baseLangCode = args.length >= 6 ? args[5] : "";
                localeInfo.pluralLangCode = args.length >= 7 ? args[6] : localeInfo.shortName;
                if (args.length >= 8) {
                    localeInfo.isRtl = parseInt(args[7]) == 1;
                }
                if (args.length >= 9) {
                    localeInfo.baseVersion = parseInt(args[8]);
                }
                if (args.length >= 10) {
                    localeInfo.serverIndex = parseInt(args[9]);
                } else {
                    localeInfo.serverIndex = Integer.MAX_VALUE;
                }
                if (!TextUtils.isEmpty(localeInfo.baseLangCode)) {
                    localeInfo.baseLangCode = localeInfo.baseLangCode.replace("-", "_");
                }
            }
            return localeInfo;
        }

        public File getPathToFile(Context context) {
            if (isRemote()) {
                return new File(getFilesDirFixed(context), "remote_" + shortName + ".xml");
            } else if (isUnofficial()) {
                return new File(getFilesDirFixed(context), "unofficial_" + shortName + ".xml");
            }
            return !TextUtils.isEmpty(pathToFile) ? new File(pathToFile) : null;
        }

        public File getPathToBaseFile(Context context) {
            if (isUnofficial()) {
                return new File(getFilesDirFixed(context), "unofficial_base_" + shortName + ".xml");
            }
            return null;
        }

        public String getKey() {
            if (pathToFile != null && !isRemote() && !isUnofficial()) {
                return "local_" + shortName;
            } else if (isUnofficial()) {
                return "unofficial_" + shortName;
            }
            return shortName;
        }

        public boolean hasBaseLang() {
            return isUnofficial() && !TextUtils.isEmpty(baseLangCode) && !baseLangCode.equals(shortName);
        }

        public boolean isRemote() {
            return "remote".equals(pathToFile);
        }

        public boolean isUnofficial() {
            return "unofficial".equals(pathToFile);
        }

        public boolean isLocal() {
            return !TextUtils.isEmpty(pathToFile) && !isRemote() && !isUnofficial();
        }

        public boolean isBuiltIn() {
            return builtIn;
        }

        public String getLangCode() {
            return shortName.replace("_", "-");
        }

        public String getBaseLangCode() {
            return baseLangCode == null ? "" : baseLangCode.replace("_", "-");
        }
    }

    private boolean loadingRemoteLanguages;

    /**
     * 当前已下载到本地的可用的语言
     */
    public ArrayList<LocaleInfo> languages = new ArrayList<>();
    /**
     * 非官方语言
     */
    public ArrayList<LocaleInfo> unofficialLanguages = new ArrayList<>();
    /**
     * 远程语言支持
     */
    public ArrayList<LocaleInfo> remoteLanguages = new ArrayList<>();
    public HashMap<String, LocaleInfo> remoteLanguagesDict = new HashMap<>();
    public HashMap<String, LocaleInfo> languagesDict = new HashMap<>();

    /**
     * 除了内置的语言之外的语言，即您自己安装的语言
     */
    private ArrayList<LocaleInfo> otherLanguages = new ArrayList<>();
    public static AbsLangAction action = null;

    //双重校验锁的单例模式模式
    private static volatile MLang Instance = null;

    /**
     * MLang 在内部不会持有 context
     * 这里需要 context 是要根据 context 找资源做初始化
     * @param context 上下文
     * @return MLang
     */
    public static MLang getInstance(Context context) {
        MLang localInstance = Instance;
        if (localInstance == null) {
            synchronized (MLang.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new MLang(context);
                }
            }
        }
        return localInstance;
    }

    /**
     * MLang 在内部不会持有 context
     * 这里需要 context 是要根据 context 找资源做初始化
     * @param context 上下文
     */
    public MLang(Context context) {
        addRules(new String[]{"bem", "brx", "da", "de", "el", "en", "eo", "es", "et", "fi", "fo", "gl", "he", "iw", "it", "nb",
                              "nl", "nn", "no", "sv", "af", "bg", "bn", "ca", "eu", "fur", "fy", "gu", "ha", "is", "ku",
                              "lb", "ml", "mr", "nah", "ne", "om", "or", "pa", "pap", "ps", "so", "sq", "sw", "ta", "te",
                              "tk", "ur", "zu", "mn", "gsw", "chr", "rm", "pt", "an", "ast"}, new PluralRules_One());
        addRules(new String[]{"cs", "sk"}, new PluralRules_Czech());
        addRules(new String[]{"ff", "fr", "kab"}, new PluralRules_French());
        addRules(new String[]{"hr", "ru", "sr", "uk", "be", "bs", "sh"}, new PluralRules_Balkan());
        addRules(new String[]{"lv"}, new PluralRules_Latvian());
        addRules(new String[]{"lt"}, new PluralRules_Lithuanian());
        addRules(new String[]{"pl"}, new PluralRules_Polish());
        addRules(new String[]{"ro", "mo"}, new PluralRules_Romanian());
        addRules(new String[]{"sl"}, new PluralRules_Slovenian());
        addRules(new String[]{"ar"}, new PluralRules_Arabic());
        addRules(new String[]{"mk"}, new PluralRules_Macedonian());
        addRules(new String[]{"cy"}, new PluralRules_Welsh());
        addRules(new String[]{"br"}, new PluralRules_Breton());
        addRules(new String[]{"lag"}, new PluralRules_Langi());
        addRules(new String[]{"shi"}, new PluralRules_Tachelhit());
        addRules(new String[]{"mt"}, new PluralRules_Maltese());
        addRules(new String[]{"ga", "se", "sma", "smi", "smj", "smn", "sms"}, new PluralRules_Two());
        addRules(new String[]{"ak", "am", "bh", "fil", "tl", "guw", "hi", "ln", "mg", "nso", "ti", "wa"}, new PluralRules_Zero());
        addRules(new String[]{"az", "bm", "fa", "ig", "hu", "ja", "kde", "kea", "ko", "my", "ses", "sg", "to",
                              "tr", "vi", "wo", "yo", "zh", "bo", "dz", "id", "jv", "jw", "ka", "km", "kn", "ms", "th", "in"}, new PluralRules_None());

        LocaleInfo localeInfo = new LocaleInfo();
        localeInfo.name = "English";
        localeInfo.nameEnglish = "English";
        localeInfo.shortName = localeInfo.pluralLangCode = "en";
        localeInfo.pathToFile = null;
        localeInfo.builtIn = true;
        languages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        localeInfo = new LocaleInfo();
        localeInfo.name = "中文简体";
        localeInfo.nameEnglish = "Chinese";
        localeInfo.shortName = localeInfo.pluralLangCode = "zh";
        localeInfo.pathToFile = null;
        localeInfo.builtIn = true;
        languages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        loadOtherLanguages(context);
        if (remoteLanguages.isEmpty()) {
            runOnUIThread(() -> loadRemoteLanguages(context));
        }

        for (int a = 0; a < otherLanguages.size(); a++) {
            LocaleInfo locale = otherLanguages.get(a);
            languages.add(locale);
            languagesDict.put(locale.getKey(), locale);
        }

        for (int a = 0; a < remoteLanguages.size(); a++) {
            LocaleInfo locale = remoteLanguages.get(a);
            LocaleInfo existingLocale = getLanguageFromDict(locale.getKey());
            if (existingLocale != null) {
                existingLocale.pathToFile = locale.pathToFile;
                existingLocale.version = locale.version;
                existingLocale.baseVersion = locale.baseVersion;
                existingLocale.serverIndex = locale.serverIndex;
                remoteLanguages.set(a, existingLocale);
            } else {
                languages.add(locale);
                languagesDict.put(locale.getKey(), locale);
            }
        }

        for (int a = 0; a < unofficialLanguages.size(); a++) {
            LocaleInfo locale = unofficialLanguages.get(a);
            LocaleInfo existingLocale = getLanguageFromDict(locale.getKey());
            if (existingLocale != null) {
                existingLocale.pathToFile = locale.pathToFile;
                existingLocale.version = locale.version;
                existingLocale.baseVersion = locale.baseVersion;
                existingLocale.serverIndex = locale.serverIndex;
                unofficialLanguages.set(a, existingLocale);
            } else {
                languagesDict.put(locale.getKey(), locale);
            }
        }

        systemDefaultLocale = Locale.getDefault();
        is24HourFormat = DateFormat.is24HourFormat(context);
        LocaleInfo currentInfo = null;
        boolean override = false;

        try {
            String lang = null;
            if (action != null) {
                lang = action.loadLanguageKeyInLocal();
            }
            if (lang != null) {
                currentInfo = getLanguageFromDict(lang);
                if (currentInfo != null) {
                    override = true;
                }
            }

            if (currentInfo == null && systemDefaultLocale.getLanguage() != null) {
                currentInfo = getLanguageFromDict(systemDefaultLocale.getLanguage());
            }
            if (currentInfo == null) {
                currentInfo = getLanguageFromDict(getLocaleString(systemDefaultLocale));
                if (currentInfo == null) {
                    currentInfo = getLanguageFromDict("en");
                }
            }

            applyLanguage(context, currentInfo, override, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            IntentFilter timezoneFilter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            context.registerReceiver(new TimeZoneChangedReceiver(), timezoneFilter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        runOnUIThread(() -> currentSystemLocale = getSystemLocaleStringIso639(context));
    }

    public void setAction(AbsLangAction action) {
        this.action = action;
        action = action;
    }

    public LocaleInfo getLanguageFromDict(String key) {
        if (key == null) {
            return null;
        }
        return languagesDict.get(key.toLowerCase().replace("-", "_"));
    }

    private void addRules(String[] languages, PluralRules rules) {
        for (String language : languages) {
            allRules.put(language, rules);
        }
    }

    private String stringForQuantity(int quantity) {
        switch (quantity) {
            case QUANTITY_ZERO:
                return "zero";
            case QUANTITY_ONE:
                return "one";
            case QUANTITY_TWO:
                return "two";
            case QUANTITY_FEW:
                return "few";
            case QUANTITY_MANY:
                return "many";
            default:
                return "other";
        }
    }

    public Locale getSystemDefaultLocale() {
        return systemDefaultLocale;
    }

    public boolean isCurrentLocalLocale() {
        return currentLocaleInfo.isLocal();
    }

    public void reloadCurrentRemoteLocale(Context context, String langCode, boolean force) {
        if (langCode != null) {
            langCode = langCode.replace("-", "_");
        }
        if (langCode == null || currentLocaleInfo != null && (langCode.equals(currentLocaleInfo.shortName) || langCode.equals(currentLocaleInfo.baseLangCode))) {
            applyRemoteLanguage(context, currentLocaleInfo, langCode, force);
        }
    }

    public void checkUpdateForCurrentRemoteLocale(Context context, int version, int baseVersion) {
        if (currentLocaleInfo == null || currentLocaleInfo != null && !currentLocaleInfo.isRemote() && !currentLocaleInfo.isUnofficial()) {
            return;
        }
        if (currentLocaleInfo.hasBaseLang()) {
            if (currentLocaleInfo.baseVersion < baseVersion) {
                applyRemoteLanguage(context, currentLocaleInfo, currentLocaleInfo.baseLangCode, false);
            }
        }
        if (currentLocaleInfo.version < version) {
            applyRemoteLanguage(context, currentLocaleInfo, currentLocaleInfo.shortName, false);
        }
    }

    private String getLocaleString(Locale locale) {
        if (locale == null) {
            return "en";
        }
        String languageCode = locale.getLanguage();
        String countryCode = locale.getCountry();
        String variantCode = locale.getVariant();
        if (languageCode.length() == 0 && countryCode.length() == 0) {
            return "en";
        }
        StringBuilder result = new StringBuilder(11);
        result.append(languageCode);
        if (countryCode.length() > 0 || variantCode.length() > 0) {
            result.append('_');
        }
        result.append(countryCode);
        if (variantCode.length() > 0) {
            result.append('_');
        }
        result.append(variantCode);
        return result.toString();
    }

    public static String getSystemLocaleStringIso639(Context context) {
        Locale locale = getInstance(context).getSystemDefaultLocale();
        if (locale == null) {
            return "en";
        }
        String languageCode = locale.getLanguage();
        String countryCode = locale.getCountry();
        String variantCode = locale.getVariant();
        if (languageCode.length() == 0 && countryCode.length() == 0) {
            return "en";
        }
        StringBuilder result = new StringBuilder(11);
        result.append(languageCode);
        if (countryCode.length() > 0 || variantCode.length() > 0) {
            result.append('-');
        }
        result.append(countryCode);
        if (variantCode.length() > 0) {
            result.append('_');
        }
        result.append(variantCode);
        return result.toString();
    }

    public static String getLocaleStringIso639(Context context) {
        LocaleInfo info = getInstance(context).currentLocaleInfo;
        if (info != null) {
            return info.getLangCode();
        }
        Locale locale = getInstance(context).currentLocale;
        if (locale == null) {
            return "en";
        }
        String languageCode = locale.getLanguage();
        String countryCode = locale.getCountry();
        String variantCode = locale.getVariant();
        if (languageCode.length() == 0 && countryCode.length() == 0) {
            return "en";
        }
        StringBuilder result = new StringBuilder(11);
        result.append(languageCode);
        if (countryCode.length() > 0 || variantCode.length() > 0) {
            result.append('-');
        }
        result.append(countryCode);
        if (variantCode.length() > 0) {
            result.append('_');
        }
        result.append(variantCode);
        return result.toString();
    }

    public static String getLocaleAlias(String code) {
        if (code == null) {
            return null;
        }
        switch (code) {
            case "in":
                return "id";
            case "iw":
                return "he";
            case "jw":
                return "jv";
            case "no":
                return "nb";
            case "tl":
                return "fil";
            case "ji":
                return "yi";
            case "id":
                return "in";
            case "he":
                return "iw";
            case "jv":
                return "jw";
            case "nb":
                return "no";
            case "fil":
                return "tl";
            case "yi":
                return "ji";
        }

        return null;
    }

    /**
     * 解析翻译文件并应用
     * @param file 语言文件
     * @return 是否应用成功
     */
    public boolean applyLanguageFile(Context context, File file) {
        try {
            HashMap<String, String> stringMap = getLocaleFileStrings(file);

            //翻译文件的版本控制
            String languageName = stringMap.get("LanguageName");
            String languageNameInEnglish = stringMap.get("LanguageNameInEnglish");
            String languageCode = stringMap.get("LanguageCode");

            if (languageName != null && languageName.length() > 0 &&
                    languageNameInEnglish != null && languageNameInEnglish.length() > 0 &&
                    languageCode != null && languageCode.length() > 0) {

                if (languageName.contains("&") || languageName.contains("|")) {
                    return false;
                }
                if (languageNameInEnglish.contains("&") || languageNameInEnglish.contains("|")) {
                    return false;
                }
                if (languageCode.contains("&") || languageCode.contains("|") || languageCode.contains("/") || languageCode.contains("\\")) {
                    return false;
                }

                //将当前需要应用的翻译文件复制到内部目录
                File finalFile = new File(getFilesDirFixed(context), languageCode + ".xml");
                if (!copyFile(file, finalFile)) {
                    return false;
                }

                String key = "local_" + languageCode.toLowerCase();
                LocaleInfo localeInfo = getLanguageFromDict(key);
                if (localeInfo == null) {
                    localeInfo = new LocaleInfo();
                    localeInfo.name = languageName;
                    localeInfo.nameEnglish = languageNameInEnglish;
                    localeInfo.shortName = languageCode.toLowerCase();
                    localeInfo.pluralLangCode = localeInfo.shortName;

                    localeInfo.pathToFile = finalFile.getAbsolutePath();
                    languages.add(localeInfo);
                    languagesDict.put(localeInfo.getKey(), localeInfo);
                    otherLanguages.add(localeInfo);

                    saveOtherLanguages(context);
                }
                localeValues = stringMap;
                applyLanguage(context, localeInfo, true, false, true, false);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void saveOtherLanguages(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("langconfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        StringBuilder stringBuilder = new StringBuilder();
        for (int a = 0; a < otherLanguages.size(); a++) {
            LocaleInfo localeInfo = otherLanguages.get(a);
            String loc = localeInfo.getSaveString();
            if (loc != null) {
                if (stringBuilder.length() != 0) {
                    stringBuilder.append("&");
                }
                stringBuilder.append(loc);
            }
        }
        editor.putString("locales", stringBuilder.toString());
        stringBuilder.setLength(0);
        for (int a = 0; a < remoteLanguages.size(); a++) {
            LocaleInfo localeInfo = remoteLanguages.get(a);
            String loc = localeInfo.getSaveString();
            if (loc != null) {
                if (stringBuilder.length() != 0) {
                    stringBuilder.append("&");
                }
                stringBuilder.append(loc);
            }
        }
        editor.putString("remote", stringBuilder.toString());
        stringBuilder.setLength(0);
        for (int a = 0; a < unofficialLanguages.size(); a++) {
            LocaleInfo localeInfo = unofficialLanguages.get(a);
            String loc = localeInfo.getSaveString();
            if (loc != null) {
                if (stringBuilder.length() != 0) {
                    stringBuilder.append("&");
                }
                stringBuilder.append(loc);
            }
        }
        editor.putString("unofficial", stringBuilder.toString());
        editor.apply();
    }

    public boolean deleteLanguage(Context context, LocaleInfo localeInfo) {
        if (localeInfo.pathToFile == null || localeInfo.isRemote() && localeInfo.serverIndex != Integer.MAX_VALUE) {
            return false;
        }
        if (currentLocaleInfo == localeInfo) {
            LocaleInfo info = null;
            if (systemDefaultLocale.getLanguage() != null) {
                info = getLanguageFromDict(systemDefaultLocale.getLanguage());
            }
            if (info == null) {
                info = getLanguageFromDict(getLocaleString(systemDefaultLocale));
            }
            if (info == null) {
                info = getLanguageFromDict("en");
            }
            applyLanguage(context, info, true, false);
        }

        unofficialLanguages.remove(localeInfo);
        remoteLanguages.remove(localeInfo);
        remoteLanguagesDict.remove(localeInfo.getKey());
        otherLanguages.remove(localeInfo);
        languages.remove(localeInfo);
        languagesDict.remove(localeInfo.getKey());
        File file = new File(localeInfo.pathToFile);
        file.delete();
        saveOtherLanguages(context);
        return true;
    }

    private void loadOtherLanguages(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("langconfig", Activity.MODE_PRIVATE);
        String locales = preferences.getString("locales", null);
        if (!TextUtils.isEmpty(locales)) {
            String[] localesArr = locales.split("&");
            for (String locale : localesArr) {
                LocaleInfo localeInfo = LocaleInfo.createWithString(locale);
                if (localeInfo != null) {
                    otherLanguages.add(localeInfo);
                }
            }
        }
        locales = preferences.getString("remote", null);
        if (!TextUtils.isEmpty(locales)) {
            String[] localesArr = locales.split("&");
            for (String locale : localesArr) {
                LocaleInfo localeInfo = LocaleInfo.createWithString(locale);
                localeInfo.shortName = localeInfo.shortName.replace("-", "_");
                if (remoteLanguagesDict.containsKey(localeInfo.getKey())) {
                    continue;
                }
                if (localeInfo != null) {
                    remoteLanguages.add(localeInfo);
                    remoteLanguagesDict.put(localeInfo.getKey(), localeInfo);
                }
            }
        }
        locales = preferences.getString("unofficial", null);
        if (!TextUtils.isEmpty(locales)) {
            String[] localesArr = locales.split("&");
            for (String locale : localesArr) {
                LocaleInfo localeInfo = LocaleInfo.createWithString(locale);
                localeInfo.shortName = localeInfo.shortName.replace("-", "_");
                if (localeInfo != null) {
                    unofficialLanguages.add(localeInfo);
                }
            }
        }
    }

    private HashMap<String, String> getLocaleFileStrings(File file) {
        return getLocaleFileStrings(file, false);
    }

    /**
     * 翻译文件是一个xml格式的文件
     * 可以知道Telegram自定义了一套翻译文件的规范
     * 使用了Android系统自带的xml解释器来解析
     * @param file xml格式
     * @param preserveEscapes
     * @return stringMap.put(attrName, value);
     */
    private HashMap<String, String> getLocaleFileStrings(File file, boolean preserveEscapes) {
        FileInputStream stream = null;
        reloadLastFile = false;
        try {
            if (!file.exists()) {
                return new HashMap<>();
            }
            HashMap<String, String> stringMap = new HashMap<>();
            XmlPullParser parser = Xml.newPullParser();
            //copyFile(file, new File(applicationContext.getExternalFilesDir(null), "locale10.xml"));
            stream = new FileInputStream(file);
            parser.setInput(stream, "UTF-8");
            int eventType = parser.getEventType();
            String name = null;
            String value = null;
            String attrName = null;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    name = parser.getName();
                    int c = parser.getAttributeCount();
                    if (c > 0) {
                        attrName = parser.getAttributeValue(0);
                    }
                } else if (eventType == XmlPullParser.TEXT) {
                    if (attrName != null) {
                        value = parser.getText();
                        if (value != null) {
                            value = value.trim();
                            if (preserveEscapes) {
                                value = value.replace("<", "&lt;").replace(">", "&gt;").replace("'", "\\'").replace("& ", "&amp; ");
                            } else {
                                value = value.replace("\\n", "\n");
                                value = value.replace("\\", "");
                                String old = value;
                                value = value.replace("&lt;", "<");
                                if (!reloadLastFile && !value.equals(old)) {
                                    reloadLastFile = true;
                                }
                            }
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    value = null;
                    attrName = null;
                    name = null;
                }
                if (name != null && name.equals("string") && value != null && attrName != null && value.length() != 0 && attrName.length() != 0) {
                    stringMap.put(attrName, value);
                    name = null;
                    value = null;
                    attrName = null;
                }
                eventType = parser.next();
            }
            return stringMap;
        } catch (Exception e) {
            e.printStackTrace();
            reloadLastFile = true;
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new HashMap<>();
    }

    public void applyLanguage(Context context, LocaleInfo localeInfo, boolean override, boolean init) {
        applyLanguage(context, localeInfo, override, init, false, false);
    }

    public void applyLanguage(Context context, final LocaleInfo localeInfo, boolean override, boolean init, boolean fromFile, boolean force) {
        if (localeInfo == null) {
            return;
        }
        boolean hasBase = localeInfo.hasBaseLang();
        File pathToFile = localeInfo.getPathToFile(context);
        File pathToBaseFile = localeInfo.getPathToBaseFile(context);
        String shortName = localeInfo.shortName;
        if (!init) {
            //            ConnectionsManager.setLangCode(localeInfo.getLangCode());
        }
        LocaleInfo existingInfo = getLanguageFromDict(localeInfo.getKey());
        if (existingInfo == null) {
            if (localeInfo.isRemote()) {
                remoteLanguages.add(localeInfo);
                remoteLanguagesDict.put(localeInfo.getKey(), localeInfo);
                languages.add(localeInfo);
                languagesDict.put(localeInfo.getKey(), localeInfo);
                saveOtherLanguages(context);
            } else if (localeInfo.isUnofficial()) {
                unofficialLanguages.add(localeInfo);
                languagesDict.put(localeInfo.getKey(), localeInfo);
                saveOtherLanguages(context);
            }
        }
        if ((localeInfo.isRemote() || localeInfo.isUnofficial()) && (force || !pathToFile.exists() || hasBase && !pathToBaseFile.exists())) {
            if (init) {
                runOnUIThread(() -> applyRemoteLanguage(context, localeInfo, null, true));
            } else {
                applyRemoteLanguage(context, localeInfo, null, true);
            }
        }
        try {
            Locale newLocale;
            String[] args;
            if (!TextUtils.isEmpty(localeInfo.pluralLangCode)) {
                args = localeInfo.pluralLangCode.split("_");
            } else if (!TextUtils.isEmpty(localeInfo.baseLangCode)) {
                args = localeInfo.baseLangCode.split("_");
            } else {
                args = localeInfo.shortName.split("_");
            }
            if (args.length == 1) {
                newLocale = new Locale(args[0]);
            } else {
                newLocale = new Locale(args[0], args[1]);
            }
            if (override) {
                languageOverride = localeInfo.shortName;
                if (action != null) {
                    action.saveLanguageKeyInLocal(localeInfo.getKey());
                }
            }
            if (pathToFile == null) {
                localeValues.clear();
            } else if (!fromFile) {
                localeValues = getLocaleFileStrings(hasBase ? localeInfo.getPathToBaseFile(context) : localeInfo.getPathToFile(context));
                if (hasBase) {
                    localeValues.putAll(getLocaleFileStrings(localeInfo.getPathToFile(context)));
                }
            }
            currentLocale = newLocale;
            currentLocaleInfo = localeInfo;

            if (currentLocaleInfo != null && !TextUtils.isEmpty(currentLocaleInfo.pluralLangCode)) {
                currentPluralRules = allRules.get(currentLocaleInfo.pluralLangCode);
            }
            if (currentPluralRules == null) {
                currentPluralRules = allRules.get(args[0]);
                if (currentPluralRules == null) {
                    currentPluralRules = allRules.get(currentLocale.getLanguage());
                    if (currentPluralRules == null) {
                        currentPluralRules = new PluralRules_None();
                    }
                }
            }
            changingConfiguration = true;
            Locale.setDefault(currentLocale);
            android.content.res.Configuration config = new android.content.res.Configuration();
            config.locale = currentLocale;
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
            changingConfiguration = false;
            if (reloadLastFile) {
                if (init) {
                    runOnUIThread(() -> reloadCurrentRemoteLocale(context, null, force));
                } else {
                    reloadCurrentRemoteLocale(context, null, force);
                }
                reloadLastFile = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            changingConfiguration = false;
        }
        recreateFormatters(context);
    }

    public LocaleInfo getCurrentLocaleInfo() {
        return currentLocaleInfo;
    }

    public static String getCurrentLanguageName(Context context) {
        LocaleInfo localeInfo = getInstance(context).currentLocaleInfo;
        return localeInfo == null || TextUtils.isEmpty(localeInfo.name) ? getString(context, "LanguageName", R.string.LanguageName) : localeInfo.name;
    }

    /**
     * 多语言动态化的核心方法
     * @param key
     * @param res
     * @return
     */
    private String getStringInternal(Context context, String key, int res) {
        //如果是云端的字符串，直接从 localeValues 拿，否则使用系统的 getString 拿
        String value = USE_CLOUD_STRINGS ? localeValues.get(key) : null;
        if (value == null) {
            try {
                value = context.getString(res);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (value == null) {
            value = "LOC_ERR:" + key;
        }
        return value;
    }

    public static String getServerString(Context context, String key) {
        String value = getInstance(context).localeValues.get(key);
        if (value == null) {
            int resourceId = context.getResources().getIdentifier(key, "string", context.getPackageName());
            if (resourceId != 0) {
                value = context.getString(resourceId);
            }
        }
        return value;
    }

    public static String getString(Context context, String key, int res) {
        return getInstance(context).getStringInternal(context, key, res);
    }

    public static String getString(Context context, String key) {
        if (TextUtils.isEmpty(key)) {
            return "LOC_ERR:" + key;
        }
        int resourceId = context.getResources().getIdentifier(key, "string", context.getPackageName());
        if (resourceId != 0) {
            return getString(context, key, resourceId);
        }
        return getServerString(context, key);
    }

    public static String getPluralString(Context context, String key, int plural) {
        if (key == null || key.length() == 0 || getInstance(context).currentPluralRules == null) {
            return "LOC_ERR:" + key;
        }
        String param = getInstance(context).stringForQuantity(getInstance(context).currentPluralRules.quantityForNumber(plural));
        param = key + "_" + param;
        int resourceId = context.getResources().getIdentifier(param, "string", context.getPackageName());
        return getString(context, param, resourceId);
    }

    public static String formatPluralString(Context context, String key, int plural) {
        if (key == null || key.length() == 0 || getInstance(context).currentPluralRules == null) {
            return "LOC_ERR:" + key;
        }
        String param = getInstance(context).stringForQuantity(getInstance(context).currentPluralRules.quantityForNumber(plural));
        param = key + "_" + param;
        int resourceId = context.getResources().getIdentifier(param, "string", context.getPackageName());
        return formatString(context, param, resourceId, plural);
    }

    public static String formatPluralStringComma(Context context, String key, int plural) {
        try {
            if (key == null || key.length() == 0 || getInstance(context).currentPluralRules == null) {
                return "LOC_ERR:" + key;
            }
            String param = getInstance(context).stringForQuantity(getInstance(context).currentPluralRules.quantityForNumber(plural));
            param = key + "_" + param;
            StringBuilder stringBuilder = new StringBuilder(String.format(Locale.US, "%d", plural));
            for (int a = stringBuilder.length() - 3; a > 0; a -= 3) {
                stringBuilder.insert(a, ',');
            }

            String value = USE_CLOUD_STRINGS ? getInstance(context).localeValues.get(param) : null;
            if (value == null) {
                int resourceId = context.getResources().getIdentifier(param, "string", context.getPackageName());
                value = context.getString(resourceId);
            }
            value = value.replace("%1$d", "%1$s");

            if (getInstance(context).currentLocale != null) {
                return String.format(getInstance(context).currentLocale, value, stringBuilder);
            } else {
                return String.format(value, stringBuilder);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "LOC_ERR: " + key;
        }
    }

    public static String formatString(Context context, String key, int res, Object... args) {
        try {
            String value = USE_CLOUD_STRINGS ? getInstance(context).localeValues.get(key) : null;
            if (value == null) {
                value = context.getString(res);
            }

            if (getInstance(context).currentLocale != null) {
                return String.format(getInstance(context).currentLocale, value, args);
            } else {
                return String.format(value, args);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "LOC_ERR: " + key;
        }
    }

    public static String formatTTLString(Context context, int ttl) {
        if (ttl < 60) {
            return MLang.formatPluralString(context, "Seconds", ttl);
        } else if (ttl < 60 * 60) {
            return MLang.formatPluralString(context, "Minutes", ttl / 60);
        } else if (ttl < 60 * 60 * 24) {
            return MLang.formatPluralString(context, "Hours", ttl / 60 / 60);
        } else if (ttl < 60 * 60 * 24 * 7) {
            return MLang.formatPluralString(context, "Days", ttl / 60 / 60 / 24);
        } else {
            int days = ttl / 60 / 60 / 24;
            if (ttl % 7 == 0) {
                return MLang.formatPluralString(context, "Weeks", days / 7);
            } else {
                return String.format("%s %s", MLang.formatPluralString(context, "Weeks", days / 7), MLang.formatPluralString(context, "Days", days % 7));
            }
        }
    }

    public String formatCurrencyString(long amount, String type) {
        type = type.toUpperCase();
        String customFormat;
        double doubleAmount;
        boolean discount = amount < 0;
        amount = Math.abs(amount);
        Currency currency = Currency.getInstance(type);
        switch (type) {
            case "CLF":
                customFormat = " %.4f";
                doubleAmount = amount / 10000.0;
                break;

            case "IRR":
                doubleAmount = amount / 100.0f;
                if (amount % 100 == 0) {
                    customFormat = " %.0f";
                } else {
                    customFormat = " %.2f";
                }
                break;

            case "BHD":
            case "IQD":
            case "JOD":
            case "KWD":
            case "LYD":
            case "OMR":
            case "TND":
                customFormat = " %.3f";
                doubleAmount = amount / 1000.0;
                break;

            case "BIF":
            case "BYR":
            case "CLP":
            case "CVE":
            case "DJF":
            case "GNF":
            case "ISK":
            case "JPY":
            case "KMF":
            case "KRW":
            case "MGA":
            case "PYG":
            case "RWF":
            case "UGX":
            case "UYI":
            case "VND":
            case "VUV":
            case "XAF":
            case "XOF":
            case "XPF":
                customFormat = " %.0f";
                doubleAmount = amount;
                break;

            case "MRO":
                customFormat = " %.1f";
                doubleAmount = amount / 10.0;
                break;

            default:
                customFormat = " %.2f";
                doubleAmount = amount / 100.0;
                break;
        }
        String result;
        if (currency != null) {
            NumberFormat format = NumberFormat.getCurrencyInstance(currentLocale != null ? currentLocale : systemDefaultLocale);
            format.setCurrency(currency);
            if (type.equals("IRR")) {
                format.setMaximumFractionDigits(0);
            }
            return (discount ? "-" : "") + format.format(doubleAmount);
        }
        return (discount ? "-" : "") + String.format(Locale.US, type + customFormat, doubleAmount);
    }

    public String formatCurrencyDecimalString(long amount, String type, boolean inludeType) {
        type = type.toUpperCase();
        String customFormat;
        double doubleAmount;
        amount = Math.abs(amount);
        switch (type) {
            case "CLF":
                customFormat = " %.4f";
                doubleAmount = amount / 10000.0;
                break;

            case "IRR":
                doubleAmount = amount / 100.0f;
                if (amount % 100 == 0) {
                    customFormat = " %.0f";
                } else {
                    customFormat = " %.2f";
                }
                break;

            case "BHD":
            case "IQD":
            case "JOD":
            case "KWD":
            case "LYD":
            case "OMR":
            case "TND":
                customFormat = " %.3f";
                doubleAmount = amount / 1000.0;
                break;

            case "BIF":
            case "BYR":
            case "CLP":
            case "CVE":
            case "DJF":
            case "GNF":
            case "ISK":
            case "JPY":
            case "KMF":
            case "KRW":
            case "MGA":
            case "PYG":
            case "RWF":
            case "UGX":
            case "UYI":
            case "VND":
            case "VUV":
            case "XAF":
            case "XOF":
            case "XPF":
                customFormat = " %.0f";
                doubleAmount = amount;
                break;

            case "MRO":
                customFormat = " %.1f";
                doubleAmount = amount / 10.0;
                break;

            default:
                customFormat = " %.2f";
                doubleAmount = amount / 100.0;
                break;
        }
        return String.format(Locale.US, inludeType ? type : "" + customFormat, doubleAmount).trim();
    }

    public static String formatStringSimple(Context context, String string, Object... args) {
        try {
            if (getInstance(context).currentLocale != null) {
                return String.format(getInstance(context).currentLocale, string, args);
            } else {
                return String.format(string, args);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "LOC_ERR: " + string;
        }
    }

    public static String formatCallDuration(Context context, int duration) {
        if (duration > 3600) {
            String result = MLang.formatPluralString(context, "Hours", duration / 3600);
            int minutes = duration % 3600 / 60;
            if (minutes > 0) {
                result += ", " + MLang.formatPluralString(context, "Minutes", minutes);
            }
            return result;
        } else if (duration > 60) {
            return MLang.formatPluralString(context, "Minutes", duration / 60);
        } else {
            return MLang.formatPluralString(context, "Seconds", duration);
        }
    }

    /**
     * 设备的语言设置发生改变
     * 用户在手机设置里改了默认语言，我们要跟着系统变
     * @param newConfig Configuration
     */
    public void onDeviceConfigurationChange(Context context, Configuration newConfig) {
        if (changingConfiguration) {
            return;
        }
        is24HourFormat = DateFormat.is24HourFormat(context);
        systemDefaultLocale = newConfig.locale;
        if (languageOverride != null) {
            LocaleInfo toSet = currentLocaleInfo;
            currentLocaleInfo = null;
            applyLanguage(context, toSet, false, false);
        } else {
            Locale newLocale = newConfig.locale;
            if (newLocale != null) {
                String d1 = newLocale.getDisplayName();
                String d2 = currentLocale.getDisplayName();
                if (d1 != null && d2 != null && !d1.equals(d2)) {
                    recreateFormatters(context);
                }
                currentLocale = newLocale;
                if (currentLocaleInfo != null && !TextUtils.isEmpty(currentLocaleInfo.pluralLangCode)) {
                    currentPluralRules = allRules.get(currentLocaleInfo.pluralLangCode);
                }
                if (currentPluralRules == null) {
                    currentPluralRules = allRules.get(currentLocale.getLanguage());
                    if (currentPluralRules == null) {
                        currentPluralRules = allRules.get("en");
                    }
                }
            }
        }
        String newSystemLocale = getSystemLocaleStringIso639(context);
        if (currentSystemLocale != null && !newSystemLocale.equals(currentSystemLocale)) {
            currentSystemLocale = newSystemLocale;
            //            ConnectionsManager.setSystemLangCode(currentSystemLocale);
        }
    }

    public static String formatDateChat(Context context, long date) {
        return formatDateChat(context, date, false);
    }

    public static String formatDateChat(Context context, long date, boolean checkYear) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            int currentYear = calendar.get(Calendar.YEAR);
            date *= 1000;

            calendar.setTimeInMillis(date);
            if (checkYear && currentYear == calendar.get(Calendar.YEAR) || !checkYear && Math.abs(System.currentTimeMillis() - date) < 31536000000L) {
                return getInstance(context).chatDate.format(date);
            }
            return getInstance(context).chatFullDate.format(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "LOC_ERR: formatDateChat";
    }

    public static String formatDate(Context context, long date) {
        try {
            date *= 1000;
            Calendar rightNow = Calendar.getInstance();
            int day = rightNow.get(Calendar.DAY_OF_YEAR);
            int year = rightNow.get(Calendar.YEAR);
            rightNow.setTimeInMillis(date);
            int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
            int dateYear = rightNow.get(Calendar.YEAR);

            if (dateDay == day && year == dateYear) {
                return getInstance(context).formatterDay.format(new Date(date));
            } else if (dateDay + 1 == day && year == dateYear) {
                return getString(context, "Yesterday", R.string.Yesterday);
            } else if (Math.abs(System.currentTimeMillis() - date) < 31536000000L) {
                return getInstance(context).formatterDayMonth.format(new Date(date));
            } else {
                return getInstance(context).formatterYear.format(new Date(date));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "LOC_ERR: formatDate";
    }

    public static String formatDateAudio(Context context, long date, boolean shortFormat) {
        try {
            date *= 1000;
            Calendar rightNow = Calendar.getInstance();
            int day = rightNow.get(Calendar.DAY_OF_YEAR);
            int year = rightNow.get(Calendar.YEAR);
            rightNow.setTimeInMillis(date);
            int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
            int dateYear = rightNow.get(Calendar.YEAR);

            if (dateDay == day && year == dateYear) {
                if (shortFormat) {
                    return MLang.formatString(context, "TodayAtFormatted", R.string.TodayAtFormatted, getInstance(context).formatterDay.format(new Date(date)));
                } else {
                    return MLang.formatString(context, "TodayAtFormattedWithToday", R.string.TodayAtFormattedWithToday, getInstance(context).formatterDay.format(new Date(date)));
                }
            } else if (dateDay + 1 == day && year == dateYear) {
                return MLang.formatString(context, "YesterdayAtFormatted", R.string.YesterdayAtFormatted, getInstance(context).formatterDay.format(new Date(date)));
            } else if (Math.abs(System.currentTimeMillis() - date) < 31536000000L) {
                return MLang.formatString(context, "formatDateAtTime", R.string.formatDateAtTime, getInstance(context).formatterDayMonth.format(new Date(date)), getInstance(context).formatterDay.format(new Date(date)));
            } else {
                return MLang.formatString(context, "formatDateAtTime", R.string.formatDateAtTime, getInstance(context).formatterYear.format(new Date(date)), getInstance(context).formatterDay.format(new Date(date)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "LOC_ERR";
    }

    public static String formatDateCallLog(Context context, long date) {
        try {
            date *= 1000;
            Calendar rightNow = Calendar.getInstance();
            int day = rightNow.get(Calendar.DAY_OF_YEAR);
            int year = rightNow.get(Calendar.YEAR);
            rightNow.setTimeInMillis(date);
            int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
            int dateYear = rightNow.get(Calendar.YEAR);

            if (dateDay == day && year == dateYear) {
                return getInstance(context).formatterDay.format(new Date(date));
            } else if (dateDay + 1 == day && year == dateYear) {
                return MLang.formatString(context, "YesterdayAtFormatted", R.string.YesterdayAtFormatted, getInstance(context).formatterDay.format(new Date(date)));
            } else if (Math.abs(System.currentTimeMillis() - date) < 31536000000L) {
                return MLang.formatString(context, "formatDateAtTime", R.string.formatDateAtTime, getInstance(context).chatDate.format(new Date(date)), getInstance(context).formatterDay.format(new Date(date)));
            } else {
                return MLang.formatString(context, "formatDateAtTime", R.string.formatDateAtTime, getInstance(context).chatFullDate.format(new Date(date)), getInstance(context).formatterDay.format(new Date(date)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "LOC_ERR";
    }

    public static String formatLocationUpdateDate(Context context, long date) {
        try {
            date *= 1000;
            Calendar rightNow = Calendar.getInstance();
            int day = rightNow.get(Calendar.DAY_OF_YEAR);
            int year = rightNow.get(Calendar.YEAR);
            rightNow.setTimeInMillis(date);
            int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
            int dateYear = rightNow.get(Calendar.YEAR);

            if (dateDay == day && year == dateYear) {
                int diff = (int) (getTimeFromServer() - date / 1000) / 60;
                if (diff < 1) {
                    return MLang.getString(context, "LocationUpdatedJustNow", R.string.LocationUpdatedJustNow);
                } else if (diff < 60) {
                    return MLang.formatPluralString(context, "UpdatedMinutes", diff);
                }
                return MLang.formatString(context, "LocationUpdatedFormatted", R.string.LocationUpdatedFormatted, MLang.formatString(context, "TodayAtFormatted", R.string.TodayAtFormatted, getInstance(context).formatterDay.format(new Date(date))));
            } else if (dateDay + 1 == day && year == dateYear) {
                return MLang.formatString(context, "LocationUpdatedFormatted", R.string.LocationUpdatedFormatted, MLang.formatString(context, "YesterdayAtFormatted", R.string.YesterdayAtFormatted, getInstance(context).formatterDay.format(new Date(date))));
            } else if (Math.abs(System.currentTimeMillis() - date) < 31536000000L) {
                String format = MLang.formatString(context, "formatDateAtTime", R.string.formatDateAtTime, getInstance(context).formatterDayMonth.format(new Date(date)), getInstance(context).formatterDay.format(new Date(date)));
                return MLang.formatString(context, "LocationUpdatedFormatted", R.string.LocationUpdatedFormatted, format);
            } else {
                String format = MLang.formatString(context, "formatDateAtTime", R.string.formatDateAtTime, getInstance(context).formatterYear.format(new Date(date)), getInstance(context).formatterDay.format(new Date(date)));
                return MLang.formatString(context, "LocationUpdatedFormatted", R.string.LocationUpdatedFormatted, format);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "LOC_ERR";
    }

    public static String formatLocationLeftTime(int time) {
        String text;
        int hours = time / 60 / 60;
        time -= hours * 60 * 60;
        int minutes = time / 60;
        time -= minutes * 60;
        if (hours != 0) {
            text = String.format("%dh", hours + (minutes > 30 ? 1 : 0));
        } else if (minutes != 0) {
            text = String.format("%d", minutes + (time > 30 ? 1 : 0));
        } else {
            text = String.format("%d", time);
        }
        return text;
    }

    public static String formatDateOnline(Context context, long date) {
        try {
            date *= 1000;
            Calendar rightNow = Calendar.getInstance();
            int day = rightNow.get(Calendar.DAY_OF_YEAR);
            int year = rightNow.get(Calendar.YEAR);
            rightNow.setTimeInMillis(date);
            int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
            int dateYear = rightNow.get(Calendar.YEAR);

            if (dateDay == day && year == dateYear) {
                return MLang.formatString(context, "LastSeenFormatted", R.string.LastSeenFormatted, MLang.formatString(context, "TodayAtFormatted", R.string.TodayAtFormatted, getInstance(context).formatterDay.format(new Date(date))));
                /*int diff = (int) (ConnectionsManager.getInstance().getCurrentTime() - date) / 60;
                if (diff < 1) {
                    return MLang.getString("LastSeenNow", R.string.LastSeenNow);
                } else if (diff < 60) {
                    return MLang.formatPluralString("LastSeenMinutes", diff);
                } else {
                    return MLang.formatPluralString("LastSeenHours", (int) Math.ceil(diff / 60.0f));
                }*/
            } else if (dateDay + 1 == day && year == dateYear) {
                return MLang.formatString(context, "LastSeenFormatted", R.string.LastSeenFormatted, MLang.formatString(context, "YesterdayAtFormatted", R.string.YesterdayAtFormatted, getInstance(context).formatterDay.format(new Date(date))));
            } else if (Math.abs(System.currentTimeMillis() - date) < 31536000000L) {
                String format = MLang.formatString(context, "formatDateAtTime", R.string.formatDateAtTime, getInstance(context).formatterDayMonth.format(new Date(date)), getInstance(context).formatterDay.format(new Date(date)));
                return MLang.formatString(context, "LastSeenDateFormatted", R.string.LastSeenDateFormatted, format);
            } else {
                String format = MLang.formatString(context, "formatDateAtTime", R.string.formatDateAtTime, getInstance(context).formatterYear.format(new Date(date)), getInstance(context).formatterDay.format(new Date(date)));
                return MLang.formatString(context, "LastSeenDateFormatted", R.string.LastSeenDateFormatted, format);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "LOC_ERR";
    }

    private FastDateFormat createFormatter(Locale locale, String format, String defaultFormat) {
        if (format == null || format.length() == 0) {
            format = defaultFormat;
        }
        FastDateFormat formatter;
        try {
            formatter = FastDateFormat.getInstance(format, locale);
        } catch (Exception e) {
            format = defaultFormat;
            formatter = FastDateFormat.getInstance(format, locale);
        }
        return formatter;
    }

    public void recreateFormatters(Context context) {
        Locale locale = currentLocale;
        if (locale == null) {
            locale = Locale.getDefault();
        }
        String lang = locale.getLanguage();
        if (lang == null) {
            lang = "en";
        }
        lang = lang.toLowerCase();
        isRTL = lang.length() == 2 && (lang.equals("ar") || lang.equals("fa") || lang.equals("he") || lang.equals("iw")) ||
                lang.startsWith("ar_") || lang.startsWith("fa_") || lang.startsWith("he_") || lang.startsWith("iw_")
                || currentLocaleInfo != null && currentLocaleInfo.isRtl;
        nameDisplayOrder = lang.equals("ko") ? 2 : 1;

        formatterDayMonth = createFormatter(locale, getStringInternal(context, "formatterMonth", R.string.formatterMonth), "dd MMM");
        formatterYear = createFormatter(locale, getStringInternal(context, "formatterYear", R.string.formatterYear), "dd.MM.yy");
        formatterYearMax = createFormatter(locale, getStringInternal(context, "formatterYearMax", R.string.formatterYearMax), "dd.MM.yyyy");
        chatDate = createFormatter(locale, getStringInternal(context, "chatDate", R.string.chatDate), "d MMMM");
        chatFullDate = createFormatter(locale, getStringInternal(context, "chatFullDate", R.string.chatFullDate), "d MMMM yyyy");
        formatterWeek = createFormatter(locale, getStringInternal(context, "formatterWeek", R.string.formatterWeek), "EEE");
        formatterScheduleDay = createFormatter(locale, getStringInternal(context, "formatDateSchedule", R.string.formatDateSchedule), "MMM d");
        formatterScheduleYear = createFormatter(locale, getStringInternal(context, "formatDateScheduleYear", R.string.formatDateScheduleYear), "MMM d yyyy");
        formatterDay = createFormatter(lang.toLowerCase().equals("ar") || lang.toLowerCase().equals("ko") ? locale : Locale.US, is24HourFormat ? getStringInternal(context, "formatterDay24H", R.string.formatterDay24H) : getStringInternal(context, "formatterDay12H", R.string.formatterDay12H), is24HourFormat ? "HH:mm" : "h:mm a");
        formatterStats = createFormatter(locale, is24HourFormat ? getStringInternal(context, "formatterStats24H", R.string.formatterStats24H) : getStringInternal(context, "formatterStats12H", R.string.formatterStats12H), is24HourFormat ? "MMM dd yyyy, HH:mm" : "MMM dd yyyy, h:mm a");
        formatterBannedUntil = createFormatter(locale, is24HourFormat ? getStringInternal(context, "formatterBannedUntil24H", R.string.formatterBannedUntil24H) : getStringInternal(context, "formatterBannedUntil12H", R.string.formatterBannedUntil12H), is24HourFormat ? "MMM dd yyyy, HH:mm" : "MMM dd yyyy, h:mm a");
        formatterBannedUntilThisYear = createFormatter(locale, is24HourFormat ? getStringInternal(context, "formatterBannedUntilThisYear24H", R.string.formatterBannedUntilThisYear24H) : getStringInternal(context, "formatterBannedUntilThisYear12H", R.string.formatterBannedUntilThisYear12H), is24HourFormat ? "MMM dd, HH:mm" : "MMM dd, h:mm a");
        formatterScheduleSend[0] = createFormatter(locale, getStringInternal(context, "SendTodayAt", R.string.SendTodayAt), "'Send today at' HH:mm");
        formatterScheduleSend[1] = createFormatter(locale, getStringInternal(context, "SendDayAt", R.string.SendDayAt), "'Send on' MMM d 'at' HH:mm");
        formatterScheduleSend[2] = createFormatter(locale, getStringInternal(context, "SendDayYearAt", R.string.SendDayYearAt), "'Send on' MMM d yyyy 'at' HH:mm");
        formatterScheduleSend[3] = createFormatter(locale, getStringInternal(context, "RemindTodayAt", R.string.RemindTodayAt), "'Remind today at' HH:mm");
        formatterScheduleSend[4] = createFormatter(locale, getStringInternal(context, "RemindDayAt", R.string.RemindDayAt), "'Remind on' MMM d 'at' HH:mm");
        formatterScheduleSend[5] = createFormatter(locale, getStringInternal(context, "RemindDayYearAt", R.string.RemindDayYearAt), "'Remind on' MMM d yyyy 'at' HH:mm");
    }

    public static boolean isRTLCharacter(char ch) {
        return Character.getDirectionality(ch) == Character.DIRECTIONALITY_RIGHT_TO_LEFT || Character.getDirectionality(ch) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC || Character.getDirectionality(ch) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING || Character.getDirectionality(ch) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE;
    }

    public static String formatSectionDate(Context context, long date) {
        try {
            date *= 1000;
            Calendar rightNow = Calendar.getInstance();
            int year = rightNow.get(Calendar.YEAR);
            rightNow.setTimeInMillis(date);
            int dateYear = rightNow.get(Calendar.YEAR);
            int month = rightNow.get(Calendar.MONTH);

            final String[] months = new String[]{
                    MLang.getString(context, "January", R.string.January),
                    MLang.getString(context, "February", R.string.February),
                    MLang.getString(context, "March", R.string.March),
                    MLang.getString(context, "April", R.string.April),
                    MLang.getString(context, "May", R.string.May),
                    MLang.getString(context, "June", R.string.June),
                    MLang.getString(context, "July", R.string.July),
                    MLang.getString(context, "August", R.string.August),
                    MLang.getString(context, "September", R.string.September),
                    MLang.getString(context, "October", R.string.October),
                    MLang.getString(context, "November", R.string.November),
                    MLang.getString(context, "December", R.string.December)
            };
            if (year == dateYear) {
                return months[month];
            } else {
                return months[month] + " " + dateYear;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "LOC_ERR";
    }

    public static String formatDateForBan(Context context, long date) {
        try {
            date *= 1000;
            Calendar rightNow = Calendar.getInstance();
            int year = rightNow.get(Calendar.YEAR);
            rightNow.setTimeInMillis(date);
            int dateYear = rightNow.get(Calendar.YEAR);

            if (year == dateYear) {
                return getInstance(context).formatterBannedUntilThisYear.format(new Date(date));
            } else {
                return getInstance(context).formatterBannedUntil.format(new Date(date));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "LOC_ERR";
    }

    public static String stringForMessageListDate(Context context, long date) {
        try {
            date *= 1000;
            Calendar rightNow = Calendar.getInstance();
            int day = rightNow.get(Calendar.DAY_OF_YEAR);
            rightNow.setTimeInMillis(date);
            int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);

            if (Math.abs(System.currentTimeMillis() - date) >= 31536000000L) {
                return getInstance(context).formatterYear.format(new Date(date));
            } else {
                int dayDiff = dateDay - day;
                if (dayDiff == 0 || dayDiff == -1 && System.currentTimeMillis() - date < 60 * 60 * 8 * 1000) {
                    return getInstance(context).formatterDay.format(new Date(date));
                } else if (dayDiff > -7 && dayDiff <= -1) {
                    return getInstance(context).formatterWeek.format(new Date(date));
                } else {
                    return getInstance(context).formatterDayMonth.format(new Date(date));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "LOC_ERR";
    }

    public static String formatShortNumber(int number, int[] rounded) {
        StringBuilder K = new StringBuilder();
        int lastDec = 0;
        int KCount = 0;
        while (number / 1000 > 0) {
            K.append("K");
            lastDec = (number % 1000) / 100;
            number /= 1000;
        }
        if (rounded != null) {
            double value = number + lastDec / 10.0;
            for (int a = 0; a < K.length(); a++) {
                value *= 1000;
            }
            rounded[0] = (int) value;
        }
        if (lastDec != 0 && K.length() > 0) {
            if (K.length() == 2) {
                return String.format(Locale.US, "%d.%dM", number, lastDec);
            } else {
                return String.format(Locale.US, "%d.%d%s", number, lastDec, K.toString());
            }
        }
        if (K.length() == 2) {
            return String.format(Locale.US, "%dM", number);
        } else {
            return String.format(Locale.US, "%d%s", number, K.toString());
        }
    }

    private String escapeString(String str) {
        if (str.contains("[CDATA")) {
            return str;
        }
        return str.replace("<", "&lt;").replace(">", "&gt;").replace("& ", "&amp; ");
    }

    public void saveRemoteLocaleStringsForCurrentLocale(Context context, final LangPackDifference difference) {
        if (currentLocaleInfo == null) {
            return;
        }
        final String langCode = difference.lang_code.replace('-', '_').toLowerCase();
        if (!langCode.equals(currentLocaleInfo.shortName) && !langCode.equals(currentLocaleInfo.baseLangCode)) {
            return;
        }
        saveRemoteLocaleStrings(context, currentLocaleInfo, difference);
    }

    public void saveRemoteLocaleStrings(Context context, LocaleInfo localeInfo, final LangPackDifference difference) {
        if (difference == null || difference.strings.isEmpty() || localeInfo == null || localeInfo.isLocal()) {
            return;
        }
        final String langCode = difference.lang_code.replace('-', '_').toLowerCase();
        int type;
        if (langCode.equals(localeInfo.shortName)) {
            type = 0;
        } else if (langCode.equals(localeInfo.baseLangCode)) {
            type = 1;
        } else {
            type = -1;
        }
        if (type == -1) {
            return;
        }
        File finalFile;
        if (type == 0) {
            finalFile = localeInfo.getPathToFile(context);
        } else {
            finalFile = localeInfo.getPathToBaseFile(context);
        }
        try {
            final HashMap<String, String> values;
            if (difference.from_version == 0) {
                values = new HashMap<>();
            } else {
                values = getLocaleFileStrings(finalFile, true);
            }
            for (int a = 0; a < difference.strings.size(); a++) {
                LangPackString string = difference.strings.get(a);
                if (string.isNewOrReplace()) {
                    values.put(string.key, escapeString(string.value));
                } else if (string.isDeleted()) {
                    values.remove(string.key);
                }
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(finalFile));
            writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            writer.write("<resources>\n");
            for (HashMap.Entry<String, String> entry : values.entrySet()) {
                writer.write(String.format("<string name=\"%1$s\">%2$s</string>\n", entry.getKey(), entry.getValue()));
            }
            writer.write("</resources>");
            writer.close();
            boolean hasBase = localeInfo.hasBaseLang();
            final HashMap<String, String> valuesToSet = getLocaleFileStrings(hasBase ? localeInfo.getPathToBaseFile(context) : localeInfo.getPathToFile(context));
            if (hasBase) {
                valuesToSet.putAll(getLocaleFileStrings(localeInfo.getPathToFile(context)));
            }
            runOnUIThread(() -> {
                if (localeInfo != null) {
                    if (type == 0) {
                        localeInfo.version = difference.version;
                    } else {
                        localeInfo.baseVersion = difference.version;
                    }
                }
                saveOtherLanguages(context);
                try {
                    if (currentLocaleInfo == localeInfo) {
                        Locale newLocale;
                        String[] args;
                        if (!TextUtils.isEmpty(localeInfo.pluralLangCode)) {
                            args = localeInfo.pluralLangCode.split("_");
                        } else if (!TextUtils.isEmpty(localeInfo.baseLangCode)) {
                            args = localeInfo.baseLangCode.split("_");
                        } else {
                            args = localeInfo.shortName.split("_");
                        }
                        if (args.length == 1) {
                            newLocale = new Locale(args[0]);
                        } else {
                            newLocale = new Locale(args[0], args[1]);
                        }
                        if (newLocale != null) {
                            languageOverride = localeInfo.shortName;

                            if (action != null) {
                                action.saveLanguageKeyInLocal(localeInfo.getKey());
                            }
                        }
                        if (newLocale != null) {
                            localeValues = valuesToSet;
                            currentLocale = newLocale;
                            currentLocaleInfo = localeInfo;
                            if (currentLocaleInfo != null && !TextUtils.isEmpty(currentLocaleInfo.pluralLangCode)) {
                                currentPluralRules = allRules.get(currentLocaleInfo.pluralLangCode);
                            }
                            if (currentPluralRules == null) {
                                currentPluralRules = allRules.get(currentLocale.getLanguage());
                                if (currentPluralRules == null) {
                                    currentPluralRules = allRules.get("en");
                                }
                            }
                            changingConfiguration = true;
                            Locale.setDefault(currentLocale);
                            Configuration config = new Configuration();
                            config.locale = currentLocale;
                            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
                            changingConfiguration = false;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    changingConfiguration = false;
                }
                recreateFormatters(context);
                //                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.reloadInterface);TODO ?
            });
        } catch (Exception ignore) {

        }
    }

    public void loadRemoteLanguages(Context context) {
        if (loadingRemoteLanguages) {
            return;
        }
        loadingRemoteLanguages = true;
        if (action != null) {
            action.langpack_getLanguages((LangAction.GetLanguagesCallback) languageList -> {
                loadingRemoteLanguages = false;
                for (int a = 0, size = remoteLanguages.size(); a < size; a++) {
                    remoteLanguages.get(a).serverIndex = Integer.MAX_VALUE;
                }
                for (int a = 0, size = languageList.size(); a < size; a++) {
                    LangPackLanguage language = (LangPackLanguage) languageList.get(a);
                    LocaleInfo localeInfo = new LocaleInfo();
                    localeInfo.nameEnglish = language.name;
                    localeInfo.name = language.native_name;
                    localeInfo.shortName = language.lang_code.replace('-', '_').toLowerCase();
                    if (language.base_lang_code != null) {
                        localeInfo.baseLangCode = language.base_lang_code.replace('-', '_').toLowerCase();
                    } else {
                        localeInfo.baseLangCode = "";
                    }
                    localeInfo.pluralLangCode = language.plural_code.replace('-', '_').toLowerCase();
                    localeInfo.isRtl = language.rtl;
                    localeInfo.pathToFile = "remote";
                    localeInfo.serverIndex = a;

                    LocaleInfo existing = getLanguageFromDict(localeInfo.getKey());
                    if (existing == null) {
                        languages.add(localeInfo);
                        languagesDict.put(localeInfo.getKey(), localeInfo);
                    } else {
                        existing.nameEnglish = localeInfo.nameEnglish;
                        existing.name = localeInfo.name;
                        existing.baseLangCode = localeInfo.baseLangCode;
                        existing.pluralLangCode = localeInfo.pluralLangCode;
                        existing.pathToFile = localeInfo.pathToFile;
                        existing.serverIndex = localeInfo.serverIndex;
                        localeInfo = existing;
                    }
                    if (!remoteLanguagesDict.containsKey(localeInfo.getKey())) {
                        remoteLanguages.add(localeInfo);
                        remoteLanguagesDict.put(localeInfo.getKey(), localeInfo);
                    }
                }
                for (int a = 0; a < remoteLanguages.size(); a++) {
                    LocaleInfo info = remoteLanguages.get(a);
                    if (info.serverIndex != Integer.MAX_VALUE || info == currentLocaleInfo) {
                        continue;
                    }
                    remoteLanguages.remove(a);
                    remoteLanguagesDict.remove(info.getKey());
                    languages.remove(info);
                    languagesDict.remove(info.getKey());
                    a--;
                }
                saveOtherLanguages(context);
                //                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.suggestedLangpack);TODO ?
                applyLanguage(context, currentLocaleInfo, true, false);
            });
        }
    }

    private void applyRemoteLanguage(Context context, LocaleInfo localeInfo, String langCode, boolean force) {
        if (localeInfo == null || localeInfo != null && !localeInfo.isRemote() && !localeInfo.isUnofficial()) {
            return;
        }
        if (localeInfo.hasBaseLang() && (langCode == null || langCode.equals(localeInfo.baseLangCode))) {
            if (localeInfo.baseVersion != 0 && !force) {
                if (localeInfo.hasBaseLang()) {
                    if (action != null) {
                        action.langpack_getDifference("", localeInfo.getLangCode(), localeInfo.version, new LangAction.GetDifferenceCallback() {
                            @Override
                            public void onLoad(LangPackDifference languageList) {
                                saveRemoteLocaleStrings(context, localeInfo, languageList);
                            }
                        });
                    }
                }
            } else {
                if (action != null) {
                    action.langpack_getLangPack(localeInfo.getBaseLangCode(), new LangAction.GetLangPackCallback() {
                        @Override
                        public void onLoad(LangPackDifference languageList) {
                            saveRemoteLocaleStrings(context, localeInfo, languageList);
                        }
                    });
                }
            }
        }
        if (langCode == null || langCode.equals(localeInfo.shortName)) {
            if (localeInfo.version != 0 && !force) {
                if (action != null) {
                    action.langpack_getDifference("", localeInfo.getLangCode(), localeInfo.version, new LangAction.GetDifferenceCallback() {
                        @Override
                        public void onLoad(LangPackDifference languageList) {
                            saveRemoteLocaleStrings(context, localeInfo, languageList);
                        }
                    });
                }
            } else {
                if (action != null) {
                    action.langpack_getLangPack(localeInfo.getLangCode(), new LangAction.GetLangPackCallback() {
                        @Override
                        public void onLoad(LangPackDifference languageList) {
                            saveRemoteLocaleStrings(context, localeInfo, languageList);
                        }
                    });
                }
            }
        }
    }

    public String getTranslitString(String src) {
        return getTranslitString(src, true, false);
    }

    public String getTranslitString(String src, boolean onlyEnglish) {
        return getTranslitString(src, true, onlyEnglish);
    }

    public String getTranslitString(String src, boolean ru, boolean onlyEnglish) {
        if (src == null) {
            return null;
        }

        if (ruTranslitChars == null) {
            ruTranslitChars = new HashMap<>(33);
            ruTranslitChars.put("а", "a");
            ruTranslitChars.put("б", "b");
            ruTranslitChars.put("в", "v");
            ruTranslitChars.put("г", "g");
            ruTranslitChars.put("д", "d");
            ruTranslitChars.put("е", "e");
            ruTranslitChars.put("ё", "yo");
            ruTranslitChars.put("ж", "zh");
            ruTranslitChars.put("з", "z");
            ruTranslitChars.put("и", "i");
            ruTranslitChars.put("й", "i");
            ruTranslitChars.put("к", "k");
            ruTranslitChars.put("л", "l");
            ruTranslitChars.put("м", "m");
            ruTranslitChars.put("н", "n");
            ruTranslitChars.put("о", "o");
            ruTranslitChars.put("п", "p");
            ruTranslitChars.put("р", "r");
            ruTranslitChars.put("с", "s");
            ruTranslitChars.put("т", "t");
            ruTranslitChars.put("у", "u");
            ruTranslitChars.put("ф", "f");
            ruTranslitChars.put("х", "h");
            ruTranslitChars.put("ц", "ts");
            ruTranslitChars.put("ч", "ch");
            ruTranslitChars.put("ш", "sh");
            ruTranslitChars.put("щ", "sch");
            ruTranslitChars.put("ы", "i");
            ruTranslitChars.put("ь", "");
            ruTranslitChars.put("ъ", "");
            ruTranslitChars.put("э", "e");
            ruTranslitChars.put("ю", "yu");
            ruTranslitChars.put("я", "ya");
        }

        if (translitChars == null) {
            translitChars = new HashMap<>(487);
            translitChars.put("ȼ", "c");
            translitChars.put("ᶇ", "n");
            translitChars.put("ɖ", "d");
            translitChars.put("ỿ", "y");
            translitChars.put("ᴓ", "o");
            translitChars.put("ø", "o");
            translitChars.put("ḁ", "a");
            translitChars.put("ʯ", "h");
            translitChars.put("ŷ", "y");
            translitChars.put("ʞ", "k");
            translitChars.put("ừ", "u");
            translitChars.put("ꜳ", "aa");
            translitChars.put("ĳ", "ij");
            translitChars.put("ḽ", "l");
            translitChars.put("ɪ", "i");
            translitChars.put("ḇ", "b");
            translitChars.put("ʀ", "r");
            translitChars.put("ě", "e");
            translitChars.put("ﬃ", "ffi");
            translitChars.put("ơ", "o");
            translitChars.put("ⱹ", "r");
            translitChars.put("ồ", "o");
            translitChars.put("ǐ", "i");
            translitChars.put("ꝕ", "p");
            translitChars.put("ý", "y");
            translitChars.put("ḝ", "e");
            translitChars.put("ₒ", "o");
            translitChars.put("ⱥ", "a");
            translitChars.put("ʙ", "b");
            translitChars.put("ḛ", "e");
            translitChars.put("ƈ", "c");
            translitChars.put("ɦ", "h");
            translitChars.put("ᵬ", "b");
            translitChars.put("ṣ", "s");
            translitChars.put("đ", "d");
            translitChars.put("ỗ", "o");
            translitChars.put("ɟ", "j");
            translitChars.put("ẚ", "a");
            translitChars.put("ɏ", "y");
            translitChars.put("ʌ", "v");
            translitChars.put("ꝓ", "p");
            translitChars.put("ﬁ", "fi");
            translitChars.put("ᶄ", "k");
            translitChars.put("ḏ", "d");
            translitChars.put("ᴌ", "l");
            translitChars.put("ė", "e");
            translitChars.put("ᴋ", "k");
            translitChars.put("ċ", "c");
            translitChars.put("ʁ", "r");
            translitChars.put("ƕ", "hv");
            translitChars.put("ƀ", "b");
            translitChars.put("ṍ", "o");
            translitChars.put("ȣ", "ou");
            translitChars.put("ǰ", "j");
            translitChars.put("ᶃ", "g");
            translitChars.put("ṋ", "n");
            translitChars.put("ɉ", "j");
            translitChars.put("ǧ", "g");
            translitChars.put("ǳ", "dz");
            translitChars.put("ź", "z");
            translitChars.put("ꜷ", "au");
            translitChars.put("ǖ", "u");
            translitChars.put("ᵹ", "g");
            translitChars.put("ȯ", "o");
            translitChars.put("ɐ", "a");
            translitChars.put("ą", "a");
            translitChars.put("õ", "o");
            translitChars.put("ɻ", "r");
            translitChars.put("ꝍ", "o");
            translitChars.put("ǟ", "a");
            translitChars.put("ȴ", "l");
            translitChars.put("ʂ", "s");
            translitChars.put("ﬂ", "fl");
            translitChars.put("ȉ", "i");
            translitChars.put("ⱻ", "e");
            translitChars.put("ṉ", "n");
            translitChars.put("ï", "i");
            translitChars.put("ñ", "n");
            translitChars.put("ᴉ", "i");
            translitChars.put("ʇ", "t");
            translitChars.put("ẓ", "z");
            translitChars.put("ỷ", "y");
            translitChars.put("ȳ", "y");
            translitChars.put("ṩ", "s");
            translitChars.put("ɽ", "r");
            translitChars.put("ĝ", "g");
            translitChars.put("ᴝ", "u");
            translitChars.put("ḳ", "k");
            translitChars.put("ꝫ", "et");
            translitChars.put("ī", "i");
            translitChars.put("ť", "t");
            translitChars.put("ꜿ", "c");
            translitChars.put("ʟ", "l");
            translitChars.put("ꜹ", "av");
            translitChars.put("û", "u");
            translitChars.put("æ", "ae");
            translitChars.put("ă", "a");
            translitChars.put("ǘ", "u");
            translitChars.put("ꞅ", "s");
            translitChars.put("ᵣ", "r");
            translitChars.put("ᴀ", "a");
            translitChars.put("ƃ", "b");
            translitChars.put("ḩ", "h");
            translitChars.put("ṧ", "s");
            translitChars.put("ₑ", "e");
            translitChars.put("ʜ", "h");
            translitChars.put("ẋ", "x");
            translitChars.put("ꝅ", "k");
            translitChars.put("ḋ", "d");
            translitChars.put("ƣ", "oi");
            translitChars.put("ꝑ", "p");
            translitChars.put("ħ", "h");
            translitChars.put("ⱴ", "v");
            translitChars.put("ẇ", "w");
            translitChars.put("ǹ", "n");
            translitChars.put("ɯ", "m");
            translitChars.put("ɡ", "g");
            translitChars.put("ɴ", "n");
            translitChars.put("ᴘ", "p");
            translitChars.put("ᵥ", "v");
            translitChars.put("ū", "u");
            translitChars.put("ḃ", "b");
            translitChars.put("ṗ", "p");
            translitChars.put("å", "a");
            translitChars.put("ɕ", "c");
            translitChars.put("ọ", "o");
            translitChars.put("ắ", "a");
            translitChars.put("ƒ", "f");
            translitChars.put("ǣ", "ae");
            translitChars.put("ꝡ", "vy");
            translitChars.put("ﬀ", "ff");
            translitChars.put("ᶉ", "r");
            translitChars.put("ô", "o");
            translitChars.put("ǿ", "o");
            translitChars.put("ṳ", "u");
            translitChars.put("ȥ", "z");
            translitChars.put("ḟ", "f");
            translitChars.put("ḓ", "d");
            translitChars.put("ȇ", "e");
            translitChars.put("ȕ", "u");
            translitChars.put("ȵ", "n");
            translitChars.put("ʠ", "q");
            translitChars.put("ấ", "a");
            translitChars.put("ǩ", "k");
            translitChars.put("ĩ", "i");
            translitChars.put("ṵ", "u");
            translitChars.put("ŧ", "t");
            translitChars.put("ɾ", "r");
            translitChars.put("ƙ", "k");
            translitChars.put("ṫ", "t");
            translitChars.put("ꝗ", "q");
            translitChars.put("ậ", "a");
            translitChars.put("ʄ", "j");
            translitChars.put("ƚ", "l");
            translitChars.put("ᶂ", "f");
            translitChars.put("ᵴ", "s");
            translitChars.put("ꞃ", "r");
            translitChars.put("ᶌ", "v");
            translitChars.put("ɵ", "o");
            translitChars.put("ḉ", "c");
            translitChars.put("ᵤ", "u");
            translitChars.put("ẑ", "z");
            translitChars.put("ṹ", "u");
            translitChars.put("ň", "n");
            translitChars.put("ʍ", "w");
            translitChars.put("ầ", "a");
            translitChars.put("ǉ", "lj");
            translitChars.put("ɓ", "b");
            translitChars.put("ɼ", "r");
            translitChars.put("ò", "o");
            translitChars.put("ẘ", "w");
            translitChars.put("ɗ", "d");
            translitChars.put("ꜽ", "ay");
            translitChars.put("ư", "u");
            translitChars.put("ᶀ", "b");
            translitChars.put("ǜ", "u");
            translitChars.put("ẹ", "e");
            translitChars.put("ǡ", "a");
            translitChars.put("ɥ", "h");
            translitChars.put("ṏ", "o");
            translitChars.put("ǔ", "u");
            translitChars.put("ʎ", "y");
            translitChars.put("ȱ", "o");
            translitChars.put("ệ", "e");
            translitChars.put("ế", "e");
            translitChars.put("ĭ", "i");
            translitChars.put("ⱸ", "e");
            translitChars.put("ṯ", "t");
            translitChars.put("ᶑ", "d");
            translitChars.put("ḧ", "h");
            translitChars.put("ṥ", "s");
            translitChars.put("ë", "e");
            translitChars.put("ᴍ", "m");
            translitChars.put("ö", "o");
            translitChars.put("é", "e");
            translitChars.put("ı", "i");
            translitChars.put("ď", "d");
            translitChars.put("ᵯ", "m");
            translitChars.put("ỵ", "y");
            translitChars.put("ŵ", "w");
            translitChars.put("ề", "e");
            translitChars.put("ứ", "u");
            translitChars.put("ƶ", "z");
            translitChars.put("ĵ", "j");
            translitChars.put("ḍ", "d");
            translitChars.put("ŭ", "u");
            translitChars.put("ʝ", "j");
            translitChars.put("ê", "e");
            translitChars.put("ǚ", "u");
            translitChars.put("ġ", "g");
            translitChars.put("ṙ", "r");
            translitChars.put("ƞ", "n");
            translitChars.put("ḗ", "e");
            translitChars.put("ẝ", "s");
            translitChars.put("ᶁ", "d");
            translitChars.put("ķ", "k");
            translitChars.put("ᴂ", "ae");
            translitChars.put("ɘ", "e");
            translitChars.put("ợ", "o");
            translitChars.put("ḿ", "m");
            translitChars.put("ꜰ", "f");
            translitChars.put("ẵ", "a");
            translitChars.put("ꝏ", "oo");
            translitChars.put("ᶆ", "m");
            translitChars.put("ᵽ", "p");
            translitChars.put("ữ", "u");
            translitChars.put("ⱪ", "k");
            translitChars.put("ḥ", "h");
            translitChars.put("ţ", "t");
            translitChars.put("ᵱ", "p");
            translitChars.put("ṁ", "m");
            translitChars.put("á", "a");
            translitChars.put("ᴎ", "n");
            translitChars.put("ꝟ", "v");
            translitChars.put("è", "e");
            translitChars.put("ᶎ", "z");
            translitChars.put("ꝺ", "d");
            translitChars.put("ᶈ", "p");
            translitChars.put("ɫ", "l");
            translitChars.put("ᴢ", "z");
            translitChars.put("ɱ", "m");
            translitChars.put("ṝ", "r");
            translitChars.put("ṽ", "v");
            translitChars.put("ũ", "u");
            translitChars.put("ß", "ss");
            translitChars.put("ĥ", "h");
            translitChars.put("ᵵ", "t");
            translitChars.put("ʐ", "z");
            translitChars.put("ṟ", "r");
            translitChars.put("ɲ", "n");
            translitChars.put("à", "a");
            translitChars.put("ẙ", "y");
            translitChars.put("ỳ", "y");
            translitChars.put("ᴔ", "oe");
            translitChars.put("ₓ", "x");
            translitChars.put("ȗ", "u");
            translitChars.put("ⱼ", "j");
            translitChars.put("ẫ", "a");
            translitChars.put("ʑ", "z");
            translitChars.put("ẛ", "s");
            translitChars.put("ḭ", "i");
            translitChars.put("ꜵ", "ao");
            translitChars.put("ɀ", "z");
            translitChars.put("ÿ", "y");
            translitChars.put("ǝ", "e");
            translitChars.put("ǭ", "o");
            translitChars.put("ᴅ", "d");
            translitChars.put("ᶅ", "l");
            translitChars.put("ù", "u");
            translitChars.put("ạ", "a");
            translitChars.put("ḅ", "b");
            translitChars.put("ụ", "u");
            translitChars.put("ằ", "a");
            translitChars.put("ᴛ", "t");
            translitChars.put("ƴ", "y");
            translitChars.put("ⱦ", "t");
            translitChars.put("ⱡ", "l");
            translitChars.put("ȷ", "j");
            translitChars.put("ᵶ", "z");
            translitChars.put("ḫ", "h");
            translitChars.put("ⱳ", "w");
            translitChars.put("ḵ", "k");
            translitChars.put("ờ", "o");
            translitChars.put("î", "i");
            translitChars.put("ģ", "g");
            translitChars.put("ȅ", "e");
            translitChars.put("ȧ", "a");
            translitChars.put("ẳ", "a");
            translitChars.put("ɋ", "q");
            translitChars.put("ṭ", "t");
            translitChars.put("ꝸ", "um");
            translitChars.put("ᴄ", "c");
            translitChars.put("ẍ", "x");
            translitChars.put("ủ", "u");
            translitChars.put("ỉ", "i");
            translitChars.put("ᴚ", "r");
            translitChars.put("ś", "s");
            translitChars.put("ꝋ", "o");
            translitChars.put("ỹ", "y");
            translitChars.put("ṡ", "s");
            translitChars.put("ǌ", "nj");
            translitChars.put("ȁ", "a");
            translitChars.put("ẗ", "t");
            translitChars.put("ĺ", "l");
            translitChars.put("ž", "z");
            translitChars.put("ᵺ", "th");
            translitChars.put("ƌ", "d");
            translitChars.put("ș", "s");
            translitChars.put("š", "s");
            translitChars.put("ᶙ", "u");
            translitChars.put("ẽ", "e");
            translitChars.put("ẜ", "s");
            translitChars.put("ɇ", "e");
            translitChars.put("ṷ", "u");
            translitChars.put("ố", "o");
            translitChars.put("ȿ", "s");
            translitChars.put("ᴠ", "v");
            translitChars.put("ꝭ", "is");
            translitChars.put("ᴏ", "o");
            translitChars.put("ɛ", "e");
            translitChars.put("ǻ", "a");
            translitChars.put("ﬄ", "ffl");
            translitChars.put("ⱺ", "o");
            translitChars.put("ȋ", "i");
            translitChars.put("ᵫ", "ue");
            translitChars.put("ȡ", "d");
            translitChars.put("ⱬ", "z");
            translitChars.put("ẁ", "w");
            translitChars.put("ᶏ", "a");
            translitChars.put("ꞇ", "t");
            translitChars.put("ğ", "g");
            translitChars.put("ɳ", "n");
            translitChars.put("ʛ", "g");
            translitChars.put("ᴜ", "u");
            translitChars.put("ẩ", "a");
            translitChars.put("ṅ", "n");
            translitChars.put("ɨ", "i");
            translitChars.put("ᴙ", "r");
            translitChars.put("ǎ", "a");
            translitChars.put("ſ", "s");
            translitChars.put("ȫ", "o");
            translitChars.put("ɿ", "r");
            translitChars.put("ƭ", "t");
            translitChars.put("ḯ", "i");
            translitChars.put("ǽ", "ae");
            translitChars.put("ⱱ", "v");
            translitChars.put("ɶ", "oe");
            translitChars.put("ṃ", "m");
            translitChars.put("ż", "z");
            translitChars.put("ĕ", "e");
            translitChars.put("ꜻ", "av");
            translitChars.put("ở", "o");
            translitChars.put("ễ", "e");
            translitChars.put("ɬ", "l");
            translitChars.put("ị", "i");
            translitChars.put("ᵭ", "d");
            translitChars.put("ﬆ", "st");
            translitChars.put("ḷ", "l");
            translitChars.put("ŕ", "r");
            translitChars.put("ᴕ", "ou");
            translitChars.put("ʈ", "t");
            translitChars.put("ā", "a");
            translitChars.put("ḙ", "e");
            translitChars.put("ᴑ", "o");
            translitChars.put("ç", "c");
            translitChars.put("ᶊ", "s");
            translitChars.put("ặ", "a");
            translitChars.put("ų", "u");
            translitChars.put("ả", "a");
            translitChars.put("ǥ", "g");
            translitChars.put("ꝁ", "k");
            translitChars.put("ẕ", "z");
            translitChars.put("ŝ", "s");
            translitChars.put("ḕ", "e");
            translitChars.put("ɠ", "g");
            translitChars.put("ꝉ", "l");
            translitChars.put("ꝼ", "f");
            translitChars.put("ᶍ", "x");
            translitChars.put("ǒ", "o");
            translitChars.put("ę", "e");
            translitChars.put("ổ", "o");
            translitChars.put("ƫ", "t");
            translitChars.put("ǫ", "o");
            translitChars.put("i̇", "i");
            translitChars.put("ṇ", "n");
            translitChars.put("ć", "c");
            translitChars.put("ᵷ", "g");
            translitChars.put("ẅ", "w");
            translitChars.put("ḑ", "d");
            translitChars.put("ḹ", "l");
            translitChars.put("œ", "oe");
            translitChars.put("ᵳ", "r");
            translitChars.put("ļ", "l");
            translitChars.put("ȑ", "r");
            translitChars.put("ȭ", "o");
            translitChars.put("ᵰ", "n");
            translitChars.put("ᴁ", "ae");
            translitChars.put("ŀ", "l");
            translitChars.put("ä", "a");
            translitChars.put("ƥ", "p");
            translitChars.put("ỏ", "o");
            translitChars.put("į", "i");
            translitChars.put("ȓ", "r");
            translitChars.put("ǆ", "dz");
            translitChars.put("ḡ", "g");
            translitChars.put("ṻ", "u");
            translitChars.put("ō", "o");
            translitChars.put("ľ", "l");
            translitChars.put("ẃ", "w");
            translitChars.put("ț", "t");
            translitChars.put("ń", "n");
            translitChars.put("ɍ", "r");
            translitChars.put("ȃ", "a");
            translitChars.put("ü", "u");
            translitChars.put("ꞁ", "l");
            translitChars.put("ᴐ", "o");
            translitChars.put("ớ", "o");
            translitChars.put("ᴃ", "b");
            translitChars.put("ɹ", "r");
            translitChars.put("ᵲ", "r");
            translitChars.put("ʏ", "y");
            translitChars.put("ᵮ", "f");
            translitChars.put("ⱨ", "h");
            translitChars.put("ŏ", "o");
            translitChars.put("ú", "u");
            translitChars.put("ṛ", "r");
            translitChars.put("ʮ", "h");
            translitChars.put("ó", "o");
            translitChars.put("ů", "u");
            translitChars.put("ỡ", "o");
            translitChars.put("ṕ", "p");
            translitChars.put("ᶖ", "i");
            translitChars.put("ự", "u");
            translitChars.put("ã", "a");
            translitChars.put("ᵢ", "i");
            translitChars.put("ṱ", "t");
            translitChars.put("ể", "e");
            translitChars.put("ử", "u");
            translitChars.put("í", "i");
            translitChars.put("ɔ", "o");
            translitChars.put("ɺ", "r");
            translitChars.put("ɢ", "g");
            translitChars.put("ř", "r");
            translitChars.put("ẖ", "h");
            translitChars.put("ű", "u");
            translitChars.put("ȍ", "o");
            translitChars.put("ḻ", "l");
            translitChars.put("ḣ", "h");
            translitChars.put("ȶ", "t");
            translitChars.put("ņ", "n");
            translitChars.put("ᶒ", "e");
            translitChars.put("ì", "i");
            translitChars.put("ẉ", "w");
            translitChars.put("ē", "e");
            translitChars.put("ᴇ", "e");
            translitChars.put("ł", "l");
            translitChars.put("ộ", "o");
            translitChars.put("ɭ", "l");
            translitChars.put("ẏ", "y");
            translitChars.put("ᴊ", "j");
            translitChars.put("ḱ", "k");
            translitChars.put("ṿ", "v");
            translitChars.put("ȩ", "e");
            translitChars.put("â", "a");
            translitChars.put("ş", "s");
            translitChars.put("ŗ", "r");
            translitChars.put("ʋ", "v");
            translitChars.put("ₐ", "a");
            translitChars.put("ↄ", "c");
            translitChars.put("ᶓ", "e");
            translitChars.put("ɰ", "m");
            translitChars.put("ᴡ", "w");
            translitChars.put("ȏ", "o");
            translitChars.put("č", "c");
            translitChars.put("ǵ", "g");
            translitChars.put("ĉ", "c");
            translitChars.put("ᶗ", "o");
            translitChars.put("ꝃ", "k");
            translitChars.put("ꝙ", "q");
            translitChars.put("ṑ", "o");
            translitChars.put("ꜱ", "s");
            translitChars.put("ṓ", "o");
            translitChars.put("ȟ", "h");
            translitChars.put("ő", "o");
            translitChars.put("ꜩ", "tz");
            translitChars.put("ẻ", "e");
        }
        StringBuilder dst = new StringBuilder(src.length());
        int len = src.length();
        boolean upperCase = false;
        for (int a = 0; a < len; a++) {
            String ch = src.substring(a, a + 1);
            if (onlyEnglish) {
                String lower = ch.toLowerCase();
                upperCase = !ch.equals(lower);
                ch = lower;
            }
            String tch = translitChars.get(ch);
            if (tch == null && ru) {
                tch = ruTranslitChars.get(ch);
            }
            if (tch != null) {
                if (onlyEnglish && upperCase) {
                    if (tch.length() > 1) {
                        tch = tch.substring(0, 1).toUpperCase() + tch.substring(1);
                    } else {
                        tch = tch.toUpperCase();
                    }
                }
                dst.append(tch);
            } else {
                if (onlyEnglish) {
                    char c = ch.charAt(0);
                    if (((c < 'a' || c > 'z') || (c < '0' || c > '9')) && c != ' ' && c != '\'' && c != ',' && c != '.' && c != '&' && c != '-' && c != '/') {
                        return null;
                    }
                    if (upperCase) {
                        ch = ch.toUpperCase();
                    }
                }
                dst.append(ch);
            }
        }
        return dst.toString();
    }

    /**
     * 对复数的处理
     *
     * 比如英语中，复数要加s等
     */
    abstract public static class PluralRules {
        abstract int quantityForNumber(int n);
    }

    public static class PluralRules_Zero extends PluralRules {
        public int quantityForNumber(int count) {
            if (count == 0 || count == 1) {
                return QUANTITY_ONE;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Welsh extends PluralRules {
        public int quantityForNumber(int count) {
            if (count == 0) {
                return QUANTITY_ZERO;
            } else if (count == 1) {
                return QUANTITY_ONE;
            } else if (count == 2) {
                return QUANTITY_TWO;
            } else if (count == 3) {
                return QUANTITY_FEW;
            } else if (count == 6) {
                return QUANTITY_MANY;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Two extends PluralRules {
        public int quantityForNumber(int count) {
            if (count == 1) {
                return QUANTITY_ONE;
            } else if (count == 2) {
                return QUANTITY_TWO;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Tachelhit extends PluralRules {
        public int quantityForNumber(int count) {
            if (count >= 0 && count <= 1) {
                return QUANTITY_ONE;
            } else if (count >= 2 && count <= 10) {
                return QUANTITY_FEW;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Slovenian extends PluralRules {
        public int quantityForNumber(int count) {
            int rem100 = count % 100;
            if (rem100 == 1) {
                return QUANTITY_ONE;
            } else if (rem100 == 2) {
                return QUANTITY_TWO;
            } else if (rem100 >= 3 && rem100 <= 4) {
                return QUANTITY_FEW;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Romanian extends PluralRules {
        public int quantityForNumber(int count) {
            int rem100 = count % 100;
            if (count == 1) {
                return QUANTITY_ONE;
            } else if ((count == 0 || (rem100 >= 1 && rem100 <= 19))) {
                return QUANTITY_FEW;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Polish extends PluralRules {
        public int quantityForNumber(int count) {
            int rem100 = count % 100;
            int rem10 = count % 10;
            if (count == 1) {
                return QUANTITY_ONE;
            } else if (rem10 >= 2 && rem10 <= 4 && !(rem100 >= 12 && rem100 <= 14)) {
                return QUANTITY_FEW;
            } else if (rem10 >= 0 && rem10 <= 1 || rem10 >= 5 && rem10 <= 9 || rem100 >= 12 && rem100 <= 14) {
                return QUANTITY_MANY;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_One extends PluralRules {
        public int quantityForNumber(int count) {
            return count == 1 ? QUANTITY_ONE : QUANTITY_OTHER;
        }
    }

    public static class PluralRules_None extends PluralRules {
        public int quantityForNumber(int count) {
            return QUANTITY_OTHER;
        }
    }

    public static class PluralRules_Maltese extends PluralRules {
        public int quantityForNumber(int count) {
            int rem100 = count % 100;
            if (count == 1) {
                return QUANTITY_ONE;
            } else if (count == 0 || (rem100 >= 2 && rem100 <= 10)) {
                return QUANTITY_FEW;
            } else if (rem100 >= 11 && rem100 <= 19) {
                return QUANTITY_MANY;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Macedonian extends PluralRules {
        public int quantityForNumber(int count) {
            if (count % 10 == 1 && count != 11) {
                return QUANTITY_ONE;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Lithuanian extends PluralRules {
        public int quantityForNumber(int count) {
            int rem100 = count % 100;
            int rem10 = count % 10;
            if (rem10 == 1 && !(rem100 >= 11 && rem100 <= 19)) {
                return QUANTITY_ONE;
            } else if (rem10 >= 2 && rem10 <= 9 && !(rem100 >= 11 && rem100 <= 19)) {
                return QUANTITY_FEW;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Latvian extends PluralRules {
        public int quantityForNumber(int count) {
            if (count == 0) {
                return QUANTITY_ZERO;
            } else if (count % 10 == 1 && count % 100 != 11) {
                return QUANTITY_ONE;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Langi extends PluralRules {
        public int quantityForNumber(int count) {
            if (count == 0) {
                return QUANTITY_ZERO;
            } else if (count == 1) {
                return QUANTITY_ONE;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_French extends PluralRules {
        public int quantityForNumber(int count) {
            if (count >= 0 && count < 2) {
                return QUANTITY_ONE;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Czech extends PluralRules {
        public int quantityForNumber(int count) {
            if (count == 1) {
                return QUANTITY_ONE;
            } else if (count >= 2 && count <= 4) {
                return QUANTITY_FEW;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Breton extends PluralRules {
        public int quantityForNumber(int count) {
            if (count == 0) {
                return QUANTITY_ZERO;
            } else if (count == 1) {
                return QUANTITY_ONE;
            } else if (count == 2) {
                return QUANTITY_TWO;
            } else if (count == 3) {
                return QUANTITY_FEW;
            } else if (count == 6) {
                return QUANTITY_MANY;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Balkan extends PluralRules {
        public int quantityForNumber(int count) {
            int rem100 = count % 100;
            int rem10 = count % 10;
            if (rem10 == 1 && rem100 != 11) {
                return QUANTITY_ONE;
            } else if (rem10 >= 2 && rem10 <= 4 && !(rem100 >= 12 && rem100 <= 14)) {
                return QUANTITY_FEW;
            } else if ((rem10 == 0 || (rem10 >= 5 && rem10 <= 9) || (rem100 >= 11 && rem100 <= 14))) {
                return QUANTITY_MANY;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Arabic extends PluralRules {
        public int quantityForNumber(int count) {
            int rem100 = count % 100;
            if (count == 0) {
                return QUANTITY_ZERO;
            } else if (count == 1) {
                return QUANTITY_ONE;
            } else if (count == 2) {
                return QUANTITY_TWO;
            } else if (rem100 >= 3 && rem100 <= 10) {
                return QUANTITY_FEW;
            } else if (rem100 >= 11 && rem100 <= 99) {
                return QUANTITY_MANY;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static String addNbsp(String src) {
        return src.replace(' ', '\u00A0');
    }

    private static Boolean useImperialSystemType;

    public static void resetImperialSystemType() {
        useImperialSystemType = null;
    }

    public static String formatDistance(Context context, float distance) {
        if (useImperialSystemType == null) {
            if (distanceSystemType == 0) {
                try {
                    TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    if (telephonyManager != null) {
                        String country = telephonyManager.getSimCountryIso().toUpperCase();
                        useImperialSystemType = "US".equals(country) || "GB".equals(country) || "MM".equals(country) || "LR".equals(country);
                    }
                } catch (Exception e) {
                    useImperialSystemType = false;
                    e.printStackTrace();
                }
            } else {
                useImperialSystemType = distanceSystemType == 2;
            }
        }
        if (useImperialSystemType) {
            distance *= 3.28084f;
            if (distance < 1000) {
                return formatString(context, "FootsAway", R.string.FootsAway, String.format("%d", (int) Math.max(1, distance)));
            } else {
                String arg;
                if (distance % 5280 == 0) {
                    arg = String.format("%d", (int) (distance / 5280));
                } else {
                    arg = String.format("%.2f", distance / 5280.0f);
                }
                return formatString(context, "MilesAway", R.string.MilesAway, arg);
            }
        } else {
            if (distance < 1000) {
                return formatString(context, "MetersAway2", R.string.MetersAway2, String.format("%d", (int) Math.max(1, distance)));
            } else {
                String arg;
                if (distance % 1000 == 0) {
                    arg = String.format("%d", (int) (distance / 1000));
                } else {
                    arg = String.format("%.2f", distance / 1000.0f);
                }
                return formatString(context, "KMetersAway2", R.string.KMetersAway2, arg);
            }
        }
    }

    //region utils
    public static int distanceSystemType = 0;
    public static boolean USE_CLOUD_STRINGS = false;
    public static Pattern pattern = Pattern.compile("[\\-0-9]+");

    public static Integer parseInt(CharSequence value) {
        if (value == null) {
            return 0;
        }
        int val = 0;
        try {
            Matcher matcher = pattern.matcher(value);
            if (matcher.find()) {
                String num = matcher.group(0);
                val = Integer.parseInt(num);
            }
        } catch (Exception ignore) {}
        return val;
    }

    public static boolean copyFile(InputStream sourceFile, File destFile) throws IOException {
        OutputStream out = new FileOutputStream(destFile);
        byte[] buf = new byte[4096];
        int len;
        while ((len = sourceFile.read(buf)) > 0) {
            Thread.yield();
            out.write(buf, 0, len);
        }
        out.close();
        return true;
    }

    public static boolean copyFile(File sourceFile, File destFile) throws IOException {
        if (sourceFile.equals(destFile)) {
            return true;
        }
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        try (FileInputStream source = new FileInputStream(sourceFile); FileOutputStream destination = new FileOutputStream(destFile)) {
            destination.getChannel().transferFrom(source.getChannel(), 0, source.getChannel().size());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static long getTimeFromServer() {
        if (action == null) {
            return System.currentTimeMillis();
        } else {
            return action.getTimeFromServer();
        }
    }

    @SuppressLint("SdCardPath")
    public static File getFilesDirFixed(Context context) {
        if (action == null) {
            return getFilesDirFixed(context, "/data/data/com.locale.lib/files");
        } else {
            return action.getFilesDirFixed(context);
        }
    }

    public static File getFilesDirFixed(Context context, String fallbackDirPath) {
        for (int a = 0; a < 10; a++) {
            File path = context.getFilesDir();
            if (path != null) {
                return path;
            }
        }
        try {
            ApplicationInfo info = context.getApplicationInfo();
            File path = new File(info.dataDir, "files");
            path.mkdirs();
            return path;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new File(fallbackDirPath);
    }

    public static void runOnUIThread(Runnable runnable) {
        if (action != null) {
            action.runOnUIThread(runnable);
        }
    }
    //endregion
}
