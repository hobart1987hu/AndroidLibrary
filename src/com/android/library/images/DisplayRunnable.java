package com.android.library.images;

import android.graphics.Bitmap;
import android.util.Log;

import com.android.library.images.aware.ImageAware;
import com.android.library.inf.IImageLoadCallback;

public class DisplayRunnable implements Runnable {

    private static final String        TAG = "DisplayRunnable";

    private final Bitmap               mBitmap;

    // TODO
    private final DisplayConfiguration mDisplayConfiguration;

    private final IImageLoadCallback   mCallback;

    private final ImageAware     mImageWrappedView;

    private final String               mObject;

    public DisplayRunnable(ImageLoadInfo info, Bitmap bitmap){
        mBitmap = bitmap;
        mDisplayConfiguration = info.mDisplayConfig;
        mCallback = info.mCallback;
        mImageWrappedView = info.mWrappedView;
        mObject = info.mObject;
    }

    @Override
    public void run() {

        Log.d(TAG, "start to display....");

        final IImageLoadCallback callback = mCallback;

        if (null == callback) {

            Log.d(TAG, "call back is null...");

            setBitmap(mBitmap, mImageWrappedView);
            return;
        }

        if (mImageWrappedView.isCollected()) {

            Log.d(TAG, "imageView is collected..");

            mCallback.onLoadingCancelled(mBitmap, mImageWrappedView.getWrappedView());
        }

        callback.onLoadingComplete(mObject, mImageWrappedView.getWrappedView(), mBitmap);
        setBitmap(mBitmap, mImageWrappedView);

        Log.d(TAG, "display finished..");
    }

    private void setBitmap(final Bitmap bitmap, final ImageAware wrappedView) {
        
        Log.d(TAG, "setBitmap....");
        
        mDisplayConfiguration.displayer.display(bitmap, wrappedView);
        
//        if (null == bitmap) {
//            final Bitmap failedBitmap = mDisplayConfiguration.FailedBitmap;
//            wrappedView.setImageBitmap(failedBitmap == null ? null : failedBitmap);
//            wrappedView.setImageDrawable(null);
//        } else {
//            // TODO
//            // TransitionDrawable drawable=new TransitionDrawable(layers);
//            // wrappedView.setImageDrawable(drawable);
//            wrappedView.setImageBitmap(bitmap);
//        }
    }
}
