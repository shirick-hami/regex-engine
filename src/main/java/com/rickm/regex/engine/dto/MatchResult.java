package com.rickm.regex.engine.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Represents the result of a regex matching operation.
 */
@Data
@Builder
public class MatchResult {

    /** Whether a match was found */
    private boolean matched;

    /** The starting index of the match in the input string */
    private int startIndex;

    /** The ending index of the match (exclusive) */
    private int endIndex;

    /** The matched substring */
    private String matchedText;

    /** The original input string */
    private String input;

    /** The regex pattern used */
    private String pattern;

    /** Number of backtracking steps performed */
    private long backtrackCount;

    /** Time taken for matching in milliseconds */
    private long matchTimeMs;

    /** List of all matches found (for findAll operation) */
    private List<MatchInfo> allMatches;

    /**
     * Creates a non-match result.
     *
     * @param input the input string
     * @param pattern the pattern
     * @return a MatchResult indicating no match
     */
    public static MatchResult noMatch(String input, String pattern) {
        return MatchResult.builder()
                .matched(false)
                .startIndex(-1)
                .endIndex(-1)
                .matchedText(null)
                .input(input)
                .pattern(pattern)
                .build();
    }

    /**
     * Creates a successful match result.
     *
     * @param input the input string
     * @param pattern the pattern
     * @param start the start index
     * @param end the end index
     * @return a successful MatchResult
     */
    public static MatchResult match(String input, String pattern, int start, int end) {
        return MatchResult.builder()
                .matched(true)
                .startIndex(start)
                .endIndex(end)
                .matchedText(input.substring(start, end))
                .input(input)
                .pattern(pattern)
                .build();
    }

    /**
     * Information about a single match occurrence.
     */
    @Data
    @Builder
    public static class MatchInfo {
        private int startIndex;
        private int endIndex;
        private String matchedText;
    }
}
