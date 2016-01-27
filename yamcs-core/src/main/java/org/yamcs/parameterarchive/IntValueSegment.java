package org.yamcs.parameterarchive;

import java.nio.ByteBuffer;
import java.util.List;

import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.utils.DecodingException;
import org.yamcs.utils.ValueUtility;
import org.yamcs.utils.VarIntUtil;

/**
 * 32 bit integers  
 * encoded as deltas of deltas (good if the values are relatively constant or in increasing order)
 *  
 * @author nm
 *
 */
public class IntValueSegment extends ValueSegment {
    private boolean signed;
    int[] values;
    
    IntValueSegment() {
        super(FORMAT_ID_IntValueSegment);
    }


    
    
    @Override
    public void writeTo(ByteBuffer bb) {
        bb.put(signed?(byte)1:0);
        
        int n = values.length;
        int[] ddz = VarIntUtil.encodeDeltaDeltaZigZag(values);
        
        VarIntUtil.writeVarInt32(bb, n);
        for(int i=0; i<n; i++) {
            VarIntUtil.writeVarInt32(bb, ddz[i]);
        }
    }

    @Override
    public void parseFrom(ByteBuffer bb) throws DecodingException {
        signed = (bb.get()==1);
        int n = VarIntUtil.readVarInt32(bb);
        int[] ddz = new int[n];
        for(int i=0; i<n; i++) {
            ddz[i]=VarIntUtil.readVarInt32(bb);
        }
        values = VarIntUtil.decodeDeltaDeltaZigZag(ddz);
    }

    public static IntValueSegment  consolidate(List<Value> values, boolean signed) {
        IntValueSegment segment = new IntValueSegment();
        int n = values.size();
        segment.values = new int[n];
        segment.signed = signed;
        if(signed) {
            for(int i =0;i<n; i++) {
                segment.values[i] = values.get(i).getSint32Value();
            }    
        } else {
            for(int i =0;i<n; i++) {
                segment.values[i] = values.get(i).getUint32Value();
            }    
        }
        
        return segment;
    }
    

    @Override
    public int getMaxSerializedSize() {
        return 4+5*values.length; //4 for the size plus 5 for each element
    }


    @Override
    public Value get(int index) {
        if(signed) {
            return ValueUtility.getSint32Value(values[index]);
        } else {
            return ValueUtility.getUint32Value(values[index]);
        }
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

    @Override
    public int size() {
        return values.length;
    }
}
