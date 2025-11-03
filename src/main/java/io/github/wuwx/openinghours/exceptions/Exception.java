package io.github.wuwx.openinghours.exceptions;

/**
 * Base exception class for opening hours
 * 
 * @author wuwx
 */
public class Exception extends RuntimeException {
    
    /**
     * Constructs a new exception with the specified message
     * 
     * @param message the detail message
     */
    public Exception(String message) {
        super(message);
    }
    
    /**
     * Constructs a new exception with the specified message and cause
     * 
     * @param message the detail message
     * @param cause the cause
     */
    public Exception(String message, Throwable cause) {
        super(message, cause);
    }
}

