package org.yamcs.parameterarchive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.protobuf.Yamcs.Value.Type;

/**
 * GenericValueSegment keeps an ArrayList of Values. 
 * It is used during the archive buildup and as a catch all for non optimized ValueSegments.
 * 
 * 
 * 
 */
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

    /**
     * Encode using regular protobuf delimited field writes
     */
	@Override
	public void writeTo(OutputStream stream) throws IOException {
		for(Value v: values) {
			v.writeDelimitedTo(stream);
		}
	}
	/**
     * Decode using regular protobuf delimited field writes
     */
	@Override
	public void parseFrom(InputStream stream) throws IOException {
		while(stream.available()>0) {
			values.add(Value.parseDelimitedFrom(stream));
		}
	}
	
	public ValueSegment consolidate() {
		if(values.size()==0) return this;
		
		Type type = values.get(0).getType();
		switch(type) {
			case UINT32:
				return UInt32ValueSegment.consolidate(values);
			case SINT32:
				return SInt32ValueSegment.consolidate(values);
				
			default:
				return this;
		}
	}
}
