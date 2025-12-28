package com.rickm.regex.engine.exception;

/**
 * Exception thrown when a regex matching operation fails.
 */
public class RegexMatchException extends RuntimeException {
    
    private final String pattern;
    private final String input;
    private final String reason;
    
    /**
     * Creates a new match exception.
     * 
     * @param message the error message
     * @param pattern the regex pattern
     * @param input the input string
     */
    public RegexMatchException(String message, String pattern, String input) {
        super(formatMessage(message, pattern, input));
        this.pattern = pattern;
        this.input = input;
        this.reason = message;
    }
    
    /**
     * Creates a new match exception with a cause.
     * 
     * @param message the error message
     * @param pattern the regex pattern
     * @param input the input string
     * @param cause the underlying cause
     */
    public RegexMatchException(String message, String pattern, String input, Throwable cause) {
        super(formatMessage(message, pattern, input), cause);
        this.pattern = pattern;
        this.input = input;
        this.reason = message;
    }
    
    private static String formatMessage(String message, String pattern, String input) {
        String truncatedInput = input.length() > 50 
                ? input.substring(0, 50) + "..." 
                : input;
        return String.format("Regex match error: %s\nPattern: %s\nInput: %s", 
                message, pattern, truncatedInput);
    }
    
    public String getPattern() {
        return pattern;
    }
    
    public String getInput() {
        return input;
    }
    
    public String getReason() {
        return reason;
    }
}
