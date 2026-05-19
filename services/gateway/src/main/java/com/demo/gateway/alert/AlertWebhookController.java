package com.demo.gateway.alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 模拟接收 AlertManager webhook。
 * 把告警关键字段（含 traceId / dashboard / runbook 三个一键链接）打印到日志，
 * 实际生产中应转发到飞书/钉钉/Slack。
 */
@RestController
public class AlertWebhookController {
    private static final Logger log = LoggerFactory.getLogger("ALERT");

    @SuppressWarnings("unchecked")
    @PostMapping("/alert/webhook")
    public Map<String, Object> receive(@RequestBody Map<String, Object> body) {
        List<Map<String, Object>> alerts = (List<Map<String, Object>>) body.getOrDefault("alerts", List.of());
        for (Map<String, Object> a : alerts) {
            Map<String, Object> labels = (Map<String, Object>) a.getOrDefault("labels", Map.of());
            Map<String, Object> ann    = (Map<String, Object>) a.getOrDefault("annotations", Map.of());
            log.warn("=================================");
            log.warn("[ALERT] {} status={}", labels.get("alertname"), a.get("status"));
            log.warn("        application={} instance={} severity={}",
                    labels.get("application"), labels.get("instance"), labels.get("severity"));
            log.warn("        summary    : {}", ann.get("summary"));
            log.warn("        dashboard  : {}", ann.get("dashboard"));
            log.warn("        trace_link : {}", ann.get("trace_link"));
            log.warn("        runbook    : {}", ann.get("runbook_url"));
            log.warn("=================================");
        }
        return Map.of("ok", true, "received", alerts.size());
    }
}
