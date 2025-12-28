package com.rickm.regex.controller;

import com.rickm.regex.service.RegexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller for health monitoring and metrics endpoints.
 */
@RestController
@RequestMapping("/api/v1/monitor")
@RequiredArgsConstructor
@Tag(name = "Monitoring", description = "Health monitoring and metrics")
public class MonitoringController {

    private final RegexService regexService;

    /**
     * Gets comprehensive engine metrics.
     */
    @GetMapping("/metrics")
    @Operation(summary = "Get engine metrics", description = "Returns detailed performance metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();

        // Engine metrics
        metrics.put("engine", regexService.getMetrics());

        // JVM metrics
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();

        Map<String, Object> jvm = new LinkedHashMap<>();
        jvm.put("uptimeMs", runtimeBean.getUptime());
        jvm.put("heapUsed", memoryBean.getHeapMemoryUsage().getUsed());
        jvm.put("heapMax", memoryBean.getHeapMemoryUsage().getMax());
        jvm.put("heapCommitted", memoryBean.getHeapMemoryUsage().getCommitted());
        jvm.put("nonHeapUsed", memoryBean.getNonHeapMemoryUsage().getUsed());
        jvm.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        metrics.put("jvm", jvm);

        // System info
        Map<String, Object> system = new LinkedHashMap<>();
        system.put("javaVersion", System.getProperty("java.version"));
        system.put("javaVendor", System.getProperty("java.vendor"));
        system.put("osName", System.getProperty("os.name"));
        system.put("osArch", System.getProperty("os.arch"));
        metrics.put("system", system);

        return ResponseEntity.ok(metrics);
    }

    /**
     * Gets a quick status check.
     */
    @GetMapping("/status")
    @Operation(summary = "Quick status check", description = "Returns basic operational status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();

        try {
            // Quick health check
            long start = System.currentTimeMillis();
            var result = regexService.matchFull("[a-z]+", "test");
            long elapsed = System.currentTimeMillis() - start;

            status.put("status", "UP");
            status.put("operational", true);
            status.put("healthCheckTimeMs", elapsed);
            status.put("cacheSize", regexService.getCacheSize());

            Map<String, Object> metrics = regexService.getMetrics();
            status.put("totalRequests", metrics.get("totalMatches"));

        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("operational", false);
            status.put("error", e.getMessage());
        }

        return ResponseEntity.ok(status);
    }

    /**
     * Resets engine metrics.
     */
    @PostMapping("/metrics/reset")
    @Operation(summary = "Reset metrics", description = "Resets all engine metrics to zero")
    public ResponseEntity<Map<String, String>> resetMetrics() {
        regexService.resetMetrics();
        return ResponseEntity.ok(Map.of("message", "Metrics reset successfully"));
    }

    /**
     * Gets cache statistics.
     */
    @GetMapping("/cache")
    @Operation(summary = "Get cache stats", description = "Returns pattern cache statistics")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> cache = new LinkedHashMap<>();
        cache.put("size", regexService.getCacheSize());
        cache.put("enabled", regexService.getCacheSize() >= 0);
        return ResponseEntity.ok(cache);
    }

    /**
     * Clears the pattern cache.
     */
    @DeleteMapping("/cache")
    @Operation(summary = "Clear cache", description = "Clears all cached compiled patterns")
    public ResponseEntity<Map<String, String>> clearCache() {
        regexService.clearCache();
        return ResponseEntity.ok(Map.of("message", "Cache cleared successfully"));
    }
}
