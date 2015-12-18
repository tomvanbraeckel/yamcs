package org.yamcs.parameterarchive;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.utils.ValueUtility;
import org.yamcs.utils.VarIntUtil;

public class SInt32ValueSegment extends ValueSegment {

    
    SInt32ValueSegment() {
        super(FORMAT_ID_SInt32ValueSegment);
    }

    int[] values;
    
    @Override
    public void writeTo(ByteBuffer bb) throws IOException {
        int n = values.length;
        VarIntUtil.writeVarInt32(bb, n);
        for(int i=0; i<n; i++) {
            VarIntUtil.writeSignedVarint32(bb, values[i]);
        }
    }

    @Override
    public void parseFrom(ByteBuffer bb) throws IOException {
        int n = VarIntUtil.readVarInt32(bb);
        values = new int[n];
        for(int i=0; i<n; i++) {
            values[i]=VarIntUtil.readSignedVarInt32(bb);
        }
    }

    @Override
    public int getMaxSerializedSize() {
        return 4+4*values.length; //4 for the size plus 4 for each element
    }


    @Override
    public Value get(int index) {
        return ValueUtility.getUint32Value(values[index]);
    }
	
    public static ValueSegment consolidate(List<Value> values) {
        SInt32ValueSegment segment = new SInt32ValueSegment();
        int n = values.size();
        segment.values = new int[n];
        for(int i =0;i<n; i++) {
            segment.values[i] = values.get(i).getSint32Value();
        }
        return segment;
    }
}
