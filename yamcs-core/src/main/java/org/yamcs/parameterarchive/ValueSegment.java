package org.yamcs.parameterarchive;

import org.yamcs.protobuf.Yamcs.Value;

public abstract class ValueSegment extends BaseSegment {
    
    ValueSegment(byte formatId) {
        super(formatId);
    }
    
    /**
     * Add the parameter value on position pos
     * @param pos
     * @param parameterValue
     */
    public void add(int pos, Value value) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * returns Value at position index
     */
    public abstract Value get(int index);

}
