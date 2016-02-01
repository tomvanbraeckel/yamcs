package org.yamcs.parameterarchive;

import org.yamcs.protobuf.Yamcs.Value;

public interface ValueSegment {
    
    /**
     * Add the parameter value on position pos
     * @param pos
     * @param parameterValue
   
    public void add(int pos, Value value);
    */
    /**
     * returns Value at position index
     */
    public abstract Value get(int index);

}
