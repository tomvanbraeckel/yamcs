package org.yamcs.parameterarchive;

import org.yamcs.protobuf.Yamcs.Value;

public interface ValueConsumer {
    void addValue(long t, Value v);
}
