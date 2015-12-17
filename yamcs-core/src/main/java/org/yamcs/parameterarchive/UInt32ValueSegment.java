package org.yamcs.parameterarchive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.ListIterator;

import org.yamcs.protobuf.Yamcs.Value;

public class UInt32ValueSegment extends ValueSegment {
	@Override
	public ListIterator<Value> getIterator(int pos) {
		return null;
	}

	@Override
	public void writeTo(OutputStream stream) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void parseFrom(InputStream stream) throws IOException {
		// TODO Auto-generated method stub
		
	}

	public static UInt32ValueSegment consolidate(List<Value> values) {
		return null;
	}

}
