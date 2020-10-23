package com.locale.lib;

import java.util.List;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/10/23
 * @description null
 * @usage null
 */
public interface LangAction {
    void langpack_getDifference(String lang_pack, String lang_code, int from_version, GetDifferenceCallback callback);

    void langpack_getLanguages(GetLanguagesCallback callback);
    void langpack_getLangPack(String lang_code, GetLangPackCallback callback);

    public interface GetLanguagesCallback {
        void onLoad(List<LangPackLanguage> languageList);
    }
    public interface GetDifferenceCallback {
        void onLoad(LangPackDifference languageList);
    }
    public interface GetLangPackCallback {
        void onLoad(LangPackDifference languageList);
    }

}
