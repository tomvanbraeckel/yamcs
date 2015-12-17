package org.yamcs.parameterarchive;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.yamcs.ParameterValue;
import org.yamcs.protobuf.Yamcs.Value;

public class GenericValueSegment extends ValueSegment {
    List<Value> values = new ArrayList<Value>();
    
    public GenericValueSegment(int parameterId) {
    }

    @Override
    public void add(int pos, ParameterValue parameterValue) {
        values.add(pos, parameterValue.getEngValue());
    }

    @Override
    public ListIterator<Value> getIterator(int pos) {
        return values.listIterator(pos);
    }
}
