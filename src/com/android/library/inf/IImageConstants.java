package com.android.library.inf;

import android.graphics.Bitmap.CompressFormat;

public interface IImageConstants {

    public static final int            HTTP_CACHE_SIZE                   = 10 * 1024 * 1024;   // 10MB
    public static final String         HTTP_CACHE_DIR                    = "http";
    public static final String         IMAGE_CACHE_DIR                   = "images";
    public static final int            IO_BUFFER_SIZE                    = 8 * 1024;

    // ImageCacheParames start
    // Default memory cache size in kilobytes
    public static final int            DEFAULT_MEM_CACHE_SIZE            = 1024 * 1024 * 5;    // 5MB

    // Default disk cache size in bytes
    public static final int            DEFAULT_DISK_CACHE_SIZE           = 1024 * 1024 * 10;   // 10MB

    // Compression settings when writing images to disk cache
    public static final CompressFormat DEFAULT_COMPRESS_FORMAT           = CompressFormat.JPEG;
    public static final int            DEFAULT_COMPRESS_QUALITY          = 70;

    // Constants to easily toggle various caches
    public static final boolean        DEFAULT_MEM_CACHE_ENABLED         = true;
    public static final boolean        DEFAULT_DISK_CACHE_ENABLED        = true;

    // ImageCacheParames end

    public static final boolean              FADEIN_BITMAP                     = true;

    public static final int    FADE_IN_TIME            = 200;
    
    public static final int                 DISK_CACHE_INDEX   = 0;
    
}
