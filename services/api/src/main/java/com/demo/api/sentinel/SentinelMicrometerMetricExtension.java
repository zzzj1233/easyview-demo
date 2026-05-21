package com.demo.api.sentinel;

import com.alibaba.csp.sentinel.metric.extension.MetricExtension;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class SentinelMicrometerMetricExtension implements MetricExtension {
    private static final AtomicReference<MeterRegistry> REGISTRY = new AtomicReference<>();

    public static void bind(MeterRegistry registry) { REGISTRY.set(registry); }

    @Override public void addPass(String resource, int n, Object... args) { inc("sentinel_pass_total", resource, n); }
    @Override public void addBlock(String resource, int n, String origin, com.alibaba.csp.sentinel.slots.block.BlockException e, Object... args) { incTag("sentinel_block_total", resource, e == null ? "unknown" : e.getClass().getSimpleName(), n); }
    @Override public void addSuccess(String resource, int n, Object... args) { /* covered by pass */ }
    @Override public void addException(String resource, int n, Throwable t) { inc("sentinel_exception_total", resource, n); }
    @Override public void addRt(String resource, long rt, Object... args) {
        MeterRegistry r = REGISTRY.get();
        if (r == null) return;
        Timer.builder("sentinel_rt_seconds").tag("resource", resource)
                .publishPercentileHistogram().register(r)
                .record(rt, TimeUnit.MILLISECONDS);
    }
    @Override public void increaseThreadNum(String resource, Object... args) {}
    @Override public void decreaseThreadNum(String resource, Object... args) {}

    private void inc(String name, String resource, int n) {
        MeterRegistry r = REGISTRY.get();
        if (r == null) return;
        Counter.builder(name).tag("resource", resource).register(r).increment(n);
    }
    private void incTag(String name, String resource, String tag, int n) {
        MeterRegistry r = REGISTRY.get();
        if (r == null) return;
        Counter.builder(name).tag("resource", resource).tag("type", tag).register(r).increment(n);
    }
}
