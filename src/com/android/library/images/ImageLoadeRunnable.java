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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.android.library.cache.DiskLruCache;
import com.android.library.images.aware.ImageAware;
import com.android.library.inf.IImageLoadCallback;
import com.android.library.util.Utils;

public class ImageLoadeRunnable implements Runnable {

    private static final String            TAG              = "ImageLoadeRunnable";

    private static final int               IO_BUFFER_SIZE   = 8 * 1024;

    private static final int               DISK_CACHE_INDEX = 0;

    private final String                   imageUrl;

    private final ImageLoadInfo            mLoadInfo;

    private final Handler                  mHandler;

    private final ImageLoaderWorker        mWorker;

    private final ImageLoaderConfiguration mLoaderConfig;

    private final ImageAware               wrappedView;

    private final IImageLoadCallback       mListener;

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

        Bitmap bitmap;

        try {

            checkTaskNotActual();

            bitmap = mLoaderConfig.mImageCache.getBitmapFromMemoCache(imageUrl);

            if (null == bitmap || bitmap.isRecycled()) {

                bitmap = tryLoadBitmap();

                if (null == bitmap) {// listener callback already was fired
                    return;
                }

                checkTaskNotActual();
                checkTaskInterrupted();

                mLoaderConfig.mImageCache.addBitmapToCache(imageUrl, bitmap);
            }

            checkTaskNotActual();
            checkTaskInterrupted();

        } catch (TaskCancelledException e) {
            fireCancelEvent();
            return;
        } finally {
            loadFromUriLock.unlock();
        }
        display(bitmap);
    }

    private void display(Bitmap bitmap) {
        DisplayRunnable task = new DisplayRunnable(mWorker, mLoadInfo, bitmap);
        runTask(task, mHandler);
    }

    private Bitmap tryLoadBitmap() throws TaskCancelledException {
        Bitmap bitmap = null;
        try {
            checkTaskNotActual();
            // load from diskcache
            bitmap = mLoaderConfig.mImageCache.getBitmapFromDiskCache(imageUrl);

            if (bitmap == null || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {

                checkTaskNotActual();

                bitmap = loadBitmapFromNetWork();

                if (bitmap == null || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
                    fireFailEvent("loadBitmFromNetWork error");
                }
            }
            checkTaskNotActual();
            checkTaskInterrupted();
        } catch (IllegalStateException e) {
            Log.d(TAG, "network error --" + e);
            fireFailEvent("network error");
        } catch (TaskCancelledException e) {
            throw e;
        } catch (OutOfMemoryError e) {
            Log.d(TAG, "out of memory --" + e);
            fireFailEvent("out of memory");
        } catch (Throwable e) {
            Log.d(TAG, "unknow --" + e);
            fireFailEvent("unknow");
        }
        return bitmap;
    }

    private Bitmap loadBitmapFromNetWork() {

        final String key = Utils.hashKeyForDisk(imageUrl);
        InputStream inputStream = null;
        DiskLruCache.Snapshot snapshot;
        DiskLruCache diskCache = mLoaderConfig.mImageCache.getDiskCache();

        Bitmap bitmap = null;

        if (diskCache != null) {
            try {
                snapshot = diskCache.get(key);
                if (snapshot == null) {
                    Log.d(TAG, "loadBitmapFromNetWork, not found in  cache, downloading...");
                    DiskLruCache.Editor editor = diskCache.edit(key);
                    if (editor != null) {
                        if (downloadUrlToStream(imageUrl, editor.newOutputStream(DISK_CACHE_INDEX))) {
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
                final int targetW = wrappedView.getWidth();
                final int targetH = wrappedView.getHeight();

                if (inputStream != null) {
                    bitmap = decodeSampledBitmapFromDescriptor(inputStream, targetW, targetH);
                }
            } catch (IOException e) {
                Log.e(TAG, "processBitmap IOException- " + e);
                fireFailEvent("loadBitmapFromNetWork IOException");
            } catch (IllegalStateException e) {
                Log.e(TAG, "processBitmap IllegalStateException- " + e);
                fireFailEvent("loadBitmapFromNetWork IllegalStateException");
            } catch (OutOfMemoryError e) {
                Log.e(TAG, "processBitmap  OutOfMemoryError- " + e);
                fireFailEvent("loadBitmapFromNetWork OutOfMemoryError");
            } catch (Throwable e) {
                Log.e(TAG, "processBitmap  Throwable- " + e);
                fireFailEvent("unknow");
            }
        }
        if (null != inputStream) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
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

    private void fireFailEvent(final String reason) {
        if (isTaskInterrupted()) return;
        Runnable r = new Runnable() {

            @Override
            public void run() {
                mListener.onLoadingFailed(imageUrl, wrappedView.getWrappedView(), reason);
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
     * Decode and sample down a bitmap from a file input stream to the requested width and height.
     * 
     * @param fileDescriptor The file descriptor to read from
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @param cache The ImageCache used to find candidate bitmaps for use with inBitmap
     * @return A bitmap sampled down from the original with the same aspect ratio and dimensions that are equal to or
     * greater than the requested width and height
     */
    public static Bitmap decodeSampledBitmapFromDescriptor(InputStream inputStream, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeStream(inputStream, null, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeStream(inputStream, null, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // BEGIN_INCLUDE (calculate_sample_size)
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger inSampleSize).

            long totalPixels = width * height / inSampleSize;

            // Anything more than 2x the requested pixels we'll sample down further
            final long totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels > totalReqPixelsCap) {
                inSampleSize *= 2;
                totalPixels /= 2;
            }
        }
        return inSampleSize;
        // END_INCLUDE (calculate_sample_size)
    }

    /**
     * Exceptions for case when task is cancelled (thread is interrupted, image view is reused for another task, view is
     * collected by GC).
     */
    class TaskCancelledException extends Exception {
    }
}
