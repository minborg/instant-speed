package org.minborg.instantspeed.zgc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;

public class HeapPolluter implements Runnable {

    private static final int MAX_SIZE = 10_000;
    private static final long GC_INTERVALS = TimeUnit.MINUTES.toMicros(15);

    // Deterministic random generator
    private final Random random = new Random(42);

    // Holds references to objects
    private final List<Long> list = new ArrayList<>();

    private final AtomicBoolean stopped = new AtomicBoolean();

    @Override
    public void run() {
        long nextGc = System.currentTimeMillis() + GC_INTERVALS;
        try {
            int cnt = 0;
            while (!stopped.get()) {
                for (int i = 0; i < 10; i++) {
                    list.add(random.nextLong());
                }
                Thread.sleep(1);
                if ((++cnt % MAX_SIZE) == 0) {
                    cnt = 0;
                    System.out.format("%s releasing %,d Long objects and unused backing arrays%n", HeapPolluter.class.getSimpleName(), list.size());
                    list.clear();
                }
                if (System.currentTimeMillis() > nextGc) {
                    System.out.println("Suggesting gc");
                    System.gc();
                    nextGc = System.currentTimeMillis() + GC_INTERVALS;
                }
            }
            System.out.println(HeapPolluter.class.getSimpleName() + " stopped");
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }

    public void stop() {
        stopped.set(true);
    }

}