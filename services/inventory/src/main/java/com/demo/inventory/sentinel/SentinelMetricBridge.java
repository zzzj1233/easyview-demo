package com.demo.inventory.sentinel;

import com.demo.api.sentinel.SentinelMicrometerMetricExtension;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class SentinelMetricBridge {
    private final MeterRegistry registry;
    public SentinelMetricBridge(MeterRegistry registry) { this.registry = registry; }
    @PostConstruct public void init() { SentinelMicrometerMetricExtension.bind(registry); }
}
