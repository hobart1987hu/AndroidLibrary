package com.android.library.images.aware;

import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public abstract class AwareView implements ImageAware {

	private static final String TAG = "AwareView";

	protected WeakReference<View> mRefView;

	public AwareView(View view ) {
		mRefView = new WeakReference<View>(view);
	}

	@Override
	public int getWidth() {
		if (null != mRefView && null != mRefView.get()) {

			final View view =mRefView.get();

			ViewGroup.LayoutParams params = view.getLayoutParams();

			int width = 0;

			if (null != params
					&& params.width != ViewGroup.LayoutParams.WRAP_CONTENT) {
				width = view.getWidth();// Get actual image width
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

			final View view = mRefView.get();

			ViewGroup.LayoutParams params = view.getLayoutParams();

			int height = 0;

			if (null != params
					&& params.width != ViewGroup.LayoutParams.WRAP_CONTENT) {
				height = view.getHeight();// Get actual image height
			}
			if (height == 0 && null != params) {
				height = params.height;// Get layout height parameter
			}
		}
		return 0;
	}

	@Override
	public View getWrappedView() {
	    return mRefView.get();
	}

	@Override
	public boolean isCollected() {
	    return mRefView.get() == null;
	}

	@Override
	public int getId() {
        View view = mRefView.get();
        return view == null ? super.hashCode() : view.hashCode();
    }

	@Override
	public boolean setImageDrawable(final Drawable drawable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            View view = mRefView.get();
            if (view != null) {
                setImageDrawableInto(drawable, view);
                return true;
            }
        } else {
            Log.w(TAG, WARN_CANT_SET_DRAWABLE);
        }
        return false;
    }

	@Override
	public boolean setImageBitmap(final Bitmap bitmap) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            View view = mRefView.get();
            if (view != null) {
                setImageBitmapInto(bitmap, view);
                return true;
            }
        } else {
            Log.w(TAG, WARN_CANT_SET_BITMAP);
        }
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
