package org.yamcs.parameterarchive;

import java.nio.ByteBuffer;

import org.yamcs.utils.DecodingException;

/**
 * Base class for all segments of values, timestamps or flags
 * 
 * @author nm
 *
 */
public abstract class BaseSegment {
    
    public static final byte FORMAT_ID_SortedTimeValueSegment = 1;
    
    public static final byte FORMAT_ID_RLEParameterStatusSegment = 2;
    public static final byte FORMAT_ID_BasicParameterStatusSegment = 3;  
    
    public static final byte FORMAT_ID_GenericValueSegment = 10;    
    public static final byte FORMAT_ID_IntValueSegment = 11;
    public static final byte FORMAT_ID_StringValueSegment = 13;
    public static final byte FORMAT_ID_EnumValueSegment = 14;
    public static final byte FORMAT_ID_BooleanValueSegment = 15;
    public static final byte FORMAT_ID_FloatValueSegment = 16;
    public static final byte FORMAT_ID_DoubleValueSegment = 17;
    public static final byte FORMAT_ID_UInt64ValueSegment = 18;
    public static final byte FORMAT_ID_BinaryValueSegment = 19;
    public static final byte FORMAT_ID_BinaryEnumValueSegment = 20;
    
    
    protected byte formatId;
    
    BaseSegment(byte formatId) {
        this.formatId = formatId;
    }
    
  

    public abstract void writeTo(ByteBuffer buf);
    
    public abstract void parseFrom(ByteBuffer buf) throws DecodingException;
    
    /**
     * 
     * @return a high approximation for the serialized size in order to allocate a ByteBuffer big enough
     */
    public abstract int getMaxSerializedSize();
   
    
    /**
     * returns an array containing the values in the range [posStart, posStop) if ascending or [posStop, posStart) if descending
     * 
     * 
     * @param posStart
     * @param posStop
     * @param ascending
     * @return
     */
    public abstract Object getRange(int posStart, int posStop, boolean ascending) ;
    
    public byte getFormatId() {
        return formatId;
    }

    public static BaseSegment newValueSegment(byte formatId, long segmentStart) throws DecodingException {
        switch(formatId) {
        case FORMAT_ID_RLEParameterStatusSegment:
            return new RLEParameterStatusSegment();
        case FORMAT_ID_BasicParameterStatusSegment:
            return new BasicParameterStatusSegment();
        case FORMAT_ID_SortedTimeValueSegment:
            return new SortedTimeSegment(segmentStart);
        case FORMAT_ID_GenericValueSegment:
            return new GenericValueSegment();        
        case FORMAT_ID_IntValueSegment:
            return new IntValueSegment();
        case FORMAT_ID_StringValueSegment:
            return new StringValueSegment();
        case FORMAT_ID_EnumValueSegment:
            return new EnumValueSegment();
        case FORMAT_ID_BooleanValueSegment:
            return new BooleanValueSegment();
        case FORMAT_ID_FloatValueSegment:
            return new FloatValueSegment();
        case FORMAT_ID_DoubleValueSegment:
            return new DoubleValueSegment();
        case FORMAT_ID_UInt64ValueSegment:
            return new UInt64ValueSegment();
        case FORMAT_ID_BinaryEnumValueSegment:
            return new BinaryEnumValueSegment();
        case FORMAT_ID_BinaryValueSegment:
            return new BinaryValueSegment();
        default:
          throw new DecodingException("Invalid value format id "+formatId); 
        }
    }
    
    public abstract int size();
}
