package com.rickm.regex.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTOs for the Regex Engine API.
 */
public class RegexResponse {

    /**
     * Response for pattern compilation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Response from pattern compilation")
    public static class CompileResponse {

        @Schema(description = "Whether compilation was successful")
        private boolean valid;

        @Schema(description = "The compiled pattern")
        private String pattern;

        @Schema(description = "Time taken to compile in milliseconds")
        private long compileTimeMs;

        @Schema(description = "Human-readable AST description")
        private String astDescription;

        @Schema(description = "Error message if compilation failed")
        private String error;

        @Schema(description = "Position in pattern where error occurred")
        private Integer errorPosition;
    }

    /**
     * Response for matching operations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Response from matching operation")
    public static class MatchResponse {

        @Schema(description = "Whether a match was found")
        private boolean matched;

        @Schema(description = "The pattern used")
        private String pattern;

        @Schema(description = "The input string")
        private String input;

        @Schema(description = "Start index of match (0-based)")
        private int startIndex;

        @Schema(description = "End index of match (exclusive)")
        private int endIndex;

        @Schema(description = "The matched substring")
        private String matchedText;

        @Schema(description = "Number of backtracking steps")
        private long backtrackCount;

        @Schema(description = "Time taken to match in milliseconds")
        private long matchTimeMs;

        @Schema(description = "All matches found (for findAll)")
        private List<MatchInfo> allMatches;

        @Schema(description = "Engine used for matching")
        private String engine;
    }

    /**
     * Information about a single match.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Information about a single match occurrence")
    public static class MatchInfo {

        @Schema(description = "Start index of this match")
        private int startIndex;

        @Schema(description = "End index of this match (exclusive)")
        private int endIndex;

        @Schema(description = "The matched text")
        private String matchedText;
    }

    /**
     * Response for replace operations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Response from replace operation")
    public static class ReplaceResponse {

        @Schema(description = "The original input")
        private String input;

        @Schema(description = "The pattern used")
        private String pattern;

        @Schema(description = "The replacement string")
        private String replacement;

        @Schema(description = "The resulting string after replacement")
        private String result;

        @Schema(description = "Number of replacements made")
        private int replacementCount;
    }

    /**
     * Response for split operations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Response from split operation")
    public static class SplitResponse {

        @Schema(description = "The original input")
        private String input;

        @Schema(description = "The pattern used to split")
        private String pattern;

        @Schema(description = "The resulting parts after splitting")
        private String[] parts;

        @Schema(description = "Number of parts produced")
        private int partCount;
    }

    /**
     * Generic error response.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Error response")
    public static class ErrorResponse {

        @Schema(description = "Error type")
        private String error;

        @Schema(description = "Detailed error message")
        private String message;

        @Schema(description = "Position in pattern where error occurred (if applicable)")
        private Integer position;

        @Schema(description = "HTTP status code")
        private int status;

        @Schema(description = "Timestamp of error")
        private String timestamp;
    }
}
