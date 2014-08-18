package com.android.library.images.aware;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;

public interface ImageAware {
    
    int getWidth();
    
    int getHeight();
    
    View getWrappedView();
    
    boolean isCollected();
    
    int getId();
    
    boolean setImageDrawable(Drawable drawable);
    
    boolean setImageBitmap(Bitmap bitmap);
    
}
