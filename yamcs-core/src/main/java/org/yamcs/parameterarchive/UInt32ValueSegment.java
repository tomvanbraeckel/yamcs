package org.yamcs.parameterarchive;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.utils.ValueUtility;
import org.yamcs.utils.VarIntUtil;

public class UInt32ValueSegment extends ValueSegment {
   
    
    UInt32ValueSegment() {
        super(FORMAT_ID_UInt32ValueSegment);
    }


    int[] values;
    
    @Override
    public void writeTo(ByteBuffer bb) throws IOException {
        int n = values.length;
        VarIntUtil.writeVarint32(bb, n);
        for(int i=0; i<n; i++) {
            VarIntUtil.writeVarint32(bb, values[i]);
        }
    }

    @Override
    public void parseFrom(ByteBuffer bb) throws IOException {
        int n = VarIntUtil.readVarInt32(bb);
        values = new int[n];
        for(int i=0; i<n; i++) {
            values[i]=VarIntUtil.readVarInt32(bb);
        }
    }

    public static UInt32ValueSegment  consolidate(List<Value> values) {
        UInt32ValueSegment segment = new UInt32ValueSegment();
        int n = values.size();
        segment.values = new int[n];
        for(int i =0;i<n; i++) {
            segment.values[i] = values.get(i).getUint32Value();
        }
        return segment;
    }

    @Override
    public int getMaxSerializedSize() {
        return 4+4*values.length; //4 for the size plus 4 for each element
    }


    @Override
    public Value get(int index) {
        return ValueUtility.getUint32Value(values[index]);
    }
    
}
