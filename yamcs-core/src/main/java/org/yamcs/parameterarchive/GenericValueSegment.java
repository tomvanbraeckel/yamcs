package org.yamcs.parameterarchive;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.protobuf.Yamcs.Value.Type;
import org.yamcs.utils.VarIntUtil;

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
        super(FORMAT_ID_GenericValueSegment);
    }
    

    @Override
    public void add(int pos, Value v) {
        values.add(pos, v);
    }


    /**
     * Encode using regular protobuf delimited field writes
     */
    @Override
    public void writeTo(ByteBuffer bb) throws IOException {
        VarIntUtil.writeVarint32(bb, values.size());
        for(Value v: values) {
            byte[] b = v.toByteArray();
            VarIntUtil.writeVarint32(bb, b.length);
            bb.put(b);
        }
    }
    /**
     * Decode using regular protobuf delimited field writes
     */
    @Override
    public void parseFrom(ByteBuffer bb) throws IOException {
        int num = VarIntUtil.readVarInt32(bb);
        for(int i=0;i<num; i++) {
            int size = VarIntUtil.readVarInt32(bb);
            byte[] b = new byte[size];
            bb.get(b);
            values.add(Value.parseFrom(b));
        }
    }

    /**
     * Transform this generic segment in one of the specialised versions
     * @return
     */
    public ValueSegment consolidate() {
        if(values.size()==0) return this;

        Type type = values.get(0).getType();
        switch(type) {
        case UINT32:
            return UInt32ValueSegment.consolidate(values);
        case SINT32:
            return SInt32ValueSegment.consolidate(values);
        case STRING:
            return StringValueSegment.consolidate(values);
        case BOOLEAN:
            return BooleanValueSegment.consolidate(values);
        case BINARY:
            
        case DOUBLE:
        case FLOAT:
        case TIMESTAMP:
        case SINT64:
        case UINT64:
        default:
            return this;
        }
    }

    @Override
    public int getMaxSerializedSize() {
        int size = 4*values.size(); //max 4 bytes for storing each value's size
        for(Value v: values) {
            size += v.getSerializedSize();
        }
        return size;
    }

    @Override
    public Value get(int index) {
        return values.get(index);
    }
}
