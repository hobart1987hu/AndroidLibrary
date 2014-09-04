package com.android.library.images;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;

import com.android.library.images.display.BitmapDisplayer;
import com.android.library.images.display.SimpleBitmapDisplayer;

public class DisplayConfiguration {

    public Bitmap  mLoadingBitmap;

    public boolean isFadeIn;

    public Bitmap  FailedBitmap;

    public Context mContext;

    public Handler mHandler;

    public BitmapDisplayer displayer;
    
    public DisplayConfiguration(Builder builder){
        this.FailedBitmap = builder.downloadFailedBitmap;
        this.isFadeIn = builder.isFadeIn;
        this.mLoadingBitmap = builder.mLoadingBitmap;
        this.mContext = builder.mContext;
        this.mHandler = builder.mHandler;
        this.displayer=createBitmapDisplayer();
    }

    public static class Builder {

        public Bitmap  mLoadingBitmap;

        public boolean isFadeIn;

        public Bitmap  downloadFailedBitmap;

        public Context mContext;

        public Handler mHandler;

        public Builder(Context context, Handler handler){
            this.mContext = context;
            this.mHandler = handler;
        }

        public Builder setIsFadeIn(boolean isFadeIn) {
            this.isFadeIn = isFadeIn;
            return this;
        }

        public Builder setLoadingBitmap(Bitmap bitmap) {
            this.mLoadingBitmap = bitmap;
            return this;
        }

        public Builder setLoadingBitmap(int resId) {
            this.mLoadingBitmap = BitmapFactory.decodeResource(mContext.getResources(), resId);
            return this;
        }

        public Builder setFailedBitmap(Bitmap bitmap) {
            this.downloadFailedBitmap = bitmap;
            return this;
        }

        public Builder setFailedBitmap(int resId) {
            this.downloadFailedBitmap = BitmapFactory.decodeResource(mContext.getResources(), resId);
            return this;
        }

        public DisplayConfiguration builder() {

            return new DisplayConfiguration(this);
        }
    }
    
    public Handler getHandler(){
        return mHandler;
    }
    
    public BitmapDisplayer getDisplayer(){
        return displayer;
    }
    
    /** Creates default implementation of {@link BitmapDisplayer} - {@link SimpleBitmapDisplayer} */
    public static BitmapDisplayer createBitmapDisplayer() {
        return new SimpleBitmapDisplayer();
    }
}
