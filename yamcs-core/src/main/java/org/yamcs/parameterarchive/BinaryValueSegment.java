package org.yamcs.parameterarchive;

import java.util.List;

import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.utils.DecodingException;
import org.yamcs.utils.ValueUtility;


public class BinaryValueSegment extends ObjectSegment<byte[]> implements ValueSegment {  
    static BinarySerializer serializer = new BinarySerializer();
    
    BinaryValueSegment(boolean buildForSerialisation) {
        super(serializer, buildForSerialisation);
    }


    public static final int MAX_UTF8_CHAR_LENGTH = 3; //I've seen this in protobuf somwhere
    protected List<String> values;
    

    static class BinarySerializer implements ObjectSerializer<byte[]>  {
        @Override
        public byte getFormatId() {
            return BaseSegment.FORMAT_ID_StringValueSegment;
        }

        @Override
        public byte[] deserialize(byte[] b) throws DecodingException {
            return b;
        }

        @Override
        public byte[] serialize(byte[] b) {
            return b;
        }
    }


    @Override
    public Value getValue(int index) {
        return ValueUtility.getBinaryValue(get(index));
    }

    BinaryValueSegment consolidate() {
        return (BinaryValueSegment) super.consolidate();
    }
    
    public static BinaryValueSegment consolidate(List<Value> values) {
        BinaryValueSegment bvs = new BinaryValueSegment(true);
        for(Value v: values) {
            bvs.add(v.getBinaryValue().toByteArray());
        }
        return bvs.consolidate();
    }
}
