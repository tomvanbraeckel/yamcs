package org.yamcs.parameterarchive;

import org.yamcs.protobuf.Yamcs.Value;

public class TimedValue {
    final long instant;
    final Value value;
    public TimedValue(long instant, Value value) {
        this.instant = instant;
        this.value = value;
    }
   
}
