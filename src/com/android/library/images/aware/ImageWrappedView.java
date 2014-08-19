package com.android.library.images.aware;

import java.lang.reflect.Field;

import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

public class ImageWrappedView extends AwareView {

	private static final String TAG = "ImageWrappedView";

	public ImageWrappedView(ImageView imageView) {
		super(imageView);
	}

	@Override
	public int getWidth() {
		int width = super.getWidth();
		if (width <= 0) {
			ImageView imageView = (ImageView) mRefView.get();
			if (imageView != null) {
				width = getImageViewFieldValue(imageView, "mMaxWidth"); // Check
																		// maxWidth
																		// parameter
			}
		}
		return width;
	}

	@Override
	public int getHeight() {
		int height = super.getHeight();
		if (height <= 0) {
			ImageView imageView = (ImageView) mRefView.get();
			if (imageView != null) {
				height = getImageViewFieldValue(imageView, "mMaxHeight"); // Check
																			// maxHeight
																			// parameter
			}
		}
		return height;
	}

	private static int getImageViewFieldValue(Object object, String fieldName) {
		int value = 0;
		try {
			Field field = ImageView.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			int fieldValue = (Integer) field.get(object);
			if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
				value = fieldValue;
			}
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
		return value;
	}

	@Override
	protected void setImageDrawableInto(Drawable drawable, View view) {
		// TODO Auto-generated method stub
		((ImageView) view).setImageDrawable(drawable);
		if (drawable instanceof AnimationDrawable) {
			((AnimationDrawable)drawable).start();
		}
	}

	@Override
	protected void setImageBitmapInto(Bitmap bitmap, View view) {
		((ImageView) view).setImageBitmap(bitmap);
	}
}
