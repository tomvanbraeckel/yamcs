package org.yamcs.parameterarchive;

public class MultipleParameterValueRequest {
    final int[] parameterIds;
    final int[] parameterGroupIds;
    final long start;
    final long stop;
    final boolean ascending;
    
    public MultipleParameterValueRequest(long start, long stop, int[] parameterIds, int[] parameterGroupIds,  boolean ascending) {
        if(parameterIds.length!=parameterGroupIds.length) throw new IllegalArgumentException("Different number of parameter ids than parameter group ids");
        this.parameterIds = parameterIds;
        this.parameterGroupIds = parameterGroupIds;
        this.start = start;
        this.stop = stop;
        this.ascending = ascending;
    }
    
}
