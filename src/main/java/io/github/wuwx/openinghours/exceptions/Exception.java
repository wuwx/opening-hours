package io.github.wuwx.openinghours.exceptions;

/**
 * Base exception class for opening hours
 * 
 * @author wuwx
 */
public class Exception extends RuntimeException {
    
    public Exception(String message) {
        super(message);
    }
    
    public Exception(String message, Throwable cause) {
        super(message, cause);
    }
}

