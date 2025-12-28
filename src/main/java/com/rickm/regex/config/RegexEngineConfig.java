package com.rickm.regex.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the regex engine.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "regex.engine")
@Validated
public class RegexEngineConfig {

    /**
     * Maximum allowed pattern length in characters.
     * Default: 10,000 characters.
     */
    @Positive
    @Max(100000)
    private int maxPatternLength = 10000;

    /**
     * Maximum allowed input string length in characters.
     * Default: 1,000,000 characters (1MB for ASCII).
     */
    @Positive
    @Max(10000000)
    private int maxInputLength = 1000000;

    /**
     * Maximum number of backtracking steps allowed.
     * This prevents catastrophic backtracking.
     * Default: 100,000 steps.
     */
    @Positive
    @Max(10000000)
    private long maxBacktrackLimit = 100000;

    /**
     * Maximum execution time for a single match operation in milliseconds.
     * Default: 30 seconds.
     */
    @Min(100)
    @Max(300000)
    private long timeoutMs = 30000;

    /**
     * Whether to enable caching of compiled patterns.
     */
    private boolean cacheEnabled = true;

    /**
     * Maximum number of patterns to cache.
     */
    @Positive
    @Max(10000)
    private int cacheMaxSize = 1000;
}
