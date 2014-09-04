package com.android.library.images;

import android.graphics.Bitmap;
import android.util.Log;

import com.android.library.images.aware.ImageAware;
import com.android.library.inf.IImageLoadCallback;

/**
 * Display Thread
 */
public class DisplayRunnable implements Runnable {

    private static final String        TAG = "DisplayRunnable";

    private final Bitmap               mBitmap;

    private final DisplayConfiguration mDisplayConfiguration;

    private final IImageLoadCallback   mCallback;

    private final ImageAware           mImageWrappedView;

    private final String               mObject;

    private final ImageLoaderWorker    mImageWorker;

    public DisplayRunnable(ImageLoaderWorker worker, ImageLoadInfo info, Bitmap bitmap){
        mBitmap = bitmap;
        mDisplayConfiguration = info.mDisplayConfig;
        mCallback = info.mCallback;
        mImageWrappedView = info.mWrappedView;
        mObject = info.mObject;
        mImageWorker = worker;
    }

    @Override
    public void run() {

        Log.d(TAG, "start to display....");

        if (mImageWrappedView.isCollected()) {
            Log.d(TAG, "imageView is collected..");
            mCallback.onLoadingCancelled(mBitmap, mImageWrappedView.getWrappedView());
        } else if (isViewWasReused()) {
            Log.d(TAG, "imageView is reused..");
            mCallback.onLoadingCancelled(mBitmap, mImageWrappedView.getWrappedView());
        } else {
            mDisplayConfiguration.displayer.display(mBitmap, mImageWrappedView);
            mImageWorker.cancelDisplayTaskFor(mImageWrappedView);
            mCallback.onLoadingComplete(mObject, mImageWrappedView.getWrappedView(), mBitmap);
        }
    }

    /** Checks whether memory cache key (image URI) for current ImageAware is actual */
    private boolean isViewWasReused() {
        String currentCacheKey = mImageWorker.getLoadingUriForView(mImageWrappedView);
        return !mObject.equals(currentCacheKey);
    }
}
