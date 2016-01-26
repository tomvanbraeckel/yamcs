package org.yamcs.parameterarchive;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yamcs.utils.DecodingException;
import org.yamcs.utils.VarIntUtil;

/**
 * Stores enum values - same as StringValueSegment but assumes that the values are repeating so it assigns an id to each
 * 
 * The encoding consists of:
 *  - number of unique values
 *  - for each unique value: size+value
 *  - number of values
 *  - for each value: the index of the value in the unique value list 
 *     
 *  
 *  TODO: this is copy&paste from EnumValueSegment that stores String instead of byte[]. We should perhaps have a generic class 
 *  
 *  care has to be taken when putting byte[] in HashMap
 *     
 * @author nm
 *
 */
public class BinaryEnumValueSegment extends BinaryValueSegment {
    Map<ByteBuffer, Integer> valuemap;
    List<byte[]> unique;
    
    
    BinaryEnumValueSegment(List<byte[]> values) {
        super(values, FORMAT_ID_BinaryEnumValueSegment);
        unique = new ArrayList<byte[]>();
        valuemap = new HashMap<ByteBuffer, Integer>();
        
        int c=0;
        for(byte[] v:values) {
            ByteBuffer bb = ByteBuffer.wrap(v);
            if(!valuemap.containsKey(bb)) {
                valuemap.put(bb, c++);
                unique.add(v);
            }
        }
    }
    
    

    public BinaryEnumValueSegment() {
        super(FORMAT_ID_EnumValueSegment);
    }



    @Override
    public void writeTo(ByteBuffer bb) {
        int n = unique.size();
        VarIntUtil.writeVarInt32(bb, n);
        for(int i=0;i<n;i++) {
            byte[] u = unique.get(i);
            VarIntUtil.writeVarInt32(bb, u.length);
            bb.put(u);
        }
        n = values.size();
        
        VarIntUtil.writeVarInt32(bb, n);
        for(int i=0;i<n;i++) {
            VarIntUtil.writeVarInt32(bb, valuemap.get(ByteBuffer.wrap(values.get(i))));
        }
    }
    
    
    @Override
    public void parseFrom(ByteBuffer bb) throws DecodingException  {
        int n = VarIntUtil.readVarInt32(bb);
        List<byte[]> unique = new ArrayList<byte[]>(n);
        for(int i=0;i<n;i++) {
            int length = VarIntUtil.readVarInt32(bb);
            if(length>bb.remaining()) throw new DecodingException("Invalid binary length read: "+length);
            byte[] b = new byte[length];
            bb.get(b);
            unique.add(b);
        }
        n = VarIntUtil.readVarInt32(bb);
        values = new ArrayList<byte[]>(n);
        for(int i=0;i<n;i++) {
            int idx = VarIntUtil.readVarInt32(bb);
            values.add(unique.get(idx));
        }
    }
    
    
    @Override
    public int getMaxSerializedSize() {
        int size = 4 + unique.size()*4 + 4 + values.size()*4;
        for(byte[] s: unique) {
            size += s.length;
        }
        return size;
    }
}
