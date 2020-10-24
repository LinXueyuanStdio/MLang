package com.locale.ui;

import com.locale.lib.model.LangPackDifference;
import com.locale.lib.model.LangPackLanguage;
import com.locale.lib.model.LangPackString;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/10/24
 * @description null
 * @usage null
 */
public class Server {
    public static ArrayList<LangPackString> englishStrings() {
        ArrayList<LangPackString> list = new ArrayList<>();
        list.add(new LangPackString("", ""));
        return list;
    }
    public static ArrayList<LangPackString> chineseStrings() {
        ArrayList<LangPackString> list = new ArrayList<>();
        list.add(new LangPackString("", ""));
        return list;
    }

    public static LangPackDifference englishPackDifference() {
        LangPackDifference difference = new LangPackDifference();
        difference.lang_code = "english";
        difference.from_version = 0;
        difference.version = 1;
        difference.strings = englishStrings();
        return difference;
    }

    public static LangPackDifference chinesePackDifference() {
        LangPackDifference difference = new LangPackDifference();
        difference.lang_code = "chinese";
        difference.from_version = 0;
        difference.version = 1;
        difference.strings = chineseStrings();
        return difference;
    }

    public static LangPackLanguage chineseLanguage() {
        LangPackLanguage langPackLanguage = new LangPackLanguage();
        langPackLanguage.name = "chinese";
        langPackLanguage.native_name = "简体中文";
        return langPackLanguage;
    }
    public static LangPackLanguage englishLanguage() {
        LangPackLanguage langPackLanguage = new LangPackLanguage();
        langPackLanguage.name = "english";
        langPackLanguage.native_name = "English";
        return langPackLanguage;
    }

    public static List<LangPackLanguage> avaliable() {
        List<LangPackLanguage> langPackLanguages = new ArrayList<>();
        langPackLanguages.add(englishLanguage());
        langPackLanguages.add(chineseLanguage());
        return langPackLanguages;
    }

    public static void request_langpack_getDifference(String lang_pack, String lang_code, int from_version, @NonNull final GetDifferenceCallback callback) {

    }
    public static void request_langpack_getLanguages(@NonNull GetLanguagesCallback callback) {

    }
    public static void request_langpack_getLangPack(String lang_code, @NonNull GetLangPackCallback callback) {

    }

    public interface GetDifferenceCallback{
        void onNext(final LangPackDifference difference);
    }
    public interface GetLanguagesCallback{
        void onNext(final List<LangPackLanguage> languageList);
    }
    public interface GetLangPackCallback{
        void onNext(final LangPackDifference difference);
    }
}
