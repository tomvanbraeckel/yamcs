package org.yamcs.parameterarchive;

import java.util.ArrayList;

import org.yamcs.protobuf.Yamcs.Value;

class MyValueConsummer implements ValueConsumer {
    ArrayList<Long> times = new ArrayList<>();
    ArrayList<Value> values = new ArrayList<>();

    @Override
    public void addValue(long t, Value v) {
        times.add(t);
        values.add(v);
    }

}