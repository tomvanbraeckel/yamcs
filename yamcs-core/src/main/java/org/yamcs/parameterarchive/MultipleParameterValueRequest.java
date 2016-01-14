package org.yamcs.parameterarchive;

import org.yamcs.protobuf.Yamcs.NamedObjectId;

public class MultipleParameterValueRequest {
    final NamedObjectId[] parameterNames;
    final int[] parameterIds;
    final int[] parameterGroupIds;
    final long start;
    final long stop;
    final boolean ascending;
    
    //these shall also be considered final - just that I didn't want the constructor to get very long
    boolean retrieveRawValues = false;
    boolean retrieveEngValues = true;
    boolean retrieveParamStatus = true;
    int limit = -1;
    
    public MultipleParameterValueRequest(long start, long stop, NamedObjectId[] parameterNames, int[] parameterIds, int[] parameterGroupIds,  boolean ascending) {
        if(parameterGroupIds.length!=parameterIds.length) throw new IllegalArgumentException("Different number of parameter ids than parameter group ids");
        if(parameterNames.length!=parameterIds.length) throw new IllegalArgumentException("Different number of parameter names than parameter ids");
        
        
        this.parameterNames = parameterNames;
        this.parameterIds = parameterIds;
        this.parameterGroupIds = parameterGroupIds;
        this.start = start;
        this.stop = stop;
        this.ascending = ascending;
    }
    
    /**
     * retrieve a limited number of "lines"
     * negative means no limit
     * @param limit
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }
}
