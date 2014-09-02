package com.android.library.inf;

import android.graphics.Bitmap;
import android.view.View;

public interface IImageLoadCallback {

    void onLoadingStarted(Object data, View view);

    void onLoadingFailed(Object data, View view, String reason);

    void onLoadingComplete(Object data, View view,Bitmap loadedBitmap);

    void onLoadingCancelled(Object data, View view);
    
    void publishProgress(int totalSize, int progress);
}
