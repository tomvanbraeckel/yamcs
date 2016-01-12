package org.yamcs.parameterarchive;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.yamcs.ParameterValue;
import org.yamcs.protobuf.Pvalue.ParameterStatus;
import org.yamcs.utils.DecodingException;
import org.yamcs.utils.IntArray;
import org.yamcs.utils.VarIntUtil;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Stores parameter status runlength encoded
 * 
 * @author nm
 *
 */
public class RLEParameterStatusSegment extends AbstractParameterStatusSegment {
    //count for each status in the status list - the sum of all counts is equal with size
    IntArray counts;
    List<ParameterStatus> statusList;
    int size = 0;

    RLEParameterStatusSegment() {
        super(FORMAT_ID_RLEParameterStatusSegment);
        statusList = new ArrayList<>();
        counts = new IntArray();
    }


    public void addParameterValue(ParameterValue pv) {
        addParameterStatus(getStatus(pv));
    }
    
    void addParameterStatus(ParameterStatus ps) {
        boolean added = false;

        if(!statusList.isEmpty()) {
            ParameterStatus lastFlag = statusList.get(statusList.size()-1);
            if(ps.equals(lastFlag)) {
                int n = counts.size() -1;
                int lastCount = counts.get(n);
                lastCount++;
                counts.set(n, lastCount);
                added = true;
            }
        }

        if(!added) {
            counts.add(1);
            statusList.add(ps);
        }
        size++;
    }

    @Override
    public void writeTo(ByteBuffer bb) {
        //first write the counts
        VarIntUtil.writeVarInt32(bb, counts.size());
        for(int i=0; i<counts.size(); i++) {
            VarIntUtil.writeVarInt32(bb, counts.get(i));
        }

        //then write the flags
        VarIntUtil.writeVarInt32(bb, statusList.size());
        
        for(int i=0; i<statusList.size(); i++) {
            ParameterStatus ps = statusList.get(i);
            byte[] b= ps.toByteArray();
            VarIntUtil.writeVarInt32(bb, b.length);
            bb.put(b);
        }
    }

    @Override
    public void parseFrom(ByteBuffer bb) throws DecodingException {
        
        int countNum = VarIntUtil.readVarInt32(bb);
        counts = new IntArray(countNum);
        size = 0;

        for(int i=0; i<countNum; i++) {
            int c = VarIntUtil.readVarInt32(bb);
            counts.add(c);
            size+=c;

        }
        
        int statusNum = VarIntUtil.readVarInt32(bb);
        statusList = new ArrayList<>(statusNum);
        try {
            for(int i=0; i<statusNum; i++) {
                int size = VarIntUtil.readVarInt32(bb);
                byte[] b = new byte[size];
                bb.get(b);
                ParameterStatus flag = ParameterStatus.parseFrom(b);
                statusList.add(flag);
            }
        } catch (InvalidProtocolBufferException e) {
            throw new DecodingException("Failed to decode parameter status segment", e);
        }
    }

    @Override
    public int getMaxSerializedSize() {
        int size = 4*counts.size();
        for(ParameterStatus pvf: statusList) {
            size+=pvf.getSerializedSize();
        }
        return size;
    }

    @Override
    public ParameterStatus[] getRange(int posStart, int posStop, boolean ascending) {
        if(posStart>=posStop) throw new IllegalArgumentException("posStart has to be smaller than posStop");
        if(ascending) {
            return getRangeAscending(posStart, posStop);
        } else {
            return getRangeDescending(posStart, posStop);
        }
    }

    public  ParameterStatus[] getRangeAscending(int posStart, int posStop) {

        if(posStop>size) throw new IndexOutOfBoundsException("Index: "+posStop+" size: "+size);

        int n = posStop-posStart;
        ParameterStatus[] r = new ParameterStatus[n];

        int k = posStart;
        int i = 0;
        while(k>=counts.get(i)) {
            k-=counts.get(i++);
        }
        int pos = 0;

        while(pos<n) {
            r[pos++] = statusList.get(i);
            k++;
            if(k>=counts.get(i)) {
                i++;
                k=0;
            }
        }
        return r;
    }

    public  ParameterStatus[] getRangeDescending(int posStart, int posStop) {
        if(posStop>=size) throw new IndexOutOfBoundsException("Index: "+posStop+" size: "+size);

        int n = posStop-posStart;
        ParameterStatus[] r = new ParameterStatus[n];

        int k = size - posStop;
        int i = counts.size()-1;
        while(k>counts.get(i)) {
            k-=counts.get(i--);
        }
        k=counts.get(i)-k;

        int pos = 0;

        while(true) {
            r[pos++] = statusList.get(i);
            if(pos==n) break;

            k--;
            if(k<0) {
                i--;
                k=counts.get(i)-1;
            }
        }
        return r;
    }


    public ParameterStatus get(int pos) {
        int k = 0;
        int i = 0;
        while(k<=pos) {
            k+=counts.get(i);
            i++;
        }
        return statusList.get(i-1);
    }


    /**
     * the number of elements in this segment (not taking into account any compression due to run-length encoding)
     * @return
     */
    public int getSize() {
        return size;
    }
}