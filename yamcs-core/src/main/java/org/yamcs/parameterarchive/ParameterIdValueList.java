package org.yamcs.parameterarchive;

import java.util.List;

import org.python.antlr.runtime.misc.IntArray;
import org.yamcs.protobuf.Yamcs.Value;

/**
 * A list of parametersIds with values all having the same timestamp
 * @author nm
 *
 */
class ParameterIdValueList {
    final long instant;
    final int parameterGroupId;
    
    IntArray pids;
    List<Value> values;
    
    public ParameterIdValueList(long instant, int parameterGroupId) {
        this.instant = instant;
        this.parameterGroupId = parameterGroupId;
    }
    
    public void add(int parameterId, Value v) {
        pids.add(parameterId);
        values.add(v);
    }
}