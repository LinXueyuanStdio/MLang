package com.timecat.ui;

import com.timecat.component.locale.model.LangPackDifference;
import com.timecat.component.locale.model.LangPackLanguage;
import com.timecat.component.locale.model.LangPackString;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/10/24
 * @description 模仿服务器端
 * @usage null
 */
public class Server {
    public static ArrayList<LangPackString> englishStrings() {
        ArrayList<LangPackString> list = new ArrayList<>();
        list.add(new LangPackString("LanguageName", "English"));
        list.add(new LangPackString("test", "test"));
        return list;
    }

    public static ArrayList<LangPackString> chineseStrings() {
        ArrayList<LangPackString> list = new ArrayList<>();
        list.add(new LangPackString("LanguageName", "中文简体"));
        list.add(new LangPackString("test", "测试"));
        return list;
    }

    public static ArrayList<LangPackString> frenchStrings() {
        ArrayList<LangPackString> list = new ArrayList<>();
        list.add(new LangPackString("LanguageName", "法语"));
        list.add(new LangPackString("test", "000测试"));
        return list;
    }

    public static LangPackDifference englishPackDifference() {
        LangPackDifference difference = new LangPackDifference();
        difference.lang_code = "en";
        difference.from_version = 0;
        difference.version = 1;
        difference.strings = englishStrings();
        return difference;
    }

    public static LangPackDifference frenchPackDifference() {
        LangPackDifference difference = new LangPackDifference();
        difference.lang_code = "fr";
        difference.from_version = 0;
        difference.version = 1;
        difference.strings = frenchStrings();
        return difference;
    }

    public static LangPackDifference chinesePackDifference() {
        LangPackDifference difference = new LangPackDifference();
        difference.lang_code = "zh";
        difference.from_version = 0;
        difference.version = 1;
        difference.strings = chineseStrings();
        return difference;
    }

    public static LangPackLanguage chineseLanguage() {
        LangPackLanguage langPackLanguage = new LangPackLanguage();
        langPackLanguage.name = "chinese";
        langPackLanguage.native_name = "简体中文";
        langPackLanguage.lang_code = "zh";
        langPackLanguage.base_lang_code = "zh";
        return langPackLanguage;
    }

    public static LangPackLanguage frenchLanguage() {
        LangPackLanguage langPackLanguage = new LangPackLanguage();
        langPackLanguage.name = "french";
        langPackLanguage.native_name = "French";
        langPackLanguage.lang_code = "fr";
        langPackLanguage.base_lang_code = "fr";
        return langPackLanguage;
    }

    public static LangPackLanguage englishLanguage() {
        LangPackLanguage langPackLanguage = new LangPackLanguage();
        langPackLanguage.name = "english";
        langPackLanguage.native_name = "English";
        langPackLanguage.lang_code = "en";
        langPackLanguage.base_lang_code = "en";
        return langPackLanguage;
    }

    public static List<LangPackLanguage> avaliable() {
        List<LangPackLanguage> langPackLanguages = new ArrayList<>();
        langPackLanguages.add(englishLanguage());
        langPackLanguages.add(chineseLanguage());
        langPackLanguages.add(frenchLanguage());
        return langPackLanguages;
    }

    public static void request_langpack_getDifference(String lang_pack, String lang_code, int from_version, @NonNull final GetDifferenceCallback callback) {
        if ("zh".equals(lang_code)) {
            callback.onNext(chinesePackDifference());
        } else if ("en".equals(lang_code)) {
            callback.onNext(englishPackDifference());
        } else if ("fr".equals(lang_code)) {
            callback.onNext(frenchPackDifference());
        }
    }

    public static void request_langpack_getLanguages(@NonNull GetLanguagesCallback callback) {
        callback.onNext(avaliable());
    }

    public static void request_langpack_getLangPack(String lang_code, @NonNull GetLangPackCallback callback) {
        if ("zh".equals(lang_code)) {
            callback.onNext(chinesePackDifference());
        } else if ("en".equals(lang_code)) {
            callback.onNext(englishPackDifference());
        } else if ("fr".equals(lang_code)) {
            callback.onNext(frenchPackDifference());
        }
    }

    public interface GetDifferenceCallback {
        void onNext(final LangPackDifference difference);
    }

    public interface GetLanguagesCallback {
        void onNext(final List<LangPackLanguage> languageList);
    }

    public interface GetLangPackCallback {
        void onNext(final LangPackDifference difference);
    }
}
