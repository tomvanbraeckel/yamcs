package org.yamcs.parameterarchive;

public class SingleParameterValueRequest {
    long start, stop;
    int parameterGroupId;
    int parameterId;
    boolean ascending;
    ValueConsumer consumer;
    
    public SingleParameterValueRequest(long start, long stop, int parameterGroupId,
            int parameterId, boolean ascending, ValueConsumer consumer) {
        super();
        this.start = start;
        this.stop = stop;
        this.parameterGroupId = parameterGroupId;
        this.parameterId = parameterId;
        this.ascending = ascending;
        this.consumer = consumer;
    }
 
}
