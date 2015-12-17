package org.yamcs.parameterarchive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    public void add(int pos, Value v) {
        values.add(pos, v);
    }

    @Override
    public ListIterator<Value> getIterator(int pos) {
        return values.listIterator(pos);
    }

	@Override
	public void writeTo(OutputStream stream) throws IOException {
		for(Value v: values) {
			v.writeDelimitedTo(stream);
		}
	}
	
	@Override
	public void parseFrom(InputStream stream) throws IOException {
		while(stream.available()>0) {
			values.add(Value.parseDelimitedFrom(stream));
		}
	}
}
