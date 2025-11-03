package io.github.wuwx.openinghours.exceptions;

/**
 * Exception thrown when maximum limit is exceeded during time search
 * 
 * @author wuwx
 */
public class MaximumLimitExceeded extends Exception {
    
    /**
     * Constructs a new maximum limit exceeded exception
     * 
     * @param message the detail message
     */
    public MaximumLimitExceeded(String message) {
        super(message);
    }
    
    /**
     * Creates a new exception with the given message
     * 
     * @param message the detail message
     * @return a new MaximumLimitExceeded instance
     */
    public static MaximumLimitExceeded forString(String message) {
        return new MaximumLimitExceeded(message);
    }
}

