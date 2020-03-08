package com.techyourchance.multithreading.exercises.exercise4;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.techyourchance.multithreading.DefaultConfiguration;
import com.techyourchance.multithreading.R;
import com.techyourchance.multithreading.common.BaseFragment;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.UnaryOperator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;

public class Exercise4Fragment extends BaseFragment {

    public static Fragment newInstance() {
        return new Exercise4Fragment();
    }

    private final static int MAX_TIMEOUT_MS = DefaultConfiguration.DEFAULT_FACTORIAL_TIMEOUT_MS; // finalization ensures that the compiler completes initialization

    private final Handler mUiHandler = new Handler(Looper.getMainLooper()); // finalization ensures that the compiler completes initialization

    private EditText mEdtArgument;
    private EditText mEdtTimeout;
    private Button mBtnStartWork;
    private TextView mTxtResult;

    private int mNumberOfThreads;
    private ComputationRange[] mThreadsComputationRanges;
    private int mNumOfFinishedThreads;

    private long mComputationTimeoutTime;

    private boolean mAbortComputation;
    
    AtomicReferenceArray<BigInteger> results;

    private final Object LOCK = new Object();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_exercise_4, container, false);

        mEdtArgument = view.findViewById(R.id.edt_argument);
        mEdtTimeout = view.findViewById(R.id.edt_timeout);
        mBtnStartWork = view.findViewById(R.id.btn_compute);
        mTxtResult = view.findViewById(R.id.txt_result);

        mBtnStartWork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEdtArgument.getText().toString().isEmpty()) {
                    return;
                }

                mTxtResult.setText("");
                mBtnStartWork.setEnabled(false);


                InputMethodManager imm =
                        (InputMethodManager) requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mBtnStartWork.getWindowToken(), 0);

                int argument = Integer.valueOf(mEdtArgument.getText().toString());

                computeFactorial(argument, getTimeout());
            }
        });

        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        mAbortComputation = true;
    }

    @Override
    protected String getScreenTitle() {
        return "Exercise 4";
    }

    private int getTimeout() {
        int timeout;
        if (mEdtTimeout.getText().toString().isEmpty()) {
            timeout = MAX_TIMEOUT_MS;
        } else {
            timeout = Integer.valueOf(mEdtTimeout.getText().toString());
            if (timeout > MAX_TIMEOUT_MS) {
                timeout = MAX_TIMEOUT_MS;
            }
        }
        return timeout;
    }

    private void computeFactorial(final int factorialArgument, final int timeout) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                initComputationParams(factorialArgument, timeout);
                startComputation();
                waitForThreadsResultsOrTimeoutOrAbort();
                processComputationResults();
            }
        }).start();
    }

    private void initComputationParams(int factorialArgument, int timeout) {
        mNumberOfThreads = factorialArgument < 20
                ? 1 : Runtime.getRuntime().availableProcessors();

        mNumOfFinishedThreads = 0;

        mAbortComputation = false;

        results = new AtomicReferenceArray<>(mNumberOfThreads);

        mThreadsComputationRanges = new ComputationRange[mNumberOfThreads];

        initThreadsComputationRanges(factorialArgument);

        mComputationTimeoutTime = System.currentTimeMillis() + timeout;
    }

    private void initThreadsComputationRanges(int factorialArgument) {
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

    @WorkerThread
    private void startComputation() {
        for (int i = 0; i < mNumberOfThreads; i++) {

            final int threadIndex = i; // happens-before relationship with new thread

            new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (LOCK) {
                        long rangeStart = mThreadsComputationRanges[threadIndex].start;
                        long rangeEnd = mThreadsComputationRanges[threadIndex].end;
                        BigInteger product = new BigInteger("1");
                        for (long num = rangeStart; num <= rangeEnd; num++) {
                            if (isTimedOut()) {
                                break;
                            }
                            product = product.multiply(new BigInteger(String.valueOf(num)));
                        }
                        results.set(threadIndex, product);
                        mNumOfFinishedThreads++;
                    }
                }
            }).start();

        }
    }

    @WorkerThread
    private void waitForThreadsResultsOrTimeoutOrAbort() {
        while (true) {
            if (mNumOfFinishedThreads == mNumberOfThreads) {
                break;
            } else if(mAbortComputation) {
                break;
            } else if (isTimedOut()) {
                break;
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // do nothing and keep looping
                }
            }
        }
    }

    @WorkerThread
    private void processComputationResults() {
        String resultString;

        if (mAbortComputation) {
            resultString = "Computation aborted";
        }
        else {
            resultString = computeFinalResult().toString();
        }

        // need to check for timeout after computation of the final result
        if (isTimedOut()) {
            resultString = "Computation timed out";
        }

        final String finalResultString = resultString;

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!Exercise4Fragment.this.isStateSaved()) {
                    mTxtResult.setText(finalResultString);
                    mBtnStartWork.setEnabled(true);
                }
            }
        });
    }

    @WorkerThread
    private BigInteger computeFinalResult() {
        BigInteger result = new BigInteger("1");
        for (int i = 0; i < mNumberOfThreads; i++) {
            if (isTimedOut()) {
                break;
            }
            result = result.multiply(results.get(i));
        }
        return result;
    }

    private boolean isTimedOut() {
        return System.currentTimeMillis() >= mComputationTimeoutTime;
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
