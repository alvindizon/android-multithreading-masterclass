package com.techyourchance.multithreading.demonstrations.visibility;

public class VisibilityDemonstration {

    private static final Object LOCK = new Object();

    private static int sCount = 0;

    public static void main(String[] args) {
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
                synchronized (LOCK) {
                    if (localValue != sCount) {
                        System.out.println("Consumer: detected count change " + sCount);
                        localValue = sCount;
                    }
                    if (sCount >= 5) {
                        break;
                    }
                }
            }
            System.out.println("Consumer: terminating");
        }
    }

    static class Producer extends Thread {
        @Override
        public void run() {
            while (true) {
                synchronized (LOCK) { // hold this LOCK until the code inside synchronized block completes
                    if(sCount >= 5) { // transferred read of sCount inside synchronized block to make code thread-safe
                        break;
                    }
                    int localValue = sCount;
                    localValue++;
                    System.out.println("Producer: incrementing count to " + localValue);
                    sCount = localValue;
                }
                // put Thread.sleep outside of synchronized block
                // after LOCK is released, Thread will sleep for 1 sec and this should be enough
                // time for Consumer to detect changes (i.e., acquire the LOCK)
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
