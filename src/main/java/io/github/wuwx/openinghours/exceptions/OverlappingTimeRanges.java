package io.github.wuwx.openinghours.exceptions;

/**
 * Exception thrown when time ranges overlap
 * 
 * @author wuwx
 */
public class OverlappingTimeRanges extends Exception {
    
    /**
     * Constructs a new overlapping time ranges exception
     * 
     * @param message the detail message
     */
    public OverlappingTimeRanges(String message) {
        super(message);
    }
    
    /**
     * Creates a new exception for overlapping ranges
     * 
     * @param rangeA the first time range
     * @param rangeB the second time range
     * @return a new OverlappingTimeRanges instance
     */
    public static OverlappingTimeRanges forRanges(String rangeA, String rangeB) {
        return new OverlappingTimeRanges("Time ranges " + rangeA + " and " + rangeB + " overlap.");
    }
}

