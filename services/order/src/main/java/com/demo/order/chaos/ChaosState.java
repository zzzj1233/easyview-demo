package com.demo.order.chaos;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ChaosState {
    public final AtomicLong latencyMs    = new AtomicLong(0);
    public final AtomicLong dbExtraMs    = new AtomicLong(0);
    public final AtomicInteger errorPercent = new AtomicInteger(0);
    public final List<byte[]> heapHolder = new ArrayList<>();
    public final AtomicInteger cpuBurnSeconds = new AtomicInteger(0);

    public void maybeLatency()  { long ms = latencyMs.get();  if (ms > 0) sleep(ms); }
    public void maybeDbDelay()  { long ms = dbExtraMs.get();  if (ms > 0) sleep(ms); }
    public void maybeError() {
        int p = errorPercent.get();
        if (p > 0 && ThreadLocalRandom.current().nextInt(100) < p) {
            throw new RuntimeException("chaos: injected error (errorPercent=" + p + ")");
        }
    }

    public void burnCpu(int seconds) {
        cpuBurnSeconds.set(seconds);
        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
            new Thread(() -> {
                long until = System.currentTimeMillis() + seconds * 1000L;
                while (System.currentTimeMillis() < until) {
                    Math.sqrt(ThreadLocalRandom.current().nextDouble());
                }
            }, "chaos-cpu-" + i).start();
        }
    }

    public void leakHeap(int mb) {
        for (int i = 0; i < mb; i++) heapHolder.add(new byte[1024 * 1024]);
    }

    public void reset() {
        latencyMs.set(0); dbExtraMs.set(0); errorPercent.set(0);
        cpuBurnSeconds.set(0); heapHolder.clear();
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
