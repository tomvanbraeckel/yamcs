package org.yamcs.parameterarchive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ListIterator;

import org.yamcs.protobuf.Yamcs.Value;

/**
 * An array of values for a parameter.
 * 
 * @author nm
 *
 */
public abstract class ValueSegment {
    /**
     * Add the parameter value on position pos
     * @param pos
     * @param parameterValue
     */
    public void add(int pos, Value value) {
    	throw new UnsupportedOperationException();
    }

    public abstract ListIterator<Value> getIterator(int pos);
    
    public abstract void writeTo(OutputStream stream) throws IOException;
    
    public abstract void parseFrom(InputStream stream) throws IOException;
}
