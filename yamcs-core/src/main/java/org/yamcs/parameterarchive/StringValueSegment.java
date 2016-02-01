package org.yamcs.parameterarchive;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.utils.DecodingException;
import org.yamcs.utils.ValueUtility;
import org.yamcs.utils.VarIntUtil;


public class StringValueSegment extends BaseSegment implements ValueSegment {
    public static final int MAX_UTF8_CHAR_LENGTH = 3; //I've seen this in protobuf somwhere
    protected List<String> values;
    
    
    protected StringValueSegment(List<String> values, byte formatId) {
        super(formatId);
        this.values = values;
    }

    
    public StringValueSegment(List<String> values) {
        super(FORMAT_ID_StringValueSegment);
        this.values = values;
    }



    protected StringValueSegment(byte formatId) {
       super(formatId);
    }


    public StringValueSegment() {
        super(FORMAT_ID_StringValueSegment);
    }


    @Override
    public void writeTo(ByteBuffer bb) {
        VarIntUtil.writeVarInt32(bb, values.size());
        for(String v:values) {
            VarIntUtil.writeSizeDelimitedString(bb, v);
        }
    }

    @Override
    public void parseFrom(ByteBuffer bb) throws DecodingException {
        int n = VarIntUtil.readVarInt32(bb);
        values = new ArrayList<String>(n);
        for(int i=0; i<n; i++) {
            values.add(VarIntUtil.readSizeDelimitedString(bb));
        }
    }

    @Override
    public int getMaxSerializedSize() {
        int size = 4+4*values.size(); //4 for the array length, plus 4 for each value size
        for(String v:values) {
            size+=v.length()*MAX_UTF8_CHAR_LENGTH;
        }
        return size;
    }


    @Override
    public Value get(int index) {
        return ValueUtility.getStringValue(values.get(index));
    }

    @Override
    public String[] getRange(int posStart, int posStop, boolean ascending) {
        String[] r = new String[posStop-posStart];
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
    public static StringValueSegment consolidate(List<Value> values) {
        List<String> slist = new ArrayList<String>(values.size());
        
        for(Value v:values) {
            slist.add(v.getStringValue());
        }
        EnumValueSegment evs = new EnumValueSegment(slist);
        StringValueSegment svs = new StringValueSegment(slist);
        
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
