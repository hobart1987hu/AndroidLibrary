package com.android.library.images.display;

import android.graphics.Bitmap;

import com.android.library.images.aware.ImageAware;


public final class SimpleBitmapDisplayer implements BitmapDisplayer {
    @Override
    public void display(Bitmap bitmap, ImageAware imageAware) {
        imageAware.setImageBitmap(bitmap);
    }

}
