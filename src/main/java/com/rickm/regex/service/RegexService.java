package com.rickm.regex.service;

import com.rickm.regex.config.RegexEngineConfig;
import com.rickm.regex.dto.CompiledPattern;
import com.rickm.regex.engine.dto.MatchResult;
import com.rickm.regex.engine.matcher.BacktrackingMatcher;
import com.rickm.regex.engine.matcher.DFAMatcher;
import com.rickm.regex.engine.matcher.NFAMatcher;
import com.rickm.regex.engine.parser.AstNode;
import com.rickm.regex.engine.parser.BasicRegexParser;
import com.rickm.regex.engine.parser.RegexParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Main service for regex compilation and matching operations.
 *
 * Supports three matching engines:
 * - BACKTRACKING: Traditional recursive backtracking (default)
 * - NFA: Thompson's NFA simulation (linear time, parallel state tracking)
 * - DFA: Deterministic finite automaton (fastest, but higher memory)
 */
@Service
@Slf4j
public class RegexService {

    public enum MatchEngine {
        BACKTRACKING,
        NFA,
        DFA
    }

    private final RegexEngineConfig config;
    private final Map<String, CompiledPattern> patternCache;

    // Metrics
    private final AtomicLong totalMatches = new AtomicLong(0);
    private final AtomicLong successfulMatches = new AtomicLong(0);
    private final AtomicLong failedMatches = new AtomicLong(0);
    private final AtomicLong totalMatchTimeMs = new AtomicLong(0);
    private final AtomicLong compilations = new AtomicLong(0);

    public RegexService(RegexEngineConfig config) {
        this.config = config;

        if (config.isCacheEnabled()) {
            this.patternCache = new ConcurrentHashMap<>(
                    new LinkedHashMap<String, CompiledPattern>(config.getCacheMaxSize(), 0.75f, true) {
                        @Override
                        protected boolean removeEldestEntry(Map.Entry<String, CompiledPattern> eldest) {
                            return size() > config.getCacheMaxSize();
                        }
                    });
        } else {
            this.patternCache = null;
        }
    }

    /**
     * Compiles a regex pattern into an AST.
     */
    public CompiledPattern compile(String pattern) {
        validatePattern(pattern);

        if (patternCache != null && patternCache.containsKey(pattern)) {
            log.debug("Pattern cache hit for: {}", pattern);
            return patternCache.get(pattern);
        }

        log.debug("Compiling pattern: {}", pattern);
        long startTime = System.currentTimeMillis();

        RegexParser parser = new BasicRegexParser(pattern);
        AstNode ast = parser.parse();

        long compileTime = System.currentTimeMillis() - startTime;
        compilations.incrementAndGet();

        CompiledPattern compiled = CompiledPattern.builder()
                .pattern(pattern)
                .ast(ast)
                .compileTimeMs(compileTime)
                .astDescription(buildAstDescription(ast))
                .build();

        if (patternCache != null) {
            patternCache.put(pattern, compiled);
        }

        log.debug("Pattern compiled in {}ms", compileTime);
        return compiled;
    }

    /**
     * Matches using default (backtracking) engine.
     */
    public MatchResult matchFull(String pattern, String input) {
        return matchFull(pattern, input, MatchEngine.BACKTRACKING);
    }

    /**
     * Matches using specified engine.
     */
    public MatchResult matchFull(String pattern, String input, MatchEngine engine) {
        validateInput(input);
        CompiledPattern compiled = compile(pattern);

        MatchResult result;
        switch (engine) {
            case NFA:
                NFAMatcher nfaMatcher = new NFAMatcher(compiled.getAst(), config.getTimeoutMs());
                result = nfaMatcher.matchFull(input);
                break;
            case DFA:
                DFAMatcher dfaMatcher = new DFAMatcher(compiled.getAst(), config.getTimeoutMs());
                result = dfaMatcher.matchFull(input);
                break;
            default:
                BacktrackingMatcher btMatcher = new BacktrackingMatcher(
                        compiled.getAst(),
                        config.getMaxBacktrackLimit(),
                        config.getTimeoutMs()
                );
                result = btMatcher.matchFull(input);
        }

        result.setPattern(pattern);
        updateMetrics(result);
        return result;
    }

    /**
     * Finds the first match using default engine.
     */
    public MatchResult find(String pattern, String input) {
        return find(pattern, input, MatchEngine.BACKTRACKING);
    }

    /**
     * Finds the first match using specified engine.
     */
    public MatchResult find(String pattern, String input, MatchEngine engine) {
        validateInput(input);
        CompiledPattern compiled = compile(pattern);

        MatchResult result;
        switch (engine) {
            case NFA:
                NFAMatcher nfaMatcher = new NFAMatcher(compiled.getAst(), config.getTimeoutMs());
                result = nfaMatcher.find(input);
                break;
            case DFA:
                DFAMatcher dfaMatcher = new DFAMatcher(compiled.getAst(), config.getTimeoutMs());
                result = dfaMatcher.find(input);
                break;
            default:
                BacktrackingMatcher btMatcher = new BacktrackingMatcher(
                        compiled.getAst(),
                        config.getMaxBacktrackLimit(),
                        config.getTimeoutMs()
                );
                result = btMatcher.find(input);
        }

        result.setPattern(pattern);
        updateMetrics(result);
        return result;
    }

    /**
     * Finds all matches using default engine.
     */
    public MatchResult findAll(String pattern, String input) {
        return findAll(pattern, input, MatchEngine.BACKTRACKING);
    }

    /**
     * Finds all matches using specified engine.
     */
    public MatchResult findAll(String pattern, String input, MatchEngine engine) {
        validateInput(input);
        CompiledPattern compiled = compile(pattern);

        MatchResult result;
        switch (engine) {
            case NFA:
                NFAMatcher nfaMatcher = new NFAMatcher(compiled.getAst(), config.getTimeoutMs());
                result = nfaMatcher.findAll(input);
                break;
            case DFA:
                DFAMatcher dfaMatcher = new DFAMatcher(compiled.getAst(), config.getTimeoutMs());
                result = dfaMatcher.findAll(input);
                break;
            default:
                BacktrackingMatcher btMatcher = new BacktrackingMatcher(
                        compiled.getAst(),
                        config.getMaxBacktrackLimit(),
                        config.getTimeoutMs()
                );
                result = btMatcher.findAll(input);
        }

        result.setPattern(pattern);
        updateMetrics(result);
        return result;
    }

    /**
     * Validates a pattern without performing any matching.
     */
    public CompiledPattern validate(String pattern) {
        return compile(pattern);
    }

    /**
     * Replaces all matches with the replacement string.
     */
    public String replaceAll(String pattern, String input, String replacement) {
        return replaceAll(pattern, input, replacement, MatchEngine.BACKTRACKING);
    }

    public String replaceAll(String pattern, String input, String replacement, MatchEngine engine) {
        validateInput(input);

        MatchResult allMatches = findAll(pattern, input, engine);

        if (!allMatches.isMatched() || allMatches.getAllMatches().isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        for (MatchResult.MatchInfo match : allMatches.getAllMatches()) {
            result.append(input, lastEnd, match.getStartIndex());
            result.append(replacement);
            lastEnd = match.getEndIndex();
        }

        result.append(input.substring(lastEnd));
        return result.toString();
    }

    /**
     * Splits the input string by matches of the pattern.
     */
    public String[] split(String pattern, String input) {
        return split(pattern, input, MatchEngine.BACKTRACKING);
    }

    public String[] split(String pattern, String input, MatchEngine engine) {
        validateInput(input);

        MatchResult allMatches = findAll(pattern, input, engine);

        if (!allMatches.isMatched() || allMatches.getAllMatches().isEmpty()) {
            return new String[]{input};
        }

        java.util.List<String> parts = new java.util.ArrayList<>();
        int lastEnd = 0;

        for (MatchResult.MatchInfo match : allMatches.getAllMatches()) {
            parts.add(input.substring(lastEnd, match.getStartIndex()));
            lastEnd = match.getEndIndex();
        }

        parts.add(input.substring(lastEnd));
        return parts.toArray(new String[0]);
    }

    /**
     * Benchmarks all three engines on a pattern/input combination.
     */
    public Map<String, Object> benchmark(String pattern, String input) {
        validatePattern(pattern);
        validateInput(input);

        Map<String, Object> results = new LinkedHashMap<>();

        // Compile once
        CompiledPattern compiled = compile(pattern);
        results.put("pattern", pattern);
        results.put("inputLength", input.length());
        results.put("compileTimeMs", compiled.getCompileTimeMs());

        // Backtracking
        try {
            long start = System.currentTimeMillis();
            BacktrackingMatcher btMatcher = new BacktrackingMatcher(
                    compiled.getAst(), config.getMaxBacktrackLimit(), config.getTimeoutMs());
            MatchResult btResult = btMatcher.matchFull(input);
            results.put("backtracking", Map.of(
                    "matched", btResult.isMatched(),
                    "timeMs", System.currentTimeMillis() - start,
                    "operations", btResult.getBacktrackCount()
            ));
        } catch (Exception e) {
            results.put("backtracking", Map.of("error", e.getMessage()));
        }

        // NFA
        try {
            long start = System.currentTimeMillis();
            NFAMatcher nfaMatcher = new NFAMatcher(compiled.getAst(), config.getTimeoutMs());
            MatchResult nfaResult = nfaMatcher.matchFull(input);
            results.put("nfa", Map.of(
                    "matched", nfaResult.isMatched(),
                    "timeMs", System.currentTimeMillis() - start,
                    "operations", nfaResult.getBacktrackCount()
            ));
        } catch (Exception e) {
            results.put("nfa", Map.of("error", e.getMessage()));
        }

        // DFA
        try {
            long start = System.currentTimeMillis();
            DFAMatcher dfaMatcher = new DFAMatcher(compiled.getAst(), config.getTimeoutMs());
            long constructionTime = dfaMatcher.getDfaConstructionTimeMs();
            MatchResult dfaResult = dfaMatcher.matchFull(input);
            results.put("dfa", Map.of(
                    "matched", dfaResult.isMatched(),
                    "timeMs", System.currentTimeMillis() - start,
                    "constructionTimeMs", constructionTime,
                    "stateCount", dfaMatcher.getStateCount(),
                    "operations", dfaResult.getBacktrackCount()
            ));
        } catch (Exception e) {
            results.put("dfa", Map.of("error", e.getMessage()));
        }

        return results;
    }

    // ===== Cache Management =====

    public void clearCache() {
        if (patternCache != null) {
            patternCache.clear();
            log.info("Pattern cache cleared");
        }
    }

    public int getCacheSize() {
        return patternCache != null ? patternCache.size() : 0;
    }

    // ===== Metrics =====

    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalMatches", totalMatches.get());
        metrics.put("successfulMatches", successfulMatches.get());
        metrics.put("failedMatches", failedMatches.get());
        metrics.put("totalMatchTimeMs", totalMatchTimeMs.get());
        metrics.put("compilations", compilations.get());
        metrics.put("cacheSize", getCacheSize());
        metrics.put("cacheMaxSize", config.getCacheMaxSize());

        long total = totalMatches.get();
        if (total > 0) {
            metrics.put("successRate", (double) successfulMatches.get() / total);
            metrics.put("averageMatchTimeMs", (double) totalMatchTimeMs.get() / total);
        }

        return metrics;
    }

    public void resetMetrics() {
        totalMatches.set(0);
        successfulMatches.set(0);
        failedMatches.set(0);
        totalMatchTimeMs.set(0);
        compilations.set(0);
    }

    // ===== Private Methods =====

    private void validatePattern(String pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null");
        }
        if (pattern.length() > config.getMaxPatternLength()) {
            throw new IllegalArgumentException(
                    String.format("Pattern length %d exceeds maximum %d",
                            pattern.length(), config.getMaxPatternLength()));
        }
    }

    private void validateInput(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        if (input.length() > config.getMaxInputLength()) {
            throw new IllegalArgumentException(
                    String.format("Input length %d exceeds maximum %d",
                            input.length(), config.getMaxInputLength()));
        }
    }

    private void updateMetrics(MatchResult result) {
        totalMatches.incrementAndGet();
        if (result.isMatched()) {
            successfulMatches.incrementAndGet();
        } else {
            failedMatches.incrementAndGet();
        }
        totalMatchTimeMs.addAndGet(result.getMatchTimeMs());
    }

    private String buildAstDescription(AstNode node) {
        if (node == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        buildAstDescriptionRecursive(node, sb, 0);
        return sb.toString();
    }

    private void buildAstDescriptionRecursive(AstNode node, StringBuilder sb, int depth) {
        sb.append("  ".repeat(depth));
        sb.append(node.getType());

        if (node.getCharacter() != null) {
            char c = node.getCharacter();
            if (c == '\t') {
                sb.append("('\\t')");
            } else if (c == '\n') {
                sb.append("('\\n')");
            } else if (c == '\0') {
                sb.append("(empty)");
            } else {
                sb.append("('").append(c).append("')");
            }
        }

        if (node.getCharSet() != null && !node.getCharSet().isEmpty()) {
            sb.append(node.getCharSet());
        }

        sb.append("\n");

        for (AstNode child : node.getChildren()) {
            buildAstDescriptionRecursive(child, sb, depth + 1);
        }
    }
}
