package com.android.library.images.display;

import android.graphics.Bitmap;

import com.android.library.images.aware.ImageAware;


public interface BitmapDisplayer {

    void display(Bitmap bitmap, ImageAware imageAware);

}
