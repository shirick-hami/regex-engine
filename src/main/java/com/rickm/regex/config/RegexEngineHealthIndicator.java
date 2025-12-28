package com.rickm.regex.config;

import com.rickm.regex.service.RegexService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Custom health indicator for the Regex Engine.
 * Reports on engine health, cache status, and performance metrics.
 */
@Component
@RequiredArgsConstructor
public class RegexEngineHealthIndicator implements HealthIndicator {
    
    private final RegexService regexService;
    private final RegexEngineConfig config;
    
    @Override
    public Health health() {
        try {
            // Quick health check - try to compile and match a simple pattern
            long start = System.currentTimeMillis();
            var result = regexService.matchFull("a+", "aaa");
            long elapsed = System.currentTimeMillis() - start;
            
            if (!result.isMatched()) {
                return Health.down()
                        .withDetail("error", "Basic pattern matching failed")
                        .build();
            }
            
            Map<String, Object> metrics = regexService.getMetrics();
            
            Health.Builder builder = Health.up()
                    .withDetail("status", "operational")
                    .withDetail("healthCheckTimeMs", elapsed)
                    .withDetail("cacheSize", regexService.getCacheSize())
                    .withDetail("cacheMaxSize", config.getCacheMaxSize())
                    .withDetail("totalMatches", metrics.get("totalMatches"))
                    .withDetail("successRate", metrics.getOrDefault("successRate", 0.0));
            
            // Warn if cache is nearly full
            int cacheSize = regexService.getCacheSize();
            if (cacheSize > config.getCacheMaxSize() * 0.9) {
                builder.withDetail("warning", "Cache nearly full");
            }
            
            return builder.build();
            
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}
