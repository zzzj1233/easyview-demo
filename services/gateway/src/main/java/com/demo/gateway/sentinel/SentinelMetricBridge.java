package com.demo.gateway.sentinel;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.demo.api.sentinel.SentinelMicrometerMetricExtension;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class SentinelMetricBridge {
    private final MeterRegistry registry;
    public SentinelMetricBridge(MeterRegistry registry) { this.registry = registry; }
    @PostConstruct public void init() { SentinelMicrometerMetricExtension.bind(registry); }
    @Bean public SentinelResourceAspect sentinelResourceAspect() { return new SentinelResourceAspect(); }
}
