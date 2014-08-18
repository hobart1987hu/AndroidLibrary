package com.android.library.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;

import android.content.Context;
import android.os.Environment;

public class Utils {

    private Utils(){
    };

    public static long getCacheSize(Context context) {
        final String cachePath = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                                 || !FileUtils.isExternalStorageRemovable() ? FileUtils.getExternalCacheDir(context).getPath() : context.getCacheDir().getPath();
        File file = new File(cachePath);
        long size = 0;
        if (null != file && file.exists()) {
            try {
                size = getFileSizes(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return size;
    }

    private static long getFileSizes(File f) throws Exception {
        long size = 0;
        File flist[] = f.listFiles();
        for (int i = 0; i < flist.length; i++) {
            if (flist[i].isDirectory()) {
                size = size + getFileSizes(flist[i]);
            } else {
                size = size + getFileSize(flist[i]);
            }
        }
        return size;
    }

    public static long getFileSize(File f) {
        long size = 0;
        if (f.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(f);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                size = fis.available();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return size;
    }

    public static String getCacheSizeForMB(Context context) {
        double size = (double)Utils.getCacheSize(context) / (1024 * 1024);
        DecimalFormat decimalFormat = new DecimalFormat("##0.00");
        return decimalFormat.format(size) + "MB";
    }
}
