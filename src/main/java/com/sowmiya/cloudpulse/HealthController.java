package com.sowmiya.cloudpulse;

import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {
    @Value("${HOSTNAME:local}")
    private String instance;

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "service", "CloudPulse",
            "version", "1.1.0",
            "instance", instance,
            "timestamp", Instant.now().toString()
        );
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of(
            "name", "CloudPulse",
            "description", "Self-healing cloud application demo",
            "version", "1.1.0",
            "features", new String[]{"health checks", "container deployment", "cloud autoscaling", "automated-cd"}
        );
    }
}
