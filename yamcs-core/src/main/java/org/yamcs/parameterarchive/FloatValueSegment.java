package org.yamcs.parameterarchive;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.List;

import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.utils.DecodingException;
import org.yamcs.utils.ValueUtility;
import org.yamcs.utils.VarIntUtil;


public class FloatValueSegment extends BaseSegment implements ValueSegment {
    final static byte SUBFORMAT_ID_RAW = 0;
    final static byte SUBFORMAT_ID_COMPRESSED = 1;
    FloatValueSegment() {
        super(FORMAT_ID_FloatValueSegment);
    }

    float[] floats;


    @Override
    public void writeTo(ByteBuffer bb) {
        int position = bb.position();

        //try to write it compressed, if we get an buffer overflow, revert to raw encoding
        bb.put(SUBFORMAT_ID_COMPRESSED);
        int n = floats.length;
        VarIntUtil.writeVarInt32(bb, n);

        try {
            FloatCompressor.compress(floats, bb);
        } catch (BufferOverflowException e) {
            bb.position(position);
            writeRaw(bb);
        }
    }

    private void writeRaw(ByteBuffer bb) {
        bb.put(SUBFORMAT_ID_RAW);
        int n = floats.length;
        VarIntUtil.writeVarInt32(bb, n);
        for(int i=0; i<n; i++) {
            bb.putFloat(floats[i]);
        }

    }

    @Override
    public void parseFrom(ByteBuffer bb) throws DecodingException {
        byte b= bb.get();
        int n = VarIntUtil.readVarInt32(bb);
        if(b==SUBFORMAT_ID_RAW) {
            floats = new float[n];
            for(int i=0;i<n;i++) {
                floats[i] = bb.getFloat();
            }	 
        } else if(b==SUBFORMAT_ID_COMPRESSED) {
            floats = FloatCompressor.decompress(bb, n);	
        } else {
            throw new DecodingException("Unknown SUBFORMAT_ID: "+b);
        }


    }


    @Override
    public Value getValue(int index) {
        return ValueUtility.getFloatValue(floats[index]);
    }

    @Override
    public int getMaxSerializedSize() {
        return 5+4*floats.length+1;
    }

    @Override
    public float[] getRange(int posStart, int posStop, boolean ascending) {
        float[] r = new float[posStop-posStart];
        if(ascending) {
            for(int i = posStart; i<posStop; i++) {
                r[i-posStart] = floats[i];
            }
        } else {
            for(int i = posStop; i>posStart; i--) {
                r[posStop-i] = floats[i];
            }
        }

        return r;
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

    @Override
    public int size() {
        return floats.length;
    }
}
