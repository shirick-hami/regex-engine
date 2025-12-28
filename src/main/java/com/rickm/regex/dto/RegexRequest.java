package com.rickm.regex.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTOs for the Regex Engine API.
 */
public class RegexRequest {

    /**
     * Request for pattern compilation/validation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request to compile/validate a regex pattern")
    public static class CompileRequest {

        @NotBlank(message = "Pattern is required")
        @Schema(description = "The regex pattern to compile", example = "[a-z]+")
        private String pattern;
    }

    /**
     * Request for matching operations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request to match a pattern against input")
    public static class MatchRequest {

        @NotBlank(message = "Pattern is required")
        @Schema(description = "The regex pattern", example = "[a-z]+")
        private String pattern;

        @NotNull(message = "Input is required")
        @Schema(description = "The input string to match against", example = "hello world")
        private String input;
    }

    /**
     * Request for replace operations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request to replace pattern matches")
    public static class ReplaceRequest {

        @NotBlank(message = "Pattern is required")
        @Schema(description = "The regex pattern", example = "[0-9]+")
        private String pattern;

        @NotNull(message = "Input is required")
        @Schema(description = "The input string", example = "Order 123 and Order 456")
        private String input;

        @NotNull(message = "Replacement is required")
        @Schema(description = "The replacement string", example = "XXX")
        private String replacement;
    }

    /**
     * Request for split operations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request to split string by pattern")
    public static class SplitRequest {

        @NotBlank(message = "Pattern is required")
        @Schema(description = "The regex pattern to split by", example = "\\s+")
        private String pattern;

        @NotNull(message = "Input is required")
        @Schema(description = "The input string to split", example = "hello world foo bar")
        private String input;
    }
}
