package com.locale.lib;

import com.locale.lib.model.LangPackDifference;
import com.locale.lib.model.LangPackLanguage;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/10/23
 * @description null
 * @usage null
 */
public interface LangAction {
    /**
     * SharedPreferences preferences = Utilities.getGlobalMainSettings();
     * SharedPreferences.Editor editor = preferences.edit();
     * editor.putString("language", language);
     * editor.commit();
     * @param language localeInfo.getKey()
     */
    void saveLanguageKeyInLocal(String language);

    /**
     * SharedPreferences preferences = Utilities.getGlobalMainSettings();
     * String lang = preferences.getString("language", null);
     * @return lang
     */
    @Nullable
    String loadLanguageKeyInLocal();

    void langpack_getDifference(String lang_pack, String lang_code, int from_version, @NonNull GetDifferenceCallback callback);

    void langpack_getLanguages(@NonNull GetLanguagesCallback callback);

    void langpack_getLangPack(String lang_code, @NonNull GetLangPackCallback callback);

    interface GetLanguagesCallback {
        /**
         * 必须在UI线程或者主线程调用
         * @param languageList
         */
        void onLoad(List<LangPackLanguage> languageList);
    }

    interface GetDifferenceCallback {
        /**
         * 必须在UI线程或者主线程调用
         * @param languageList
         */
        void onLoad(LangPackDifference languageList);
    }

    interface GetLangPackCallback {
        /**
         * 必须在UI线程或者主线程调用
         * @param languageList
         */
        void onLoad(LangPackDifference languageList);
    }

}
