package com.timecat.ui;

import com.timecat.component.locale.LangAction;
import com.timecat.component.locale.model.LangPackDifference;
import com.timecat.component.locale.model.LangPackLanguage;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/11/14
 * @description 我的语言动作实现
 * 如果有需要的话，您可以在这里注入 Context
 * 也可以注入其他用于线程控制的对象
 */
public class MyLangAction implements LangAction {

    public void runOnUIThread(Runnable runnable) {
        MyApplication.applicationHandler.post(runnable);
    }

    @Override
    public void saveLanguageKeyInLocal(String language) {
        MyLang.saveLanguageKeyInLocal(language);
    }

    @Nullable
    @Override
    public String loadLanguageKeyInLocal() {
        return MyLang.loadLanguageKeyInLocal();
    }

    @Override
    public void langpack_getDifference(String lang_pack, String lang_code, int from_version, @NonNull final LangAction.GetDifferenceCallback callback) {
        Server.request_langpack_getDifference(lang_pack, lang_code, from_version, new Server.GetDifferenceCallback() {
            @Override
            public void onNext(final LangPackDifference difference) {
                runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        callback.onLoad(difference);
                    }
                });
            }
        });
    }

    @Override
    public void langpack_getLanguages(@NonNull final LangAction.GetLanguagesCallback callback) {
        Server.request_langpack_getLanguages(new Server.GetLanguagesCallback() {
            @Override
            public void onNext(final List<LangPackLanguage> languageList) {
                runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        callback.onLoad(languageList);
                    }
                });
            }
        });
    }

    @Override
    public void langpack_getLangPack(String lang_code, @NonNull final LangAction.GetLangPackCallback callback) {
        Server.request_langpack_getLangPack(lang_code, new Server.GetLangPackCallback() {
            @Override
            public void onNext(final LangPackDifference difference) {
                runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        callback.onLoad(difference);
                    }
                });
            }
        });
    }
}
