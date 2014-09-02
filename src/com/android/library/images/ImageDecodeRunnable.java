package com.android.library.images;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.android.library.images.aware.ImageAware;
import com.android.library.inf.IImageLoadCallback;

public class ImageDecodeRunnable implements Runnable {

    private static final String            TAG = "ImageDecodeRunnable";

    private final InputStream              mBitmapStream;

    private final ImageLoadInfo            mLoadInfo;

    private final ImageAware         mImageWrappedView;

    private final ImageLoaderConfiguration mLoaderConfig;

    private String                         mObject;

    public ImageDecodeRunnable(ImageLoadInfo info, InputStream inputStream){
        mLoadInfo = info;
        mBitmapStream = inputStream;
        mImageWrappedView = mLoadInfo.mWrappedView;
        mLoaderConfig = mLoadInfo.mLoaderConfiguration;
        mObject = mLoadInfo.mObject;
    }

    @Override
    public void run() {

        final IImageLoadCallback callback = mLoadInfo.mCallback;

        if (null == mBitmapStream) {
            failedError("start to decode bitmap found bitmap is null...", callback);
            return;
        }
        FileInputStream fileInputStream = (FileInputStream)mBitmapStream;

        FileDescriptor fileDescriptor = null;
        ;
        try {
            fileDescriptor = fileInputStream.getFD();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (null == fileDescriptor) {
            failedError("start to decode bitmap get file description is null...", callback);
            return;
        }

        final int targetW = mImageWrappedView.getWidth();
        final int targetH = mImageWrappedView.getHeight();

        Bitmap bitmap = null;

        if (fileDescriptor != null) {
            bitmap = decodeSampledBitmapFromDescriptor(fileDescriptor, targetW, targetH);
        }

        if (null == bitmap) {
            failedError("decodeSampledBitmapFromDescriptor get bitmap is null...", callback);
            return;
        }

        Log.d(TAG, "start to display....");

        if (null == callback) {

            Log.d(TAG, "call back is null...");

            setBitmap(bitmap, mImageWrappedView);
            return;
        }

        if (mImageWrappedView.isCollected()) {

            Log.d(TAG, "imageView is collected..");

            callback.onLoadingCancelled(bitmap, mImageWrappedView.getWrappedView());
        }

        callback.onLoadingComplete(mObject, mImageWrappedView.getWrappedView(), bitmap);
        setBitmap(bitmap, mImageWrappedView);

        Log.d(TAG, "display finished..");
        //
        //
        //
        // mLoaderConfig.mImageCache.getMemoCache().put(mObject, bitmap);
        //
        // DisplayRunnable r = new DisplayRunnable(mLoadInfo, bitmap);
        //
        // mHandler.post(r);
    }

    private void setBitmap(final Bitmap bitmap, final ImageAware wrappedView) {

        Log.d(TAG, "setBitmap....");

        mLoadInfo.mDisplayConfig.displayer.display(bitmap, wrappedView);

        // if (null == bitmap) {
        // final Bitmap failedBitmap = mDisplayConfiguration.FailedBitmap;
        // wrappedView.setImageBitmap(failedBitmap == null ? null : failedBitmap);
        // wrappedView.setImageDrawable(null);
        // } else {
        // // TODO
        // // TransitionDrawable drawable=new TransitionDrawable(layers);
        // // wrappedView.setImageDrawable(drawable);
        // wrappedView.setImageBitmap(bitmap);
        // }
    }

    /**
     * Decode and sample down a bitmap from a file input stream to the requested width and height.
     * 
     * @param fileDescriptor The file descriptor to read from
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @param cache The ImageCache used to find candidate bitmaps for use with inBitmap
     * @return A bitmap sampled down from the original with the same aspect ratio and dimensions that are equal to or
     * greater than the requested width and height
     */
    public static Bitmap decodeSampledBitmapFromDescriptor(FileDescriptor fileDescriptor, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // BEGIN_INCLUDE (calculate_sample_size)
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger inSampleSize).

            long totalPixels = width * height / inSampleSize;

            // Anything more than 2x the requested pixels we'll sample down further
            final long totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels > totalReqPixelsCap) {
                inSampleSize *= 2;
                totalPixels /= 2;
            }
        }
        return inSampleSize;
        // END_INCLUDE (calculate_sample_size)
    }

    private void failedError(String errorInfo, IImageLoadCallback callback) {
        if (null != callback) {
            callback.onLoadingFailed(mObject, mImageWrappedView.getWrappedView(), errorInfo);
        } else {
            Log.d(TAG, "decode bitmap error:" + errorInfo);
        }
    }

}
