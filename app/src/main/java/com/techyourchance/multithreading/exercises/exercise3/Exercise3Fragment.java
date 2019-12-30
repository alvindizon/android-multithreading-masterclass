package com.techyourchance.multithreading.exercises.exercise3;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.techyourchance.multithreading.R;
import com.techyourchance.multithreading.common.BaseFragment;

import java.util.concurrent.atomic.AtomicBoolean;

public class Exercise3Fragment extends BaseFragment {

    private static final int SECONDS_TO_COUNT = 3;

    public static Fragment newInstance() {
        return new Exercise3Fragment();
    }

    private Button mBtnCountSeconds;
    private TextView mTxtCount;
    private AtomicBoolean countBoolean = new AtomicBoolean(false);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_exercise_3, container, false);

        mBtnCountSeconds = view.findViewById(R.id.btn_count_seconds);
        mTxtCount = view.findViewById(R.id.txt_count);

        mBtnCountSeconds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                countIterations();
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // ensure that button is enabled when exercise 3 screen is first displayed
        mBtnCountSeconds.setEnabled(true);
        // set flag to true in order for count runnable to execute
        countBoolean.set(true);
    }

    @Override
    public void onStop() {
        countBoolean.set(false);
        super.onStop();
    }

    @Override
    protected String getScreenTitle() {
        return "Exercise 3";
    }

    private void countIterations() {
        /*
        1. Disable button to prevent multiple clicks
        2. Start counting on background thread using loop and Thread.sleep()
        3. Show count in TextView
        4. When count completes, show "done" in TextView and enable the button
         */
        new Thread(() -> {
            runOnUiThread(() -> mBtnCountSeconds.setEnabled(false));
            for(int i = 1; i <= SECONDS_TO_COUNT; i++) {
                if(!countBoolean.get()) {
                    return; // return if fragment is not displayed on screen
                }
                final int count = i;
                runOnUiThread(() -> mTxtCount.setText("Count: " + count));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            runOnUiThread(() -> mBtnCountSeconds.setEnabled(true));
            runOnUiThread(() -> mTxtCount.setText("Done counting"));
        }).start();
    }

    private void runOnUiThread(Runnable runnable) {
        if(countBoolean.get()) { // if screen has returned to main menu, do not post runnable to UI thread
            requireActivity().runOnUiThread(runnable);
        }
    }
}
