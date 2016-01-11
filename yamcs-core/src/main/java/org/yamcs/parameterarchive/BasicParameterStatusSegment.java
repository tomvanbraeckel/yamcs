package org.yamcs.parameterarchive;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.yamcs.ParameterValue;
import org.yamcs.protobuf.Pvalue.ParameterStatus;
import org.yamcs.utils.DecodingException;
import org.yamcs.utils.VarIntUtil;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * basic parameter status segment, stores the values in a list
 * @author nm
 *
 */
public class BasicParameterStatusSegment extends AbstractParameterStatusSegment{
    List<ParameterStatus> values = new ArrayList<>();

    public BasicParameterStatusSegment() {
        super(AbstractParameterStatusSegment.FORMAT_ID_BasicParameterStatusSegment);
    }

    @Override
    public void writeTo(ByteBuffer bb) {
        VarIntUtil.writeVarInt32(bb, values.size());
        for(ParameterStatus v:values) {
            byte[] b = v.toByteArray();
            VarIntUtil.writeVarInt32(bb, b.length);
            bb.put(b);
        }
    }

    @Override
    public void parseFrom(ByteBuffer bb) throws DecodingException {
        int n = VarIntUtil.readVarInt32(bb);
        values = new ArrayList<>(n);
        try {
            for(int i =0; i<n; i++) {
                int size = VarIntUtil.readVarInt32(bb);
                byte[] b = new byte[size];
                bb.get(b);
                values.add(ParameterStatus.parseFrom(b));
            }
        } catch (InvalidProtocolBufferException e) {
            throw new DecodingException("Failed to decode parameter status",e);
        }
    }

    @Override
    public int getMaxSerializedSize() {
        int s = 4*values.size();
        for(ParameterStatus ps:values) {
            s+=ps.getSerializedSize();
        }
        return s;
    }

    @Override
    public ParameterStatus[] getRange(int posStart, int posStop, boolean ascending) {
        ParameterStatus[] r = new ParameterStatus[posStop-posStart];
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


    public AbstractParameterStatusSegment consolidate() {
        RLEParameterStatusSegment rle = new RLEParameterStatusSegment();
        for(ParameterStatus ps:values) {
            rle.addParameterStatus(ps);
        }

        int basicMaxSize = getMaxSerializedSize();
        int rleMaxSize = rle.getMaxSerializedSize();

        if(basicMaxSize<=rleMaxSize) return this;
        else return rle;
    }

    public void addParameterValue(int pos, ParameterValue pv) {
        values.add(pos, getStatus(pv));
    }
}
