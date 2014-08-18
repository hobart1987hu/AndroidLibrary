package com.android.library.util;

import java.io.File;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.os.StatFs;

public class FileUtils {

    private FileUtils() {

    }

    public static boolean isMediaMounted() {
        return Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState());
    }

    /**
     * Get a usable cache directory (external if available, internal otherwise).
     * 
     * @param context The context to use
     * @param uniqueName A unique directory name to append to the cache dir
     * @return The cache dir
     */
    public static File getDiskCacheDirectory(Context context, String uniqueName) {
        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise use internal cache dir
        final String cachePath = isMediaMounted()
                || !isExternalStorageRemovable() ? getExternalCacheDir(context)
                .getPath() : context.getCacheDir().getPath();

        return new File(cachePath + File.pathSeparator + uniqueName);
    }

    /**
     * Check if external storage is built-in or removable.
     * 
     * @return True if external storage is removable (like an SD card), false
     *         otherwise.
     */
    @TargetApi(VERSION_CODES.GINGERBREAD)
    public static boolean isExternalStorageRemovable() {
        if (VersionUtils.hasGingerbread()) {
            return Environment.isExternalStorageRemovable();
        }
        return true;
    }

    /**
     * Get the external app cache directory.
     * 
     * @param context
     *            The context to use
     * @return The external cache dir
     */
    @TargetApi(VERSION_CODES.FROYO)
    public static File getExternalCacheDir(Context context) {
        if (VersionUtils.hasFroyo()) {
            return context.getExternalCacheDir();
        }

        // Before Froyo we need to construct the external cache dir ourselves
        final String cacheDir = "/Android/data/" + context.getPackageName()
                + "/cache/";
        return new File(Environment.getExternalStorageDirectory().getPath()
                + cacheDir);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static long getUsableSpace(File path) {
        if (VersionUtils.hasGingerbread()) {
            return path.getUsableSpace();
        }
        final StatFs stats = new StatFs(path.getPath());
        return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
    }
}
