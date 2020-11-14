package com.timecat.component.locale;

import android.text.TextUtils;

import java.io.File;

import androidx.annotation.Nullable;

/**
 * 需要控制语言版本所需的所有配置项都在这里了
 */
public class LocaleInfo {

    /**
     * 语言名字，用该语言写的
     * 比如汉语写的"中文"，英语写的"English"
     */
    public String name;
    /**
     * 用英语写的名字
     * 比如中文"Chinese"，英语"English"
     */
    public String nameEnglish;
    /**
     * 语言名字的缩写
     * 如中文的"zh"，英语的"en"
     */
    public String shortName;
    /**
     * 语言包的存储路径，不一定是 uri
     * 远程语言包为 "remote"
     * 非官方语言包为 "unofficial"
     * 内置语言包为 null
     */
    @Nullable
    public String pathToFile;
    /**
     * 版本名
     */
    public String baseLangCode;
    public String pluralLangCode;
    /**
     * 是否从右到左阅读
     */
    public boolean isRtl;
    /**
     * 当前版本号
     */
    public int version;
    /**
     * 当前版本是针对哪个版本的升级
     */
    public int baseVersion;
    /**
     * 是否是内置支持的语言
     */
    public boolean builtIn;
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
                localeInfo.version = Util.parseInt(args[4]);
            }
            localeInfo.baseLangCode = args.length >= 6 ? args[5] : "";
            localeInfo.pluralLangCode = args.length >= 7 ? args[6] : localeInfo.shortName;
            if (args.length >= 8) {
                localeInfo.isRtl = Util.parseInt(args[7]) == 1;
            }
            if (args.length >= 9) {
                localeInfo.baseVersion = Util.parseInt(args[8]);
            }
            if (args.length >= 10) {
                localeInfo.serverIndex = Util.parseInt(args[9]);
            } else {
                localeInfo.serverIndex = Integer.MAX_VALUE;
            }
            if (!TextUtils.isEmpty(localeInfo.baseLangCode)) {
                localeInfo.baseLangCode = localeInfo.baseLangCode.replace("-", "_");
            }
        }
        return localeInfo;
    }

    public File getPathToFile(File filesDir) {
        if (isRemote()) {
            return new File(filesDir, "remote_" + shortName + ".xml");
        } else if (isUnofficial()) {
            return new File(filesDir, "unofficial_" + shortName + ".xml");
        }
        return !TextUtils.isEmpty(pathToFile) ? new File(pathToFile) : null;
    }

    public File getPathToBaseFile(File filesDir) {
        if (isUnofficial()) {
            return new File(filesDir, "unofficial_base_" + shortName + ".xml");
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
