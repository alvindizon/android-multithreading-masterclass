package com.techyourchance.multithreading.demonstrations.visibility;

public class VisibilityDemonstration {

    private static class Counter {
        private volatile int mCount = 0; // since this is volatile it is guaranteed that the two threads will read the correct value
    }

    // the pointer to mCount is guaranteed to have happens-before relationship with the Threads, since the
    // assignment happens before their start() methods have been called
    private final static Counter sCount = new Counter(); // final here is optional, remove it and the behavior is still correct, but it is good practice to declare this as final

    public static void main(String[] args) {
        // any reads or writes to the mCount variable is guaranteed to have a happens-before relationship, thus
        // each thread sees the correct value
        new Consumer().start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            return;
        }
        new Producer().start();
    }

    static class Consumer extends Thread {
        @Override
        public void run() {
            int localValue = -1;
            while (true) {
                if (localValue != sCount.mCount) {
                    System.out.println("Consumer: detected count change " + sCount.mCount);
                    localValue = sCount.mCount;
                }
                if (sCount.mCount >= 5) {
                    break;
                }
            }
            System.out.println("Consumer: terminating");
        }
    }

    static class Producer extends Thread {
        @Override
        public void run() {
            while (true) {
                if(sCount.mCount >= 5) {
                    break;
                }
                int localValue = sCount.mCount;
                localValue++;
                System.out.println("Producer: incrementing count to " + localValue);
                sCount.mCount = localValue;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
            }
            System.out.println("Producer: terminating");
        }
    }

}
