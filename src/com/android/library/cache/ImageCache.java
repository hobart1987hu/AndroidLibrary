package com.android.library.cache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.android.library.util.FileUtils;
import com.android.library.util.Utils;
import com.android.library.util.VersionUtils;

public class ImageCache {

    private static final String      TAG              = "ImageCache";

    private static final int         DISK_CACHE_INDEX = 0;

    private ImageCacheParams         mCacheParams;

    private LruCache<String, Bitmap> mMemoCache;

    private DiskLruCache             mDiskLruCache;

    private final Object             mDiskCacheLock   = new Object();

    public ImageCache(ImageCacheParams params){
        init(params);
    }

    private void init(ImageCacheParams params) {
        this.mCacheParams = params;
        mMemoCache = new LruCache<String, Bitmap>(mCacheParams.mMemoCacheSize) {

            @Override
            protected int sizeOf(String key, Bitmap value) {
                final int bitmapSize = getBitmapSize(value) / 1024;
                return bitmapSize == 0 ? 1 : bitmapSize;
            }
        };
        initDiskCache();
    }

    public DiskLruCache getDiskCache() {
        return mDiskLruCache;
    }

    public void initDiskCache() {
        // Set up disk cache
        synchronized (mDiskCacheLock) {
            if (mDiskLruCache == null || mDiskLruCache.isClosed()) {
                File diskCacheDir = mCacheParams.mDiskFile;
                if (diskCacheDir != null) {
                    if (!diskCacheDir.exists()) {
                        diskCacheDir.mkdirs();
                    }

                    Log.d(TAG, "disk cache path:" + diskCacheDir.getAbsolutePath());

                    final long usableSpace = FileUtils.getUsableSpace(diskCacheDir);

                    Log.d(TAG, "usableSpace:" + usableSpace);

                    if (usableSpace > mCacheParams.mDiskCacheSize) {
                        try {
                            mDiskLruCache = DiskLruCache.open(diskCacheDir, 1, 1, mCacheParams.mDiskCacheSize);
                            Log.d(TAG, "Disk cache initialized");
                        } catch (final IOException e) {
                            mCacheParams.mDiskFile = null;
                            Log.e(TAG, "initDiskCache - " + e);
                        }
                    }
                }
            }
            mDiskCacheLock.notifyAll();
        }
    }

    public void addBitmapToCache(String data, Bitmap bitmap) {
        if (null == data || null == bitmap) {
            Log.d(TAG, "addBitmapToCache data is null or bitmap is null...");
            return;
        }
        if (null != mMemoCache) {
            mMemoCache.put(data, bitmap);
        }
        synchronized (mDiskCacheLock) {
            // Add to disk cache
            if (null != mDiskLruCache) {
                final String key = Utils.hashKeyForDisk(data);
                OutputStream out = null;
                try {
                    DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
                    if (snapshot == null) {
                        final DiskLruCache.Editor editor = mDiskLruCache.edit(key);
                        if (editor != null) {
                            out = editor.newOutputStream(DISK_CACHE_INDEX);
                            bitmap.compress(CompressFormat.JPEG, 100, out);
                            editor.commit();
                            out.close();
                        }
                    } else {
                        snapshot.getInputStream(DISK_CACHE_INDEX).close();
                    }
                } catch (final IOException e) {
                    Log.e(TAG, "addBitmapToCache IOException - " + e);
                } catch (Exception e) {
                    Log.e(TAG, "addBitmapToCache Exception - " + e);
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    public Bitmap getBitmapFromMemoCache(String data) {
        if (null != mMemoCache) {
            return mMemoCache.get(data);
        }
        return null;
    }

    public Bitmap getBitmapFromDiskCache(String data) {
        final String key = Utils.hashKeyForDisk(data);
        Bitmap bitmap = null;
        synchronized (mDiskCacheLock) {
            if (mDiskLruCache != null) {
                InputStream inputStream = null;
                try {
                    final DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
                    if (snapshot != null) {
                        Log.d(TAG, "Disk cache hit");
                        inputStream = snapshot.getInputStream(DISK_CACHE_INDEX);
                        if (inputStream != null) {
                            bitmap = BitmapFactory.decodeStream(inputStream);
                        }
                    }
                } catch (final IOException e) {
                    Log.e(TAG, "getBitmapFromDiskCache - " + e);
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch (IOException e) {
                    }
                }
            }
            return bitmap;
        }
    }

    // @TargetApi(VERSION_CODES.KITKAT)
    private int getBitmapSize(Bitmap value) {

        // if (VersionUtils.hasKitKat()) {
        // return value.getAllocationByteCount();
        // }
        // if (VersionUtils.hasHoneycombMR1()) {
        // return value.getByteCount();
        // }
        return value.getRowBytes() * value.getHeight();
    }

    public static class ImageCacheParams {

        public File mDiskFile;

        public int  mDiskCacheSize;

        public int  mMemoCacheSize;

        public ImageCacheParams(Context context, String diskCacheName){
            mDiskFile = FileUtils.getDiskCacheDirectory(context, diskCacheName);
        }
    }

    /**
     * Clears both the memory and disk cache associated with this ImageCache object. Note that this includes disk access
     * so this should not be executed on the main/UI thread.
     */
    public void clearCache() {
        if (mMemoCache != null) {
            mMemoCache.evictAll();
            Log.d(TAG, "Memory cache cleared");
        }
        synchronized (mDiskCacheLock) {
            if (mDiskLruCache != null && !mDiskLruCache.isClosed()) {
                try {
                    mDiskLruCache.delete();
                    Log.d(TAG, "Disk cache cleared");
                } catch (IOException e) {
                    Log.e(TAG, "clearCache - " + e);
                }
                mDiskLruCache = null;
                initDiskCache();
            }
        }
    }

    /**
     * Flushes the disk cache associated with this ImageCache object. Note that this includes disk access so this should
     * not be executed on the main/UI thread.
     */
    public void flush() {
        synchronized (mDiskCacheLock) {
            if (mDiskLruCache != null) {
                try {
                    mDiskLruCache.flush();
                    Log.d(TAG, "Disk cache flushed");
                } catch (IOException e) {
                    Log.e(TAG, "flush - " + e);
                }
            }
        }
    }

    /**
     * Closes the disk cache associated with this ImageCache object. Note that this includes disk access so this should
     * not be executed on the main/UI thread.
     */
    public void close() {
        synchronized (mDiskCacheLock) {
            if (mDiskLruCache != null) {
                try {
                    if (!mDiskLruCache.isClosed()) {
                        mDiskLruCache.close();
                        mDiskLruCache = null;
                        Log.d(TAG, "Disk cache closed");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "close - " + e);
                }
            }
        }
    }

    /**
     * Get All Cache Size
     * 
     * @return cache size in kilobytes
     */
    public long getCacheSize() {
        long memorySize = 0;
        long diskLruCacheSize = 0;
        if (null != mMemoCache) {
            memorySize = mMemoCache.size();
        }
        synchronized (mDiskCacheLock) {
            if (null != mDiskLruCache) {
                diskLruCacheSize = mDiskLruCache.size();
            }
        }
        return memorySize + diskLruCacheSize;
    }
}
