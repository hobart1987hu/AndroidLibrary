package com.android.library.util;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Environment;

public class SdcardUtils {

    private SdcardUtils() {

    }

    public static boolean isSdcardMounted() {
        return Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static boolean isExternalStorageRemovable() {
        return Environment.isExternalStorageRemovable();
    }
}
