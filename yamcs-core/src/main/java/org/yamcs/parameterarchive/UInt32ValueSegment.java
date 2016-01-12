package org.yamcs.parameterarchive;

import java.nio.ByteBuffer;
import java.util.List;

import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.utils.DecodingException;
import org.yamcs.utils.ValueUtility;
import org.yamcs.utils.VarIntUtil;

public class UInt32ValueSegment extends ValueSegment {
    UInt32ValueSegment() {
        super(FORMAT_ID_UInt32ValueSegment);
    }


    int[] values;
    
    @Override
    public void writeTo(ByteBuffer bb) {
        int n = values.length;
        VarIntUtil.writeVarInt32(bb, n);
        for(int i=0; i<n; i++) {
            VarIntUtil.writeVarInt32(bb, values[i]);
        }
    }

    @Override
    public void parseFrom(ByteBuffer bb) throws DecodingException {
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
        return 4+5*values.length; //4 for the size plus 5 for each element
    }


    @Override
    public Value get(int index) {
        return ValueUtility.getUint32Value(values[index]);
    }
    
    @Override
    public int[] getRange(int posStart, int posStop, boolean ascending) {
        int[] r = new int[posStop-posStart];
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
}
