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
                final int size = getBitmapSize(value) / 1024;
                return size > 0 ? size : 1;
            }

            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                super.entryRemoved(evicted, key, oldValue, newValue);
            }
        };
        initDiskCache();
    }

    public DiskLruCache getDiskCache() {
        return mDiskLruCache;
    }

    public LruCache<String, Bitmap> getMemoCache() {
        return mMemoCache;
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
                    
                    Log.d(TAG, "disk cache path:"+diskCacheDir.getAbsolutePath());
                    
                    final long usableSpace =FileUtils.getUsableSpace(diskCacheDir);
                    
                    Log.d(TAG, "usableSpace:"+usableSpace);
                    
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
                final String key = hashKeyForDisk(data);
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
        final String key = hashKeyForDisk(data);
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

    public static String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    private static String bytesToHexString(byte[] bytes) {
        // http://stackoverflow.com/questions/332079
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    private int getBitmapSize(Bitmap value) {

        if (VersionUtils.hasKitKat()) {
            return value.getAllocationByteCount();
        }
        if (VersionUtils.hasHoneycombMR1()) {
            return value.getByteCount();
        }
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
}
