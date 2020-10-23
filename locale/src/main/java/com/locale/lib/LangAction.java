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
    void langpack_getDifference(String lang_pack, String lang_code, int from_version);
    List<LangPackLanguage> langpack_getLanguages();
}
