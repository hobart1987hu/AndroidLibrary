package com.android.library.util;

import android.os.Build;
import android.os.Build.VERSION_CODES;

public class VersionUtils {

    /**
     * {@link Build.VERSION.SDK_INT} >={@link VERSION_CODES.FROYO}
     */
    public static boolean hasFroyo() {
        // Can use static final constants like FROYO, declared in later versions
        // of the OS since they are inlined at compile time. This is guaranteed behavior.
        return Build.VERSION.SDK_INT >= VERSION_CODES.FROYO;
    }

    /**
     * {@linkBuild.VERSION.SDK_INT }>= {@linkVERSION_CODES.GINGERBREAD}
     */
    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.GINGERBREAD;
    }

    /**
     * {@link Build.VERSION.SDK_INT} >= {@linkVERSION_CODES.HONEYCOMB}
     */
    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB;
    }

    /**
     * {@link Build.VERSION.SDK_INT} >= {@linkVERSION_CODES.HONEYCOMB_MR1}
     */
    public static boolean hasHoneycombMR1() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB_MR1;
    }

    /**
     * {@linkBuild.VERSION.SDK_INT }>= {@linkVERSION_CODES.JELLY_BEAN}
     */
    public static boolean hasJellyBean() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN;
    }

    /**
     * {@link Build.VERSION.SDK_INT} >= {@link VERSION_CODES.KITKAT}
     */
    public static boolean hasKitKat() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.KITKAT;
    }
}
