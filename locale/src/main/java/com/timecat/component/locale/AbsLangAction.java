package com.timecat.component.locale;

import android.content.Context;

import java.io.File;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/10/24
 * @description null
 * @usage null
 */
public abstract class AbsLangAction implements LangAction {
    /**
     * 同步串行调用
     * @return 时间（ms）
     */
    public abstract long getTimeFromServer();
    public abstract File getFilesDirFixed(Context context);
    public abstract void runOnUIThread(Runnable runnable);
}
