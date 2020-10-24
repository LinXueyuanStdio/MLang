package com.locale.lib.model;

import java.util.ArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/10/23
 * @description 语言包差异
 * 更新语言包的时候，增量更新
 * @usage null
 */
public class LangPackDifference {
    /**
     * 版本名字
     */
    public String lang_code;
    /**
     * 需要更新的最低版本号
     */
    public int from_version;
    /**
     * 更新完后的版本号
     */
    public int version;
    /**
     * 包差异
     */
    public ArrayList<LangPackString> strings = new ArrayList<>();

}
