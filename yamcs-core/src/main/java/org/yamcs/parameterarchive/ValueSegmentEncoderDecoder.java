package org.yamcs.parameterarchive;

import java.nio.ByteBuffer;

public class ValueSegmentEncoderDecoder {
    final byte VERSION =1;
    
    public byte[] encode(ValueSegment valueSegment) {
        ByteBuffer bb = ByteBuffer.allocate(2+valueSegment.getMaxSerializedSize());
        bb.put(VERSION);
        bb.put(valueSegment.getFormatId());
        valueSegment.writeTo(bb);
        if(bb.position()<bb.capacity()) {
            int length = bb.position();
            byte[] v = new byte[length];
            bb.rewind();
            bb.get(v, 0, length);
            return v;
        } else {
            return bb.array();
        }
    }
    
    
    public ValueSegment decode(byte[] buf, long segmentStart) throws DecodingException {
        ByteBuffer bb = ByteBuffer.wrap(buf);
        byte version = bb.get();
        if(version!=VERSION) {
            throw new DecodingException("Version of ValueSegment is "+version+" instead of expected "+VERSION);
        }
     
        byte formatId = bb.get();
        
        ValueSegment vs = ValueSegment.newValueSegment(formatId, segmentStart);
        vs.parseFrom(bb);
        return vs;
    }
}
