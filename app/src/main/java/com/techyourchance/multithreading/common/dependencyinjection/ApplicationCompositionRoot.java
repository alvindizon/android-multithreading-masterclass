package com.techyourchance.multithreading.common.dependencyinjection;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.techyourchance.fragmenthelper.FragmentContainerWrapper;
import com.techyourchance.fragmenthelper.FragmentHelper;
import com.techyourchance.multithreading.common.ScreensNavigator;
import com.techyourchance.multithreading.common.ToolbarManipulator;
import com.techyourchance.threadposter.BackgroundThreadPoster;
import com.techyourchance.threadposter.UiThreadPoster;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import androidx.fragment.app.FragmentActivity;

public class ApplicationCompositionRoot {

    private ThreadPoolExecutor mThreadPoolExecutor;
    private UiThreadPoster uiThreadPoster;
    private BackgroundThreadPoster backgroundThreadPoster;

    public ThreadPoolExecutor getThreadPool() {
        if (mThreadPoolExecutor == null) {
            mThreadPoolExecutor = new ThreadPoolExecutor(
                    10,
                    Integer.MAX_VALUE,
                    10,
                    TimeUnit.SECONDS,
                    new SynchronousQueue<>(),
                    new ThreadFactory() {
                        @Override
                        public Thread newThread(Runnable r) {
                            Log.d("ThreadFactory",
                                  String.format("size %s, active count %s, queue remaining %s",
                                                mThreadPoolExecutor.getPoolSize(),
                                                mThreadPoolExecutor.getActiveCount(),
                                                mThreadPoolExecutor.getQueue().remainingCapacity()
                                  )
                            );
                            return new Thread(r);
                        }
                    }
            );
        }
        return mThreadPoolExecutor;
    }

    public UiThreadPoster getUiThreadPoster() {
        if(uiThreadPoster == null) {
            uiThreadPoster = new UiThreadPoster();
        }
        return uiThreadPoster;
    }

    public BackgroundThreadPoster getBackgroundThreadPoster() {
        if(backgroundThreadPoster == null) {
            backgroundThreadPoster = new BackgroundThreadPoster();
        }
        return backgroundThreadPoster;
    }
}
