package com.locale.lib;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Handler;

import java.io.File;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/10/23
 * @description null
 * @usage null
 */
public class ApplicationLoader {
    public static Context applicationContext;

    public static File getFilesDirFixed() {
        for (int a = 0; a < 10; a++) {
            File path = ApplicationLoader.applicationContext.getFilesDir();
            if (path != null) {
                return path;
            }
        }
        try {
            ApplicationInfo info = applicationContext.getApplicationInfo();
            File path = new File(info.dataDir, "files");
            path.mkdirs();
            return path;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new File("/data/data/com.demo.chat.messager/files");
    }
    public static volatile Handler applicationHandler;

}
