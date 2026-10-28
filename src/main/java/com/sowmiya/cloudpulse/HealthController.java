package com.sowmiya.cloudpulse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {
    @Value("${HOSTNAME:local}")
    private String instance;

    @Value("${STRESS_ENABLED:false}")
    private boolean stressEnabled;

    @Value("${STRESS_KEY:}")
    private String stressKey;

    private final AtomicBoolean stressRunning = new AtomicBoolean(false);

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

    @GetMapping("/stress")
    public ResponseEntity<Map<String, Object>> stress(
            @RequestHeader(value = "X-Stress-Key", required = false) String providedKey,
            @RequestParam(value = "duration", defaultValue = "30") int durationSeconds) {

        if (!stressEnabled || stressKey == null || stressKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Stress testing endpoint is disabled on this instance."));
        }

        if (providedKey == null || !constantTimeEquals(stressKey, providedKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized access."));
        }

        int duration = Math.min(Math.max(durationSeconds, 10), 60);

        if (!stressRunning.compareAndSet(false, true)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "A stress test is already in progress."));
        }

        Thread.ofVirtual().start(() -> {
            try {
                long end = System.currentTimeMillis() + (duration * 1000L);
                int threads = Runtime.getRuntime().availableProcessors();
                Thread[] workers = new Thread[threads];
                for (int i = 0; i < threads; i++) {
                    workers[i] = new Thread(() -> {
                        while (System.currentTimeMillis() < end) {
                            Math.atan(Math.tan(Math.atan(Math.tan(Math.random()))));
                        }
                    });
                    workers[i].start();
                }
                for (Thread t : workers) {
                    t.join();
                }
            } catch (Exception ignored) {
            } finally {
                stressRunning.set(false);
            }
        });

        return ResponseEntity.ok(Map.of(
            "status", "Stress initiated",
            "instance", instance,
            "duration_seconds", duration,
            "target", "Controlled CPU load generation"
        ));
    }

    private boolean constantTimeEquals(String expected, String actual) {
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}
