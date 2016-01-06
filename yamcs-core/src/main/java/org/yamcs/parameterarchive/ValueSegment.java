package org.yamcs.parameterarchive;

import java.nio.ByteBuffer;

import org.yamcs.protobuf.Yamcs.Value;

/**
 * An array of values for a parameter.
 * 
 * @author nm
 *
 */
public abstract class ValueSegment {
    public static final byte FORMAT_ID_SortedTimeValueSegment = 1;
    public static final byte FORMAT_ID_GenericValueSegment = 2;
    public static final byte FORMAT_ID_SInt32ValueSegment = 3;
    public static final byte FORMAT_ID_UInt32ValueSegment = 4;
    public static final byte FORMAT_ID_StringValueSegment = 5;
    public static final byte FORMAT_ID_EnumValueSegment = 6;
    public static final byte FORMAT_ID_BooleanValueSegment = 7;
    public static final byte FORMAT_ID_FloatValueSegment = 8;
    public static final byte FORMAT_ID_DoubleValueSegment = 9;
    public static final byte FORMAT_ID_UInt64ValueSegment = 10;
    
    
    protected byte formatId;
    
    ValueSegment(byte formatId) {
        this.formatId = formatId;
    }
    
    /**
     * Add the parameter value on position pos
     * @param pos
     * @param parameterValue
     */
    public void add(int pos, Value value) {
    	throw new UnsupportedOperationException();
    }

    public abstract void writeTo(ByteBuffer buf);
    
    public abstract void parseFrom(ByteBuffer buf) throws DecodingException;
    
    /**
     * 
     * @return a high approximation for the serialized size in order to allocate a ByteBuffer big enough
     */
    public abstract int getMaxSerializedSize();
    
    /**
     * returns Value at position index
     */
    public abstract Value get(int index);

    public byte getFormatId() {
        return formatId;
    }
}
