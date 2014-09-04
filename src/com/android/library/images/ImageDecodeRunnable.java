package com.android.library.images;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;

import com.android.library.images.aware.ImageAware;
import com.android.library.inf.IImageLoadCallback;

public class ImageDecodeRunnable implements Runnable {

    private static final String      TAG = "ImageDecodeRunnable";

    private final InputStream        mBitmapStream;

    private final ImageLoadInfo      mLoadInfo;

    private final ImageAware         mImageWrappedView;

    private final String             mImageUrl;

    private final IImageLoadCallback callback;

    private final ImageLoaderWorker  mWorker;

    private final Handler            mHandler;

    public ImageDecodeRunnable(ImageLoaderWorker worker, ImageLoadInfo info, InputStream inputStream, Handler handler){
        mLoadInfo = info;
        mBitmapStream = inputStream;
        mImageWrappedView = mLoadInfo.mWrappedView;
        mImageUrl = mLoadInfo.mObject;
        callback = mLoadInfo.mCallback;
        mWorker = worker;
        mHandler = handler;
    }

    @Override
    public void run() {

        if (null == mBitmapStream) {
            Log.d(TAG, "start to decode bitmap ,get bitmap stream is null,cancel worker...");
            fireCancelEvent();
            return;
        }

        FileInputStream fileInputStream = (FileInputStream)mBitmapStream;

        FileDescriptor fileDescriptor = null;
        try {
            fileDescriptor = fileInputStream.getFD();
        } catch (IOException e) {
            Log.d(TAG, "get file descriptor from inputStream get IOException:" + e);
        }

        if (null == fileDescriptor) {
            Log.d(TAG, "start to decode bitmap get file description is null...");
            fireCancelEvent();
            return;
        }

        Bitmap bitmap = null;
        try {

            final int targetW = mImageWrappedView.getWidth();
            final int targetH = mImageWrappedView.getHeight();

            checkTaskNotActual();

            bitmap = decodeSampledBitmapFromDescriptor(fileDescriptor, targetW, targetH);

            if (null == bitmap) {
                Log.d(TAG, "decodeSampledBitmapFromDescriptor , get bitmap is null...");
                fireCancelEvent();
                return;
            }

            checkTaskNotActual();
        } catch (OutOfMemoryError e) {
            Log.d(TAG, "decodeSampledBitmapFromDescriptor ,out of memory...");
            fireCancelEvent();
            return;
        } catch (TaskCancelledException e) {
            fireCancelEvent();
            return;
        }
        
        mLoadInfo.mLoaderConfiguration.mImageCache.addBitmapToCache(mImageUrl, bitmap);

        DisplayRunnable task = new DisplayRunnable(mWorker, mLoadInfo, bitmap);

        runTask(task, mHandler);
    }

    private void fireCancelEvent() {
        if (isTaskInterrupted()) return;
        Runnable r = new Runnable() {

            @Override
            public void run() {
                callback.onLoadingCancelled(mImageUrl, mImageWrappedView.getWrappedView());
            }
        };
        runTask(r, mHandler);
    }

    private boolean isTaskInterrupted() {
        if (Thread.interrupted()) {
            return true;
        }
        return false;
    }

    static void runTask(Runnable r, Handler handler) {
        handler.post(r);
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

    private boolean isViewCollected() {
        if (mImageWrappedView.isCollected()) {
            return true;
        }
        return false;
    }

    private boolean isViewReused() {
        String currentCacheKey = mWorker.getLoadingUriForView(mImageWrappedView);
        // Check whether memory cache key (image URI) for current ImageAware is actual.
        // If ImageAware is reused for another task then current task should be cancelled.
        boolean imageAwareWasReused = !mImageUrl.equals(currentCacheKey);
        if (imageAwareWasReused) {
            return true;
        }
        return false;
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
    public static Bitmap decodeSampledBitmapFromDescriptor(FileDescriptor fileDescriptor, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
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
