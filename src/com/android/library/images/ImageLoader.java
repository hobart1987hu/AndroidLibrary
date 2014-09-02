package com.android.library.images;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import com.android.library.images.aware.ImageWrappedView;
import com.android.library.inf.IImageLoadCallback;

public class ImageLoader {

    private static final String      TAG = "ImageLoader";

    private ImageLoaderConfiguration mLoaderConfiguration;

    private ImageLoaderWorker        mLoaderWorker;

    // Single instance lazy mode
    static class SingletonHolder {

        static ImageLoader instance = new ImageLoader();
    }

    public static ImageLoader getInstance() {
        return SingletonHolder.instance;
    }

    // init some configures
    public synchronized void init(ImageLoaderConfiguration configuration) {
        if (null == mLoaderConfiguration) {
            mLoaderConfiguration = configuration;
            mLoaderWorker = new ImageLoaderWorker(configuration);
        } else {
            Log.d(TAG, "init image loader configuration....");
        }
    }

    public synchronized void checkConfiguration() {
        if (null == mLoaderConfiguration) {
            throw new IllegalStateException("null mLoaderConfiguration...");
        }
    }

    public void displayImage(ImageView imageView, String object) {
        displayImage(imageView, object, null, null);
    }

    // TODO
    public void displayImage(ImageView imageView, String object, DisplayConfiguration displayConfiguration,
                             IImageLoadCallback callback) {
        checkConfiguration();
        if (null != callback) {
            callback.onLoadingStarted(object, imageView);
        }
        final Bitmap bitmap = mLoaderConfiguration.mImageCache.getBitmapFromMemoCache(object);
        if (null != bitmap) {
            new ImageWrappedView(imageView).setImageBitmap(bitmap);
            if (null != callback) {
                callback.onLoadingComplete(object, imageView, bitmap);
            }
            return;
        } else {
            // start to load bitmap from disk cache or download ..
            final ImageWrappedView wrappedView = new ImageWrappedView(imageView);
            final ImageLoadInfo info = new ImageLoadInfo(mLoaderConfiguration, wrappedView, object,
                                                         displayConfiguration, mLoaderWorker.getLockForUri(object),
                                                         callback);
            final ImageLoadeRunnable task = new ImageLoadeRunnable(mLoaderWorker, info,
                                                                   defineHandler(displayConfiguration));
            mLoaderWorker.submit(task);
        }
    }

    private static Handler defineHandler(DisplayConfiguration displayConfiguration) {
        Handler handler = displayConfiguration.mHandler;
        if (handler == null && Looper.myLooper() == Looper.getMainLooper()) {
            handler = new Handler(Looper.getMainLooper());
        }
        return handler;
    }

    // TODO
    public void onResume() {
        mLoaderWorker.onResume();
    }

    // TODO
    public void onPause() {
        mLoaderWorker.onPause();
    }

    // TODO
    public void onDestory() {

    }

    // TODO
    public void clear() {

    }

    // TODO
    public long getAllCacheSize() {
        return 0;
    }
}
