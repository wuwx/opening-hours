package io.github.wuwx.openinghours.exceptions;

/**
 * Exception thrown when time ranges overlap
 * 
 * @author wuwx
 */
public class OverlappingTimeRanges extends Exception {
    
    public OverlappingTimeRanges(String message) {
        super(message);
    }
    
    public static OverlappingTimeRanges forRanges(String rangeA, String rangeB) {
        return new OverlappingTimeRanges("Time ranges " + rangeA + " and " + rangeB + " overlap.");
    }
}

