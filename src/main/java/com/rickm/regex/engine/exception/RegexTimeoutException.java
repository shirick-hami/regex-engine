package com.rickm.regex.engine.exception;

/**
 * Exception thrown when a regex operation times out.
 */
public class RegexTimeoutException extends RegexMatchException {
    
    private final long timeoutMs;
    private final long elapsedMs;
    
    /**
     * Creates a new timeout exception.
     * 
     * @param pattern the regex pattern
     * @param input the input string
     * @param timeoutMs the configured timeout in milliseconds
     * @param elapsedMs the actual elapsed time
     */
    public RegexTimeoutException(String pattern, String input, long timeoutMs, long elapsedMs) {
        super(String.format("Regex operation timed out (timeout: %dms, elapsed: %dms)", 
              timeoutMs, elapsedMs), pattern, input);
        this.timeoutMs = timeoutMs;
        this.elapsedMs = elapsedMs;
    }
    
    public long getTimeoutMs() {
        return timeoutMs;
    }
    
    public long getElapsedMs() {
        return elapsedMs;
    }
}
