package org.yamcs.parameterarchive;

public class SingleParameterValueRequest {
    long start, stop;
    int[] parameterGroupIds;
    int parameterId;
    boolean ascending;
    
    public SingleParameterValueRequest(long start, long stop, int parameterId, int[] parameterGroupIds, boolean ascending) {
        super();
        this.start = start;
        this.stop = stop;
        this.parameterGroupIds = parameterGroupIds;
        this.parameterId = parameterId;
        this.ascending = ascending;
    }
    public SingleParameterValueRequest(long start, long stop, int parameterId, int parameterGroupId, boolean ascending) {
        this(start, stop, parameterId, new int[] { parameterGroupId}, ascending);
    }
}
