package com.android.library.images;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.android.library.cache.DiskLruCache;
import com.android.library.images.aware.ImageAware;
import com.android.library.inf.IImageLoadCallback;
import com.android.library.util.Utils;

public class ImageLoadeRunnable implements Runnable {

    private static final String      TAG              = "ImageLoadeRunnable";

    private static final int         IO_BUFFER_SIZE   = 8 * 1024;

    private static final int         DISK_CACHE_INDEX = 0;

    private String                   imageUrl;

    private final ImageLoadInfo      mLoadInfo;

    private final Handler            mHandler;

    private final ImageLoaderWorker  mWorker;

    private ImageLoaderConfiguration mLoaderConfig;

    private ImageAware               wrappedView;

    private IImageLoadCallback       mListener;

    public ImageLoadeRunnable(ImageLoaderWorker worker, ImageLoadInfo info, Handler handler){
        mWorker = worker;
        mHandler = handler;
        imageUrl = info.mObject;
        mLoadInfo = info;
        mLoaderConfig = mLoadInfo.mLoaderConfiguration;
        wrappedView = mLoadInfo.mWrappedView;
        mListener = info.mCallback;
    }

    @Override
    public void run() {

        if (waitIfPaused()) return;

        ReentrantLock loadFromUriLock = mLoadInfo.loadFromUriLock;

        loadFromUriLock.lock();

        DiskLruCache diskCache = mLoaderConfig.mImageCache.getDiskCache();

        if (null == diskCache) {

            Log.d(TAG, "disk cache is null...");

            return;
        }

        DiskLruCache.Snapshot snapshot = null;
        final String key = Utils.hashKeyForDisk(imageUrl);
        final String url = imageUrl;
        InputStream inputStream = null;

        try {

            checkTaskNotActual();

            snapshot = diskCache.get(key);

            if (snapshot == null) {

                Log.d(TAG, "processBitmap, not found in  cache, downloading...");

                DiskLruCache.Editor editor = diskCache.edit(key);
                if (editor != null) {
                    if (downloadUrlToStream(url, editor.newOutputStream(DISK_CACHE_INDEX))) {
                        editor.commit();
                    } else {
                        editor.abort();
                    }
                }
                snapshot = diskCache.get(key);
            }
            if (snapshot != null) {
                inputStream = snapshot.getInputStream(DISK_CACHE_INDEX);
            }

            checkTaskNotActual();
            checkTaskInterrupted();

        } catch (IOException e) {
            fireCancelEvent();
            Log.e(TAG, "download bitmap  IOException - " + e);
        } catch (IllegalStateException e) {
            fireCancelEvent();
            Log.e(TAG, "download bitmap IllegalStateException - " + e);
        } catch (TaskCancelledException e) {
            fireCancelEvent();
        } finally {
            loadFromUriLock.unlock();
        }

        Log.d(TAG, "start to decode bitmap....");

        if (null == inputStream) {
            Log.d(TAG, "download bitmap get inputStrean is null...");
            fireCancelEvent();
            return;
        }

        ImageDecodeRunnable decode = new ImageDecodeRunnable(mWorker, mLoadInfo, inputStream, mHandler);

        runTask(decode, mHandler);
    }

    private void fireCancelEvent() {
        if (isTaskInterrupted()) return;
        Runnable r = new Runnable() {

            @Override
            public void run() {
                mListener.onLoadingCancelled(imageUrl, wrappedView.getWrappedView());
            }
        };
        runTask(r, mHandler);
    }

    static void runTask(Runnable r, Handler handler) {
        handler.post(r);
    }

    public ImageLoadInfo getLoadInfo() {
        return mLoadInfo;
    }

    public Object getObject() {
        return imageUrl;
    }

    /**
     * @throws TaskCancelledException if task is not actual (target ImageAware is collected by GC or the image URI of
     * this task doesn't match to image URI which is actual for current ImageAware at this moment)
     */
    private void checkTaskNotActual() throws TaskCancelledException {
        checkViewCollected();
        checkViewReused();
    }

    /** @throws TaskCancelledException if target ImageAware is collected */
    private void checkViewCollected() throws TaskCancelledException {
        if (isViewCollected()) {
            throw new TaskCancelledException();
        }
    }

    /** @throws TaskCancelledException if target ImageAware is collected by GC */
    private void checkViewReused() throws TaskCancelledException {
        if (isViewReused()) {
            throw new TaskCancelledException();
        }
    }

    /** @throws TaskCancelledException if current task was interrupted */
    private void checkTaskInterrupted() throws TaskCancelledException {
        if (isTaskInterrupted()) {
            throw new TaskCancelledException();
        }
    }

    /** @return <b>true</b> - if current task was interrupted; <b>false</b> - otherwise */
    private boolean isTaskInterrupted() {
        if (Thread.interrupted()) {
            return true;
        }
        return false;
    }

    private boolean isTaskNotActual() {
        return isViewCollected() || isViewReused();
    }

    private boolean isViewCollected() {
        if (wrappedView.isCollected()) {
            return true;
        }
        return false;
    }

    private boolean isViewReused() {
        String currentCacheKey = mWorker.getLoadingUriForView(wrappedView);
        // Check whether memory cache key (image URI) for current ImageAware is actual.
        // If ImageAware is reused for another task then current task should be cancelled.
        boolean imageAwareWasReused = !imageUrl.equals(currentCacheKey);
        if (imageAwareWasReused) {
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
            synchronized (mWorker.getPauseLock()) {
                if (pause.get()) {
                    try {
                        mWorker.getPauseLock().wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return true;
                    }
                }
            }
        }
        return isTaskNotActual();
    }

    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Exceptions for case when task is cancelled (thread is interrupted, image view is reused for another task, view is
     * collected by GC).
     */
    class TaskCancelledException extends Exception {
    }
}
