package org.yamcs.parameterarchive;

import java.nio.ByteBuffer;
import java.util.List;

import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.utils.DecodingException;
import org.yamcs.utils.ValueUtility;
import org.yamcs.utils.VarIntUtil;


public class DoubleValueSegment extends ValueSegment {
    double[] doubles;
    
    DoubleValueSegment() {
        super(FORMAT_ID_DoubleValueSegment);
    }
   
            
    @Override
    public void writeTo(ByteBuffer bb) {
        int n = doubles.length;
        VarIntUtil.writeVarInt32(bb, n);
        for(int i=0;i<n;i++) {
            bb.putDouble(doubles[i]);
        }
    }

    @Override
    public void parseFrom(ByteBuffer bb) throws DecodingException {
        int n = VarIntUtil.readVarInt32(bb);
        doubles = new double[n];
        
        for(int i=0;i<n;i++) {
            doubles[i]= bb.getDouble();
        }
    }


    @Override
    public Value get(int index) {
        return ValueUtility.getDoubleValue(doubles[index]);
    }

    @Override
    public int getMaxSerializedSize() {
        return 4+8*doubles.length;
    }
    
    static DoubleValueSegment consolidate(List<Value> values) {
        DoubleValueSegment fvs = new DoubleValueSegment();
        int n = values.size();
        fvs.doubles = new double[n];
        for(int i=0; i<n; i++) {
            fvs.doubles[i] = values.get(i).getDoubleValue();
        }
        return fvs;
    }
    
    @Override
    public double[] getRange(int posStart, int posStop, boolean ascending) {
        double[] r = new double[posStop-posStart];
        if(ascending) {
            for(int i = posStart; i<posStop; i++) {
                r[i-posStart] = doubles[i];
            }
        } else {
            for(int i = posStop; i>posStart; i--) {
                r[posStop-i] = doubles[i];
            }
        }
        
        return r;
    }


    @Override
    public int size() {
        return doubles.length;
    }
}
