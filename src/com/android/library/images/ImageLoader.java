package com.android.library.images;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.android.library.images.aware.ImageWrappedView;
import com.android.library.inf.IImageLoadCallback;
import com.android.library.os.AsyncTask;

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
        displayImage(new ImageWrappedView(imageView), object, null, null);
    }
    public void displayImage(ImageWrappedView imageView, String url, DisplayConfiguration displayConfiguration,
                             IImageLoadCallback callback) {
        checkConfiguration();

        if (null == callback) {
            callback = emptyListener;
        }

        if (TextUtils.isEmpty(url)) {
            mLoaderWorker.cancelDisplayTaskFor(imageView);
            callback.onLoadingStarted(url, imageView.getWrappedView());
            imageView.setImageDrawable(null);
            callback.onLoadingComplete(url, imageView.getWrappedView(), null);
            return;
        }
        mLoaderWorker.prepareDisplayTaskFor(imageView, url);

        callback.onLoadingStarted(url, imageView.getWrappedView());

        // get bitmap from memory cache

        final Bitmap bitmap = mLoaderConfiguration.mImageCache.getBitmapFromMemoCache(url);

        if (null != bitmap && !bitmap.isRecycled()) {

            Log.d(TAG, "start to display image from memory cache ....");

            displayConfiguration.getDisplayer().display(bitmap, imageView);
            callback.onLoadingComplete(url, imageView.getWrappedView(), bitmap);
        } else {

            Log.d(TAG, "new task  to display image  ....");

            final ImageLoadInfo info = new ImageLoadInfo(mLoaderConfiguration, imageView, url, displayConfiguration,
                                                         mLoaderWorker.getLockForUri(url), callback);

            // start to load bitmap from disk cache or download ..
            final ImageLoadeRunnable task = new ImageLoadeRunnable(mLoaderWorker, info,
                                                                   defineHandler(displayConfiguration));
            mLoaderWorker.submit(task);
        }
    }

    private static Handler defineHandler(DisplayConfiguration options) {
        Handler handler = options.getHandler();
        if (handler == null && Looper.myLooper() == Looper.getMainLooper()) {
            handler = new Handler(Looper.getMainLooper());
        }
        return handler;
    }

    private final IImageLoadCallback emptyListener = new IImageLoadCallback() {

                                                       @Override
                                                       public void publishProgress(int totalSize, int progress) {

                                                       }

                                                       @Override
                                                       public void onLoadingStarted(Object data, View view) {

                                                       }

                                                       @Override
                                                       public void onLoadingFailed(Object data, View view, String reason) {

                                                       }

                                                       @Override
                                                       public void onLoadingComplete(Object data, View view,
                                                                                     Bitmap loadedBitmap) {

                                                       }

                                                       @Override
                                                       public void onLoadingCancelled(Object data, View view) {

                                                       }
                                                   };

    // TODO
    public void onResume() {
        mLoaderWorker.onResume();
    }

    public void onPause() {
        mLoaderWorker.onPause();
    }

    public void onStop() {
        mLoaderWorker.stop();
        flushCache();
    }

    public void onDestory() {
        onStop();
    }

    public void finish() {
        onStop();
        closeCache();
        clear();
        mLoaderConfiguration = null;
        mLoaderWorker = null;
    }

    public void clear() {
        clearCache();
    }

    public long getAllCacheSize() {
        return mLoaderConfiguration.mImageCache.getCacheSize();
    }

    private static final int MESSAGE_CLEAR           = 0;
    private static final int MESSAGE_INIT_DISK_CACHE = 1;
    private static final int MESSAGE_FLUSH           = 2;
    private static final int MESSAGE_CLOSE           = 3;

    protected class CacheAsyncTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            switch ((Integer)params[0]) {
                case MESSAGE_CLEAR:
                    mLoaderConfiguration.mImageCache.clearCache();
                    break;
                case MESSAGE_FLUSH:
                    mLoaderConfiguration.mImageCache.flush();
                    break;
                case MESSAGE_CLOSE:
                    mLoaderConfiguration.mImageCache.close();
                    break;
            }
            return null;
        }
    }

    private void clearCache() {
        new CacheAsyncTask().execute(MESSAGE_CLEAR);
    }

    private void flushCache() {
        new CacheAsyncTask().execute(MESSAGE_FLUSH);
    }

    private void closeCache() {
        new CacheAsyncTask().execute(MESSAGE_CLOSE);
    }
}
