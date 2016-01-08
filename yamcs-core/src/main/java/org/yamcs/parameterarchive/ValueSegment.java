package org.yamcs.parameterarchive;

import org.yamcs.protobuf.Yamcs.Value;

public abstract class ValueSegment extends BaseSegment {
    
    ValueSegment(byte formatId) {
        super(formatId);
    }

    /**
     * returns Value at position index
     */
    public abstract Value get(int index);
}
