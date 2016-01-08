package org.yamcs.parameterarchive;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yamcs.utils.DecodingException;
import org.yamcs.utils.VarIntUtil;

/**
 * Stores enum values - same as StringValueSegment but assumes that the strings are repeating so it assigns an id to each
 * 
 * The encoding consists of:
 *  - number of unique strings
 *  - for each unique string: size+UTF8 encoded string
 *  - number of values
 *  - for each value: the index of the string in the unique string list 
 *     
 * @author nm
 *
 */
public class EnumValueSegment extends StringValueSegment {
    Map<String, Integer> valuemap;
    List<String> unique;
    
    EnumValueSegment(List<String> values) {
        super(values, FORMAT_ID_EnumValueSegment);
        unique = new ArrayList<String>();
        valuemap = new HashMap<String, Integer>();
        
        int c=0;
        for(String v:values) {
            if(!valuemap.containsKey(v)) {
                valuemap.put(v, c++);
                unique.add(v);
            }
        }
    }
    
    

    public EnumValueSegment() {
        super(FORMAT_ID_EnumValueSegment);
    }



    @Override
    public void writeTo(ByteBuffer bb) {
        int n = unique.size();
        VarIntUtil.writeVarInt32(bb, n);
        for(int i=0;i<n;i++) {
            VarIntUtil.writeSizeDelimitedString(bb, unique.get(i));
        }
        n = values.size();
        
        VarIntUtil.writeVarInt32(bb, n);
        for(int i=0;i<n;i++) {
            VarIntUtil.writeVarInt32(bb, valuemap.get(values.get(i)));
        }
    }
    
    
    @Override
    public void parseFrom(ByteBuffer bb) throws DecodingException  {
        int n = VarIntUtil.readVarInt32(bb);
        List<String> unique = new ArrayList<String>(n);
        for(int i=0;i<n;i++) {
            unique.add(VarIntUtil.readSizeDelimitedString(bb));
        }
        n = VarIntUtil.readVarInt32(bb);
        values = new ArrayList<String>(n);
        for(int i=0;i<n;i++) {
            int idx = VarIntUtil.readVarInt32(bb);
            values.add(unique.get(idx));
        }
    }
    
    
    @Override
    public int getMaxSerializedSize() {
        int size = 4 + unique.size()*4 + 4 + values.size()*4;
        for(String s: unique) {
            size+=s.length()*StringValueSegment.MAX_UTF8_CHAR_LENGTH;
        }
        return size;
    }
    
}
