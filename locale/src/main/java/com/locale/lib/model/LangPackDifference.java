package com.locale.lib.model;

import java.util.ArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/10/23
 * @description null
 * @usage null
 */
public class LangPackDifference {
    public String lang_code;
    public int from_version;
    public int version;
    public ArrayList<LangPackString> strings = new ArrayList<>();

}
