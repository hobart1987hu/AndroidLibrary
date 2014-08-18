package com.android.library.inf;

import android.graphics.drawable.BitmapDrawable;
import android.view.View;

public interface IImageLoadCallback {

    void onLoadingStarted(Object data, View view);

    void onLoadingFailed(Object data, View view, String reason);

    void onLoadingComplete(Object data, View view, BitmapDrawable loadedImage);

    void onLoadingCancelled(Object data, View view);
    
    void publishProgress(int totalSize, int progress);
}
