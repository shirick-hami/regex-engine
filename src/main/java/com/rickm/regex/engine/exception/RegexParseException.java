package com.rickm.regex.engine.exception;

/**
 * Exception thrown when a regex pattern cannot be parsed.
 */
public class RegexParseException extends RuntimeException {
    
    private final String pattern;
    private final int position;
    private final String details;
    
    /**
     * Creates a new parse exception.
     * 
     * @param message the error message
     * @param pattern the pattern being parsed
     * @param position the position where parsing failed
     */
    public RegexParseException(String message, String pattern, int position) {
        super(formatMessage(message, pattern, position));
        this.pattern = pattern;
        this.position = position;
        this.details = message;
    }
    
    /**
     * Creates a new parse exception with a cause.
     * 
     * @param message the error message
     * @param pattern the pattern being parsed
     * @param position the position where parsing failed
     * @param cause the underlying cause
     */
    public RegexParseException(String message, String pattern, int position, Throwable cause) {
        super(formatMessage(message, pattern, position), cause);
        this.pattern = pattern;
        this.position = position;
        this.details = message;
    }
    
    private static String formatMessage(String message, String pattern, int position) {
        StringBuilder sb = new StringBuilder();
        sb.append("Regex parse error: ").append(message);
        sb.append("\nPattern: ").append(pattern);
        if (position >= 0 && position <= pattern.length()) {
            sb.append("\n         ");
            sb.append(" ".repeat(position));
            sb.append("^");
        }
        sb.append("\nPosition: ").append(position);
        return sb.toString();
    }
    
    public String getPattern() {
        return pattern;
    }
    
    public int getPosition() {
        return position;
    }
    
    public String getDetails() {
        return details;
    }
}
