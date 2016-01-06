package org.yamcs.parameterarchive;

import java.nio.ByteBuffer;
import java.util.List;

import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.utils.ValueUtility;
import org.yamcs.utils.VarIntUtil;


public class FloatValueSegment extends ValueSegment {
    FloatValueSegment() {
        super(FORMAT_ID_FloatValueSegment);
    }

    float[] floats;
            
    @Override
    public void writeTo(ByteBuffer bb) {
        int n = floats.length;
        VarIntUtil.writeVarInt32(bb, n);
        for(int i=0;i<n;i++) {
            bb.putFloat(floats[i]);
        }
    }

    @Override
    public void parseFrom(ByteBuffer bb) {
        int n = VarIntUtil.readVarInt32(bb);
        floats = new float[n];
        
        for(int i=0;i<n;i++) {
            floats[i]= bb.getFloat();
        }
    }


    @Override
    public Value get(int index) {
        return ValueUtility.getFloatValue(floats[index]);
    }

    @Override
    public int getMaxSerializedSize() {
        return 4+4*floats.length;
    }
    
    static FloatValueSegment consolidate(List<Value> values) {
        FloatValueSegment fvs = new FloatValueSegment();
        int n = values.size();
        fvs.floats = new float[n];
        for(int i=0; i<n; i++) {
            fvs.floats[i] = values.get(i).getFloatValue();
        }
        return fvs;
    }
}
