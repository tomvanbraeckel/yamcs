package org.yamcs.parameterarchive;

import java.nio.ByteBuffer;
import java.util.List;

import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.utils.DecodingException;
import org.yamcs.utils.ValueUtility;
import org.yamcs.utils.VarIntUtil;

public class UInt64ValueSegment extends ValueSegment {
    UInt64ValueSegment() {
        super(FORMAT_ID_UInt64ValueSegment);
    }


    long[] values;
    
    @Override
    public void writeTo(ByteBuffer bb) {
        int n = values.length;
        VarIntUtil.writeVarInt32(bb, n);
        for(int i=0; i<n; i++) {
            VarIntUtil.writeVarInt64(bb, values[i]);
        }
    }

    @Override
    public void parseFrom(ByteBuffer bb) throws DecodingException {
        int n = VarIntUtil.readVarInt32(bb);
        values = new long[n];
        for(int i=0; i<n; i++) {
            values[i]=VarIntUtil.readVarInt64(bb);
        }
    }

    public static UInt64ValueSegment  consolidate(List<Value> values) {
        UInt64ValueSegment segment = new UInt64ValueSegment();
        int n = values.size();
        segment.values = new long[n];
        for(int i =0;i<n; i++) {
            segment.values[i] = values.get(i).getUint64Value();
        }
        return segment;
    }

    @Override
    public int getMaxSerializedSize() {
        return 4+8*values.length; //4 for the size plus 8 for each element
    }

    @Override
    public long[] getRange(int posStart, int posStop, boolean ascending) {
        long[] r = new long[posStop-posStart];
        if(ascending) {
            for(int i = posStart; i<posStop; i++) {
                r[i-posStart] = values[i];
            }
        } else {
            for(int i = posStop; i>posStart; i--) {
                r[posStop-i] = values[i];
            }
        }
        
        return r;
    }

    @Override
    public Value get(int index) {
        return ValueUtility.getUint64Value(values[index]);
    }
    
}
