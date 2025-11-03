package io.github.wuwx.openinghours.exceptions;

/**
 * Exception thrown when maximum limit is exceeded during time search
 * 
 * @author wuwx
 */
public class MaximumLimitExceeded extends Exception {
    
    public MaximumLimitExceeded(String message) {
        super(message);
    }
    
    public static MaximumLimitExceeded forString(String message) {
        return new MaximumLimitExceeded(message);
    }
}

