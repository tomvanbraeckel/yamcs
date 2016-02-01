package org.yamcs.parameterarchive;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.utils.DecodingException;
import org.yamcs.utils.ValueUtility;
import org.yamcs.utils.VarIntUtil;


public class BinaryValueSegment extends BaseSegment implements ValueSegment {  
    protected List<byte[]> values;
    
    protected BinaryValueSegment(List<byte[]> values, byte formatId) {
        super(formatId);
        this.values = values;
    }

    
    public BinaryValueSegment(List<byte[]> values) {
        super(FORMAT_ID_StringValueSegment);
        this.values = values;
    }



    protected BinaryValueSegment(byte formatId) {
       super(formatId);
    }


    public BinaryValueSegment() {
        super(FORMAT_ID_BinaryValueSegment);
    }


    @Override
    public void writeTo(ByteBuffer bb) {
        VarIntUtil.writeVarInt32(bb, values.size());
        for(byte[] v:values) {
            VarIntUtil.writeVarInt32(bb, v.length);
            bb.put(v);
        }
    }

    @Override
    public void parseFrom(ByteBuffer bb) throws DecodingException {
        int n = VarIntUtil.readVarInt32(bb);
        values = new ArrayList<byte[]>(n);
        for(int i=0; i<n; i++) {
            int length = VarIntUtil.readVarInt32(bb);
            if(length>bb.remaining()) throw new DecodingException("Invalid size of binary value read: "+length);
            byte[] b = new byte[length];
            bb.get(b);
            values.add(b);
        }
    }

    @Override
    public int getMaxSerializedSize() {
        int size = 4+4*values.size(); //4 for the array length, plus 4 for each value size
        for(byte[] v:values) {
            size+=v.length;
        }
        return size;
    }


    @Override
    public Value get(int index) {
        return ValueUtility.getBinaryValue(values.get(index));
    }

    @Override
    public byte[][] getRange(int posStart, int posStop, boolean ascending) {
        byte[][] r = new byte[posStop-posStart][];
        if(ascending) {
            for(int i = posStart; i<posStop; i++) {
                r[i-posStart] = values.get(i);
            }
        } else {
            for(int i = posStop; i>posStart; i--) {
                r[posStop-i] = values.get(i);
            }
        }
        
        return r;
    }
    public static BinaryValueSegment consolidate(List<Value> values) {
        List<byte[]> slist = new ArrayList<byte[]>(values.size());
        
        for(Value v:values) {
            slist.add(v.getBinaryValue().toByteArray());
        }
        BinaryEnumValueSegment evs = new BinaryEnumValueSegment(slist);
        BinaryValueSegment svs = new BinaryValueSegment(slist);
        
        int evsMaxSize = evs.getMaxSerializedSize();
        int svsMaxSize = svs.getMaxSerializedSize();
        
        if(evsMaxSize<svsMaxSize) return evs;
        else return svs;
    }
    
    @Override
    public String toString() {
        return values.toString();
    }


    @Override
    public int size() {
        return values.size();
    }

}
