package com.techyourchance.multithreading.exercises.exercise9;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.techyourchance.multithreading.common.BaseObservable;

import java.math.BigInteger;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import androidx.annotation.WorkerThread;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class ComputeFactorialUseCase extends BaseObservable<ComputeFactorialUseCase.Listener> {

    private Disposable disposable;

    public interface Listener {
        void onFactorialComputed(BigInteger result);
        void onFactorialComputationTimedOut();
        void onFactorialComputationAborted();
    }

    private final Handler mUiHandler = new Handler(Looper.getMainLooper());
    private int mNumberOfThreads;
    private ComputationRange[] mThreadsComputationRanges;
    
    public void computeFactorialAndNotify(final int argument, final int timeout) {
        initComputationParams(argument);
        disposable =  Flowable.range(0, mNumberOfThreads)
                .parallel(mNumberOfThreads)
                .runOn(Schedulers.io())
                .map(this::computeFactorial)
                .sequential()
                .scan(BigInteger::multiply)
                .last(new BigInteger("0"))
                .timeout(timeout, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::notifySuccess, error -> {
                    if(error instanceof TimeoutException) {
                        notifyTimeout();
                    } else {
                        notifyAborted();
                    }
                });
    }

    private void initComputationParams(int factorialArgument) {
        mNumberOfThreads = factorialArgument < 20
                ? 1 : Runtime.getRuntime().availableProcessors();

        mThreadsComputationRanges = new ComputationRange[mNumberOfThreads];

        int computationRangeSize = factorialArgument / mNumberOfThreads;

        long nextComputationRangeEnd = factorialArgument;
        for (int i = mNumberOfThreads - 1; i >= 0; i--) {
            mThreadsComputationRanges[i] = new ComputationRange(
                    nextComputationRangeEnd - computationRangeSize + 1,
                    nextComputationRangeEnd
            );
            nextComputationRangeEnd = mThreadsComputationRanges[i].start - 1;
        }

        // add potentially "remaining" values to first thread's range
        mThreadsComputationRanges[0].start = 1;
    }

    private BigInteger computeFactorial(int i) {
        long rangeStart = mThreadsComputationRanges[i].start;
        long rangeEnd = mThreadsComputationRanges[i].end;
        BigInteger product = new BigInteger("1");
        for (long num = rangeStart; num <= rangeEnd; num++) {
            product = product.multiply(new BigInteger(String.valueOf(num)));
        }
        return product;
    }

    private void notifySuccess(final BigInteger result) {
        mUiHandler.post(() -> {
            for (Listener listener : getListeners()) {
                listener.onFactorialComputed(result);
            }
        });
    }

    private void notifyAborted() {
        mUiHandler.post(() -> {
            for (Listener listener : getListeners()) {
                listener.onFactorialComputationAborted();
            }
        });
    }

    private void notifyTimeout() {
        mUiHandler.post(() -> {
            for (Listener listener : getListeners()) {
                listener.onFactorialComputationTimedOut();
            }
        });
    }

    @Override
    public void unregisterListener(Listener listener) {
        if(disposable != null) {
            disposable.dispose();
        }
        super.unregisterListener(listener);
    }

    private static class ComputationRange {
        private long start;
        private long end;

        public ComputationRange(long start, long end) {
            this.start = start;
            this.end = end;
        }
    }
}
