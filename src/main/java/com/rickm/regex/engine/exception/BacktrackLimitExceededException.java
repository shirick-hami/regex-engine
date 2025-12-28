package com.rickm.regex.engine.exception;

/**
 * Exception thrown when the backtracking limit is exceeded during matching.
 * 
 * This is a safety mechanism to prevent catastrophic backtracking
 * that could cause excessive CPU usage or denial of service.
 */
public class BacktrackLimitExceededException extends RegexMatchException {
    
    private final long limit;
    private final long actual;
    
    /**
     * Creates a new backtrack limit exceeded exception.
     * 
     * @param pattern the regex pattern
     * @param input the input string
     * @param limit the configured limit
     * @param actual the actual number of backtracks performed
     */
    public BacktrackLimitExceededException(String pattern, String input, long limit, long actual) {
        super(String.format("Backtrack limit exceeded (limit: %d, actual: %d)", limit, actual),
              pattern, input);
        this.limit = limit;
        this.actual = actual;
    }
    
    public long getLimit() {
        return limit;
    }
    
    public long getActual() {
        return actual;
    }
}
