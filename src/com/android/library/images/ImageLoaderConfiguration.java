package com.android.library.images;

import android.content.Context;

import com.android.library.cache.ImageCache;
import com.android.library.cache.ImageCache.ImageCacheParams;

public class ImageLoaderConfiguration {

    // Init cache and add some configuration
    public ImageCache mImageCache;

    public Context    mContext;

    public ImageLoaderConfiguration(Builder builder, Context context){
        mContext = context;
        ImageCacheParams params = new ImageCacheParams(context, builder.mDiskCacheName);
        params.mDiskCacheSize = builder.mDiskCacheSize;
        params.mMemoCacheSize = builder.mMemoCacheSize;
        mImageCache = new ImageCache(params);
    }

    public static class Builder {

        private Context            mContext;

        public static final String DISK_CACHE_NAME           = "image";

        public String              mDiskCacheName            = DISK_CACHE_NAME;

        public static final int    DEFAULT_MEMORY_CACHE_SIZE = 5 * 1024 * 1024;          // 5M
        public static final int    DEFAULT_DISK_CACHE_SIZE   = 10 * 1024 * 1024;         // 10M

        public int                 mMemoCacheSize            = DEFAULT_MEMORY_CACHE_SIZE;
        public int                 mDiskCacheSize            = DEFAULT_DISK_CACHE_SIZE;

        public Builder(Context context){
            mContext = context;
        }

        public Builder setMemoCacheSize(float percent) {
            if (percent < 0.01f || percent > 0.8f) {
                throw new IllegalArgumentException("setMemCacheSizePercent - percent must be "
                                                   + "between 0.01 and 0.8 (inclusive)");
            }
            mMemoCacheSize = Math.round(percent * Runtime.getRuntime().maxMemory() / 1024);

            return this;
        }

        public Builder setDiskCacheSize(float diskCacheSize) {
            mDiskCacheSize = (int)diskCacheSize;
            return this;
        }

        public ImageLoaderConfiguration build() {
            return new ImageLoaderConfiguration(this, mContext);
        }
    }
}
