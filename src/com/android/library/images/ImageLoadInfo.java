package com.android.library.images;

import java.util.concurrent.locks.ReentrantLock;

import com.android.library.images.aware.ImageAware;
import com.android.library.inf.IImageLoadCallback;

public class ImageLoadInfo {

    public ImageAware         mWrappedView;
    public String                   mObject;
    public DisplayConfiguration     mDisplayConfig;
    public IImageLoadCallback       mCallback;
    public ImageLoaderConfiguration mLoaderConfiguration;
    public ReentrantLock loadFromUriLock;
    
    public ImageLoadInfo(ImageLoaderConfiguration loaderConfiguration,ImageAware wrappedView, String object,
                         DisplayConfiguration displayConfiguration, ReentrantLock lock,IImageLoadCallback callback){
        mLoaderConfiguration=loaderConfiguration;
        mWrappedView = wrappedView;
        mObject =object;
        mDisplayConfig = displayConfiguration;
        mCallback = callback;
        loadFromUriLock=lock;
    }
}
