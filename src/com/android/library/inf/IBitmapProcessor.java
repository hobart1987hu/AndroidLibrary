package com.android.library.inf;

import android.graphics.Bitmap;
import android.view.View;

public interface IBitmapProcessor {

    Bitmap processBitmap(Object data, View view, IImageLoadCallback callback);
}
