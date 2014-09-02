package com.android.library.images;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import android.graphics.Bitmap;

public class ImageLoaderWorker {

    private static final int                 CORE_POOL_SIZE  = 10;

    private static final int                 MAX_POOL_SIZE   = 20;

    private static final int                 KEEP_ALIVE_TIME = 1;

    private static final TimeUnit            unit            = TimeUnit.MILLISECONDS;

    private Executor                         taskDistributor;

    private Executor                         taskExecutorForCachedImages;

    private Executor                         taskExecutor;

    private ImageLoaderConfiguration         mLoaderConfiguration;

    private final Map<String, ReentrantLock> uriLocks        = new WeakHashMap<String, ReentrantLock>();

    public ImageLoaderWorker(ImageLoaderConfiguration loaderConfiguration){

        mLoaderConfiguration = loaderConfiguration;

        taskDistributor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, unit,
                                                 new LinkedBlockingDeque<Runnable>());

        taskExecutorForCachedImages = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, unit,
                                                             new LinkedBlockingDeque<Runnable>());

        taskExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, unit,
                                              new LinkedBlockingDeque<Runnable>());
    }

    public void submit(final ImageLoadeRunnable task) {
        taskDistributor.execute(new Runnable() {
            @Override
            public void run() {
                final Bitmap bitmap = mLoaderConfiguration.mImageCache.getBitmapFromDiskCache((String)task.getData());
                initExecutorsIfNeed();
                if (null != bitmap) {
                    taskExecutorForCachedImages.execute(new DisplayRunnable(task.getLoadInfo(), bitmap));
                } else {
                    taskExecutor.execute(task);
                }
            }
        });
    }

    private void initExecutorsIfNeed() {
        if (((ExecutorService)taskExecutor).isShutdown()) {
            taskExecutor = createExecutor();
        }
        if (((ExecutorService)taskExecutorForCachedImages).isShutdown()) {
            taskExecutorForCachedImages = createExecutor();
        }
    }

    private Executor createExecutor() {
        return new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, unit,
                                      new LinkedBlockingDeque<Runnable>());
    }

    private AtomicBoolean mPuseWork  = new AtomicBoolean(false);
    private final Object  mPauseLock = new Object();

    // TODO
    public void onResume() {
        mPuseWork.set(false);
        synchronized (mPauseLock) {
            mPauseLock.notifyAll();
        }
    }

    // TODO
    public void onPause() {
        mPuseWork.set(true);
    }

    public AtomicBoolean getIsPauseWork() {
        return mPuseWork;
    }

    public Object getPauseLock() {
        return mPauseLock;
    }

    ReentrantLock getLockForUri(String uri) {
        ReentrantLock lock = uriLocks.get(uri);
        if (lock == null) {
            lock = new ReentrantLock();
            uriLocks.put(uri, lock);
        }
        return lock;
    }

    // TODO
    public void onDestory() {
        ((ExecutorService)taskExecutor).shutdownNow();
        ((ExecutorService)taskExecutorForCachedImages).shutdownNow();
    }

    // TODO
    public void clear() {

    }
}
