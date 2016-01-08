package org.yamcs.parameterarchive;

import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.List;

import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.utils.ValueUtility;
import org.yamcs.utils.VarIntUtil;

public class BooleanValueSegment extends ValueSegment {
    BitSet bitSet;
    
    
    BooleanValueSegment() {
        super(FORMAT_ID_BooleanValueSegment);
    }

    @Override
    public void writeTo(ByteBuffer bb) {
        long[]la = bitSet.toLongArray();
        VarIntUtil.writeVarInt32(bb, la.length);
        
        for(long l:la) {
            bb.putLong(l);
        }
    }

    @Override
    public void parseFrom(ByteBuffer bb) {
        int n = VarIntUtil.readVarInt32(bb);
        long[]la = new long[n];
        for(int i=0; i<n; i++) {
            la[i]=bb.getLong();
        }
        bitSet = BitSet.valueOf(la);
    }

    @Override
    public int getMaxSerializedSize() {
        return 4+bitSet.size()/8;
    }

    @Override
    public Value get(int index) {
        return ValueUtility.getBooleanValue(bitSet.get(index));
    }
    
    static BooleanValueSegment consolidate(List<Value> values) {
        BooleanValueSegment bvs = new BooleanValueSegment();
        int n = values.size();
        
        bvs.bitSet = new BitSet(n);
        for(int i=0; i<n; i++) {
            bvs.bitSet.set(i, values.get(i).getBooleanValue());
        }
        return bvs;
    }

    @Override
    public boolean[] getRange(int posStart, int posStop, boolean ascending) {
        boolean[] r = new boolean[posStop-posStart];
        if(ascending) {
            for(int i = posStart; i<posStop; i++) {
                r[i-posStart] = bitSet.get(i);
            }
        } else {
            for(int i = posStop; i>posStart; i--) {
                r[posStop-i] = bitSet.get(i);
            }
        }
        
        return r;
    }
    
}
