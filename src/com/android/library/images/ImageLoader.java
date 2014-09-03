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

    // TODO
    public void displayImage(ImageWrappedView imageView, String object, DisplayConfiguration displayConfiguration,
                             IImageLoadCallback callback) {
        checkConfiguration();
        
        if(null==callback){
            callback=emptyListener;
        }
        
        if (TextUtils.isEmpty(object)) {
            mLoaderWorker.cancelDisplayTaskFor(imageView);
            callback.onLoadingStarted(object, imageView.getWrappedView());
            imageView.setImageDrawable(null);
            callback.onLoadingComplete(object, imageView.getWrappedView(), null);
            return;
        }
        callback.onLoadingStarted(object, imageView.getWrappedView());
        //get bitmap from memory  cache 
        final Bitmap bitmap = mLoaderConfiguration.mImageCache.getBitmapFromMemoCache(object);
        final ImageLoadInfo info = new ImageLoadInfo(mLoaderConfiguration, imageView, object,
                                                     displayConfiguration, mLoaderWorker.getLockForUri(object),
                                                     callback);
        
        mLoaderWorker.prepareDisplayTaskFor(imageView, object);
        
        if (null != bitmap) {
            DisplayRunnable task =new DisplayRunnable(mLoaderWorker,info, bitmap);
            mLoaderWorker.submit(task);
        } else {
            // start to load bitmap from disk cache or download ..
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
    
    private final IImageLoadCallback emptyListener =new IImageLoadCallback() {
        
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
        public void onLoadingComplete(Object data, View view, Bitmap loadedBitmap) {
            
        }
        
        @Override
        public void onLoadingCancelled(Object data, View view) {
            
        }
    };
    

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
