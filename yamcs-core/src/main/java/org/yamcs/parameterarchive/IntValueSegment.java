package org.yamcs.parameterarchive;

import java.nio.ByteBuffer;
import java.util.List;

import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.utils.DecodingException;
import org.yamcs.utils.ValueUtility;
import org.yamcs.utils.VarIntUtil;

import me.lemire.integercompression.FastPFOR128;
import me.lemire.integercompression.IntWrapper;
import me.lemire.integercompression.VariableByte;

/**
 * 32 bit integers  
 * encoded as deltas of deltas (good if the values are relatively constant or in increasing order)
 *  
 * @author nm
 *
 */
public class IntValueSegment extends BaseSegment implements ValueSegment {
    final static int SUBFORMAT_ID_RAW = 0; //uncompresed
    final static int SUBFORMAT_ID_DELTAZG_FPF128_VB = 1; //compressed with DeltaZigzag and then FastPFOR128 plus VariableByte for remaining
    final static int SUBFORMAT_ID_DELTAZG_VB = 2; //compressed with DeltaZigzag plus VariableByte

    private boolean signed;
    int[] values;

    IntValueSegment() {
        super(FORMAT_ID_IntValueSegment);
    }




    @Override
    public void writeTo(ByteBuffer bb) {
        int position = bb.position();
        //try first to write compressed, if we fail (for random data we may exceed the buffer) then write in raw format
        try {
            writeCompressed(bb);
        } catch (IndexOutOfBoundsException e) {
            bb.position(position);
            writeRaw(bb);
        }
    }
    
    public void writeCompressed(ByteBuffer bb) {
        int[] ddz = VarIntUtil.encodeDeltaDeltaZigZag(values);
        
        FastPFOR128 fastpfor = FastPFORFactory.get();
        VariableByte vb = new VariableByte();
        
        IntWrapper inputoffset = new IntWrapper(0);
        IntWrapper outputoffset = new IntWrapper(0);
        int[] xc = new int[ddz.length];
        
        fastpfor.compress(ddz, inputoffset, ddz.length, xc, outputoffset);
        if (outputoffset.get() == 0) { 
            //fastpfor didn't compress anything, probably there were too few datapoints
            writeHeader(SUBFORMAT_ID_DELTAZG_VB, bb);
        } else {
            writeHeader(SUBFORMAT_ID_DELTAZG_FPF128_VB, bb);
        }
        int remaining =  ddz.length - inputoffset.get();
        vb.compress(ddz, inputoffset, remaining, xc, outputoffset);
        int length = outputoffset.get();
        for(int i=0; i<length; i++) {
            bb.putInt(xc[i]);
        }
    }
    
    
    private void writeRaw(ByteBuffer bb) {
        writeHeader(SUBFORMAT_ID_RAW, bb);
        int n = values.length;
        for(int i=0; i<n; i++) {
            bb.putInt(values[i]);
        }
    }
    //write header:
    // 1st byte:    spare    signed/unsigned subformatid
    //              3 bits   1 bit           4 bits
    // 2+st bytes:   varint of n
    private void writeHeader(int subFormatId, ByteBuffer bb) {
        int x = signed?1:0;
        x=(x<<4)|subFormatId;
        bb.put((byte)x);
        VarIntUtil.writeVarInt32(bb, values.length);
    }
    
    @Override
    public void parseFrom(ByteBuffer bb) throws DecodingException {
        
        byte x = bb.get();
        int subFormatId = x&0xF;
        signed = (((x>>4)&1)==1);
        int n = VarIntUtil.readVarInt32(bb);
        
        switch(subFormatId) {
            case SUBFORMAT_ID_RAW:
               parseRaw(bb, n);
               break;
            case SUBFORMAT_ID_DELTAZG_FPF128_VB: //intentional fall through
            case SUBFORMAT_ID_DELTAZG_VB: 
                parseCompressed(bb, n, subFormatId);
                break;
            default:
                throw new DecodingException("Unknown subformatId: "+subFormatId);
        }   
    }

    private void parseRaw(ByteBuffer bb, int n) {
        values = new int[n];
        for(int i =0;i<n; i++) {
            values[i] = bb.getInt();
        }
    }
    
    private void parseCompressed(ByteBuffer bb, int n, int subFormatId) throws DecodingException {
        int[] x = new int[(bb.limit()-bb.position())/4];
        for(int i=0; i<x.length;i++) {
            x[i]=bb.getInt();
        }
        IntWrapper inputoffset = new IntWrapper(0);
        IntWrapper outputoffset = new IntWrapper(0);
        int[] ddz = new int[n];
        
        if(subFormatId==SUBFORMAT_ID_DELTAZG_FPF128_VB) {
            FastPFOR128 fastpfor = FastPFORFactory.get();
            fastpfor.uncompress(x, inputoffset, x.length, ddz, outputoffset);
        }
        VariableByte vb = new VariableByte();
        vb.uncompress(x, inputoffset, x.length-inputoffset.get(), ddz, outputoffset);
        if(outputoffset.get()!=n) {
            throw new DecodingException("Invalid number of elements decoded; expecting: "+n+" decoded: "+outputoffset.get());
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
    public Value getValue(int index) {
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
