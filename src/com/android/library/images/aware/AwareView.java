package com.android.library.images.aware;

import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public abstract class AwareView implements ImageAware {

	private static final String TAG = "AwareView";

	protected WeakReference<ImageView> mRefView;

	public AwareView(ImageView imageView) {
		mRefView = new WeakReference<ImageView>(imageView);
	}

	@Override
	public int getWidth() {
		if (null != mRefView && null != mRefView.get()) {

			final ImageView imageView = mRefView.get();

			ViewGroup.LayoutParams params = imageView.getLayoutParams();

			int width = 0;

			if (null != params
					&& params.width != ViewGroup.LayoutParams.WRAP_CONTENT) {
				width = imageView.getWidth();// Get actual image width
			}
			if (width == 0 && null != params) {
				width = params.width;// Get layout width parameter
			}
		}
		return 0;
	}

	@Override
	public int getHeight() {
		if (null != mRefView && null != mRefView.get()) {

			final ImageView imageView = mRefView.get();

			ViewGroup.LayoutParams params = imageView.getLayoutParams();

			int height = 0;

			if (null != params
					&& params.width != ViewGroup.LayoutParams.WRAP_CONTENT) {
				height = imageView.getHeight();// Get actual image height
			}
			if (height == 0 && null != params) {
				height = params.height;// Get layout height parameter
			}
		}
		return 0;
	}

	@Override
	public View getWrappedView() {
		if (null != mRefView && null != mRefView.get()) {
			return mRefView.get();
		}
		return null;
	}

	@Override
	public boolean isCollected() {
		if (null != mRefView && null != mRefView.get()) {
			return false;
		}
		return true;
	}

	@Override
	public int getId() {
		if (null != mRefView && null != mRefView.get()) {
			return mRefView.get().hashCode();
		}
		return super.hashCode();
	}

	@Override
	public boolean setImageDrawable(final Drawable drawable) {
	    
	     if(null==mRefView||null==mRefView.get()){
	            return false;
	       }
	    final ImageView imageView=mRefView.get();
	    new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                setImageDrawableInto(drawable, imageView);
            }
        });
		return true;
	}

	@Override
	public boolean setImageBitmap(final Bitmap bitmap) {
	    
	    if(null==mRefView||null==mRefView.get()){
	        return false;
	    }
	    final ImageView imageView=mRefView.get();
	    new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                setImageBitmapInto(bitmap, imageView);
            }
        });
		return false;
	}

	/**
	 * Should set drawable into incoming view. Incoming view is guaranteed not
	 * null.<br />
	 * This method is called on UI thread.
	 */
	protected abstract void setImageDrawableInto(Drawable drawable, View view);

	/**
	 * Should set Bitmap into incoming view. Incoming view is guaranteed not
	 * null.< br />
	 * This method is called on UI thread.
	 */
	protected abstract void setImageBitmapInto(Bitmap bitmap, View view);

	public static final String WARN_CANT_SET_DRAWABLE = "Can't set a drawable into view. You should call ImageLoader on UI thread for it.";
	public static final String WARN_CANT_SET_BITMAP = "Can't set a bitmap into view. You should call ImageLoader on UI thread for it.";

}
