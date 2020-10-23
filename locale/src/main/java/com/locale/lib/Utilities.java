package com.locale.lib;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/10/23
 * @description null
 * @usage null
 */
public class Utilities {

    public static int distanceSystemType = 0;
    public static boolean USE_CLOUD_STRINGS = false;

    public static long getTimeFromServer() {
        return System.currentTimeMillis();
    }
    public static Pattern pattern = Pattern.compile("[\\-0-9]+");

    public static Integer parseInt(CharSequence value) {
        if (value == null) {
            return 0;
        }
        int val = 0;
        try {
            Matcher matcher = pattern.matcher(value);
            if (matcher.find()) {
                String num = matcher.group(0);
                val = Integer.parseInt(num);
            }
        } catch (Exception ignore) {}
        return val;
    }

    public static void runOnUIThread(Runnable runnable) {
        runOnUIThread(runnable, 0);
    }

    public static void runOnUIThread(Runnable runnable, long delay) {
        if (delay == 0) {
            applicationHandler.post(runnable);
        } else {
            applicationHandler.postDelayed(runnable, delay);
        }
    }

    public static void cancelRunOnUIThread(Runnable runnable) {
        applicationHandler.removeCallbacks(runnable);
    }

    public static boolean copyFile(InputStream sourceFile, File destFile) throws IOException {
        OutputStream out = new FileOutputStream(destFile);
        byte[] buf = new byte[4096];
        int len;
        while ((len = sourceFile.read(buf)) > 0) {
            Thread.yield();
            out.write(buf, 0, len);
        }
        out.close();
        return true;
    }

    public static boolean copyFile(File sourceFile, File destFile) throws IOException {
        if (sourceFile.equals(destFile)) {
            return true;
        }
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        try (FileInputStream source = new FileInputStream(sourceFile); FileOutputStream destination = new FileOutputStream(destFile)) {
            destination.getChannel().transferFrom(source.getChannel(), 0, source.getChannel().size());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static Context applicationContext;
    public static volatile Handler applicationHandler;

    

    public static File getFilesDirFixed() {
        return getFilesDirFixed(applicationContext);
    }

    public static File getFilesDirFixed(Context context) {
        for (int a = 0; a < 10; a++) {
            File path = context.getFilesDir();
            if (path != null) {
                return path;
            }
        }
        try {
            ApplicationInfo info = context.getApplicationInfo();
            File path = new File(info.dataDir, "files");
            path.mkdirs();
            return path;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new File("/data/data/com.demo.chat.messager/files");
    }

}
