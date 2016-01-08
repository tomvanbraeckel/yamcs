package org.yamcs.parameterarchive;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.yamcs.ParameterValue;
import org.yamcs.protobuf.Mdb.AlarmLevelType;
import org.yamcs.protobuf.Mdb.AlarmRange;
import org.yamcs.protobuf.Pvalue.AcquisitionStatus;
import org.yamcs.protobuf.Pvalue.MonitoringResult;
import org.yamcs.protobuf.Pvalue.ParameterValueFlag;
import org.yamcs.utils.DecodingException;
import org.yamcs.utils.IntArray;
import org.yamcs.utils.VarIntUtil;
import org.yamcs.xtce.FloatRange;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Stores parameter flags runlength encoded
 * @author nm
 *
 */
public class FlagSegment extends BaseSegment {
    IntArray counts;
    List<ParameterValueFlag> flags;

    FlagSegment() {
        super(FORMAT_ID_FlagSegment);
        flags = new ArrayList<>();
        counts = new IntArray();
    }


    public void addParameterValue(ParameterValue pv) {
        ParameterValueFlag pvf = getFlag(pv);
        boolean added = false;
        
        if(!flags.isEmpty()) {
            ParameterValueFlag lastFlag = flags.get(flags.size()-1);
            if(pvf.equals(lastFlag)) {
                int n = counts.size() -1;
                int lastCount = counts.get(n);
                lastCount++;
                counts.set(n, lastCount);
                added = true;
            }
        }
        
        if(!added) {
            counts.add(1);
            flags.add(pvf);
        }
    }




    @Override
    public void writeTo(ByteBuffer bb) {
        //first write the counts
        VarIntUtil.writeVarInt32(bb, counts.size());
        for(int i=0; i<counts.size(); i++) {
            VarIntUtil.writeVarInt32(bb, counts.get(i));
        }

        //then write the flags
        VarIntUtil.writeVarInt32(bb, flags.size());
        for(int i=0; i<flags.size(); i++) {
            byte[] b= flags.get(i).toByteArray();
            VarIntUtil.writeVarInt32(bb, b.length);
            bb.put(b);
        }
    }

    @Override
    public void parseFrom(ByteBuffer bb) throws DecodingException {
        int countNum = VarIntUtil.readVarInt32(bb);
        counts = new IntArray(countNum);
        for(int i=0; i<counts.size(); i++) {
            counts.add(VarIntUtil.readVarInt32(bb));
        }

        int flagNum = VarIntUtil.readVarInt32(bb);
        flags = new ArrayList<>(flagNum);
        try {
            for(int i=0; i<flags.size(); i++) {
                int size = VarIntUtil.readVarInt32(bb);
                byte[] b = new byte[size];
                bb.get(b);
                ParameterValueFlag flag;
                flag = ParameterValueFlag.parseFrom(b);
                flags.add(flag);
            }
        } catch (InvalidProtocolBufferException e) {
            throw new DecodingException("Failed to decode flag segment", e);
        }
    }

    @Override
    public int getMaxSerializedSize() {
        int size = 4* counts.size();
        for(ParameterValueFlag pvf: flags) {
            size+=pvf.getSerializedSize();
        }
        return size;
    }

    @Override
    public Object getRange(int posStart, int posStop, boolean ascending) {
        if(posStart>=posStop) throw new IllegalArgumentException("posStart has to be smaller than posStop");
        ParameterValueFlag[] r = new ParameterValueFlag[posStop-posStart];
        
        if(ascending) {
            for(int i = posStart; i<posStop; i++) {
                r[i-posStart] = get(i);
            }
        } else {
            for(int i = posStop; i>posStart; i--) {
                r[posStop-i] = get(i);  
            }
        }
        return null;
    }
    

    private ParameterValueFlag get(int pos) {
        int k = 0;
        int i = 0;
        while(k<pos) {
            k+=counts.get(i);
            i++;
        }
        return flags.get(i-1);
    }


    private ParameterValueFlag getFlag(ParameterValue pv) {
        ParameterValueFlag.Builder pvfb =  ParameterValueFlag.newBuilder();
        AcquisitionStatus acq = pv.getAcquisitionStatus();
        if(acq!=null) {
            pvfb.setAcquisitionStatus(acq);
        }
        MonitoringResult mr = pv.getMonitoringResult();
        if(mr!=null) {
            pvfb.setMonitoringResult(mr);
        }
        addAlarmRange(pvfb, AlarmLevelType.WATCH, pv.getWatchRange());
        addAlarmRange(pvfb, AlarmLevelType.WARNING, pv.getWarningRange());
        addAlarmRange(pvfb, AlarmLevelType.DISTRESS, pv.getDistressRange());
        addAlarmRange(pvfb, AlarmLevelType.CRITICAL, pv.getCriticalRange());
        addAlarmRange(pvfb, AlarmLevelType.SEVERE, pv.getSevereRange());
        
        return pvfb.build();
        
    }


    private void addAlarmRange(ParameterValueFlag.Builder pvfb, AlarmLevelType level, FloatRange range) {
        if(range==null) return;
        
        AlarmRange.Builder rangeb = AlarmRange.newBuilder();
        rangeb.setLevel(level);
        if (Double.isFinite(range.getMinInclusive()))
            rangeb.setMinInclusive(range.getMinInclusive());
        if (Double.isFinite(range.getMaxInclusive()))
            rangeb.setMaxInclusive(range.getMaxInclusive());
        pvfb.addAlarmRange(rangeb.build());
    }

}
