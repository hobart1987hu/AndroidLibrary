package com.android.library.images;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.android.library.cache.DiskLruCache;

public class ImageLoadeRunnable implements Runnable {

    private static final String      TAG              = "ImageLoadeRunnable";

    private static final int         IO_BUFFER_SIZE   = 8 * 1024;

    private static final int         DISK_CACHE_INDEX = 0;

    private Object                   mObject;

    private final ImageLoadInfo      mLoadInfo;

    private final Handler            mHandler;

    private final ImageLoaderWorker  mWorker;

    private ImageLoaderConfiguration mLoaderConfig;

    public ImageLoadeRunnable(ImageLoaderWorker worker, ImageLoadInfo info, Handler handler){
        mWorker = worker;
        mHandler = handler;
        mObject = info.mObject;
        mLoadInfo = info;
        mLoaderConfig = mLoadInfo.mLoaderConfiguration;
    }

    @Override
    public void run() {

        waitIfPaused();

        ReentrantLock loadFromUriLock = mLoadInfo.loadFromUriLock;

        loadFromUriLock.lock();

        DiskLruCache diskCache = mLoaderConfig.mImageCache.getDiskCache();

        if (null == diskCache) {

            Log.d(TAG, "disk cache is null...");

            return;
        }

        DiskLruCache.Snapshot snapshot = null;
        final String key = hashKeyForDisk((String)mObject);
        InputStream inputStream = null;

        try {
            snapshot = diskCache.get(key);
        } catch (IOException e1) {
            Log.d(TAG, "get snapshot from diskcache IOException : " + e1.getMessage());
            e1.printStackTrace();
        }
        DiskLruCache.Editor editor = null;

        if (null == snapshot) {
            try {
                editor = diskCache.edit(key);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "diskCache edit get error:" + e.getMessage());
            }
        }
        if (null != editor) {
            try {
                if (downloadUrlToStream((String)mObject, editor.newOutputStream(DISK_CACHE_INDEX))) {
                    editor.commit();
                } else {
                    editor.abort();
                }
            } catch (IOException e) {
                Log.d(TAG, "downloadUrlToStream error:" + e.getMessage());
                e.printStackTrace();
            }
        }
        try {
            snapshot = diskCache.get(key);
        } catch (IOException e1) {
            Log.d(TAG, "diskCache get data agagin IOException  error:" + e1.getMessage());
            e1.printStackTrace();
        }
        if (snapshot != null) {
            inputStream = snapshot.getInputStream(DISK_CACHE_INDEX);
        }

        loadFromUriLock.unlock();

        Log.d(TAG, "start to decode bitmap....");

        if (null == inputStream) {
            // TODO
            Log.d(TAG, "get inputStrean is null...");
            return;
        }

        ImageDecodeRunnable decode = new ImageDecodeRunnable(mLoadInfo, inputStream);

        runTask(decode, mHandler);

        // goToDecode(inputStream);
    }

    static void runTask(Runnable r, Handler handler) {
        handler.post(r);
    }

    // public void goToDecode(InputStream inputStream) {
    // if (null == mHandler || mHandler.getLooper() != Looper.getMainLooper()) {
    // Log.d(TAG, "sart to decode bitmap is not ui Thread ...return");
    // return;
    // }
    //
    // mHandler.post(decode);
    // }

    public ImageLoadInfo getLoadInfo() {
        return mLoadInfo;
    }

    public Object getObject() {
        return mObject;
    }

    private void fireProgressEvent() {

    }

    private void fireFailEvent(String type, final Throwable failCause) {

    }

    private void fireCancelEvent() {
        if (isTaskInterrupted()) return;
        // cacele callback
    }

    private boolean isTaskInterrupted() {
        if (Thread.interrupted()) {
            return true;
        }
        return false;
    }

    /**
     * Download a bitmap from a URL and write the content to an output stream.
     * 
     * @param urlString The URL to fetch
     * @return true if successful, false otherwise
     */
    public boolean downloadUrlToStream(String urlString, OutputStream outputStream) {
        disableConnectionReuseIfNecessary();
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;

        try {
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection)url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(), IO_BUFFER_SIZE);
            out = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE);

            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            Log.d(TAG, "downloadUrlToStream success...");
            return true;
        } catch (final IOException e) {
            Log.e(TAG, "Error in downloadBitmap - " + e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (final IOException e) {
            }
        }
        return false;
    }

    /**
     * Workaround for bug pre-Froyo, see here for more info:
     * http://android-developers.blogspot.com/2011/09/androids-http-clients.html
     */
    public static void disableConnectionReuseIfNecessary() {
        // HTTP connection reuse which was buggy pre-froyo
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    private boolean waitIfPaused() {

        final AtomicBoolean pause = mWorker.getIsPauseWork();

        if (pause.get()) {
            try {
                mWorker.getPauseLock().wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public Object getData() {
        return mObject;
    }

    /**
     * A hashing method that changes a string (like a URL) into a hash suitable for using as a disk filename.
     */
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

    //
    // try {
    // if (null == snapshot) {
    // try {
    // editor = diskCache.edit(data);
    // } catch (IOException e) {
    // e.printStackTrace();
    // Log.d(TAG, "diskCache edit get error:" + e.getMessage());
    // }
    // if (null != editor) {
    // waitIfPaused();
    // if (downloadUrlToStream(data, editor.newOutputStream(DISK_CACHE_INDEX))) {
    // editor.commit();
    // } else {
    // editor.abort();
    // }
    // }
    // snapshot = diskCache.get(data);
    // }
    // if (snapshot != null) {
    // inputStream = snapshot.getInputStream(DISK_CACHE_INDEX);
    // }
    // } catch (IOException e) {
    // Log.d(TAG, "IOException :" + e);
    // } catch (OutOfMemoryError e) {
    // Log.d(TAG, "OutOfMemoryError :" + e);
    // } catch (Throwable e) {
    // Log.d(TAG, "Throwable :" + e);
    // } finally {
    // loadFromUriLock.unlock();
    // }
}
