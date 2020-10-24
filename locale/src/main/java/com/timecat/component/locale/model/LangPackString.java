package com.timecat.component.locale.model;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/10/23
 * @description 包差异, 键值对
 * @usage null
 */
public class LangPackString {
    public static final int DELETE = 1;
    public static final int NEW_OR_REPLACE = 0;
    public int flag = NEW_OR_REPLACE;
    public String key;
    public String value;

    public LangPackString(String key, String value) {
        this.flag = NEW_OR_REPLACE;
        this.key = key;
        this.value = value;
    }

    public LangPackString(int flag, String key, String value) {
        this.flag = flag;
        this.key = key;
        this.value = value;
    }

    public boolean isNewOrReplace() {
        return flag == NEW_OR_REPLACE;
    }
    public boolean isDeleted() {
        return flag == DELETE;
    }
}
