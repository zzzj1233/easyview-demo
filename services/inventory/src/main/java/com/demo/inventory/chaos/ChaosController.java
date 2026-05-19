package com.demo.inventory.chaos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/chaos")
public class ChaosController {
    private static final Logger log = LoggerFactory.getLogger(ChaosController.class);
    private final ChaosState state;

    public ChaosController(ChaosState state) {
        this.state = state;
    }

    @PostMapping("/latency")
    public Map<String, Object> latency(@RequestParam long ms) {
        state.latencyMs.set(ms);
        log.warn("[CHAOS] latency injected: {} ms", ms);
        return Map.of("ok", true, "latencyMs", ms);
    }

    @PostMapping("/db-slow")
    public Map<String, Object> dbSlow(@RequestParam long ms) {
        state.dbExtraMs.set(ms);
        log.warn("[CHAOS] db-slow injected: {} ms", ms);
        return Map.of("ok", true, "dbExtraMs", ms);
    }

    @PostMapping("/error")
    public Map<String, Object> error(@RequestParam int percent) {
        state.errorPercent.set(percent);
        log.warn("[CHAOS] error rate injected: {}%", percent);
        return Map.of("ok", true, "errorPercent", percent);
    }

    @PostMapping("/cpu")
    public Map<String, Object> cpu(@RequestParam(defaultValue = "60") int seconds) {
        state.burnCpu(seconds);
        log.warn("[CHAOS] CPU burn for {}s", seconds);
        return Map.of("ok", true, "cpuBurnSeconds", seconds);
    }

    @PostMapping("/oom")
    public Map<String, Object> oom(@RequestParam(defaultValue = "50") int mb) {
        state.leakHeap(mb);
        log.warn("[CHAOS] heap leak +{} MB (total held = {} MB)", mb, state.heapHolder.size());
        return Map.of("ok", true, "addedMb", mb, "totalHeldMb", state.heapHolder.size());
    }

    @PostMapping("/reset")
    public Map<String, Object> reset() {
        state.reset();
        log.warn("[CHAOS] reset all");
        return Map.of("ok", true);
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "latencyMs",    state.latencyMs.get(),
                "dbExtraMs",    state.dbExtraMs.get(),
                "errorPercent", state.errorPercent.get(),
                "heldHeapMb",   state.heapHolder.size()
        );
    }
}
