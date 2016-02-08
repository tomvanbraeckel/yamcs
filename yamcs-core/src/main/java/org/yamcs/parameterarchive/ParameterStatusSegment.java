package org.yamcs.parameterarchive;

import org.yamcs.ParameterValue;
import org.yamcs.protobuf.Mdb.AlarmLevelType;
import org.yamcs.protobuf.Mdb.AlarmRange;
import org.yamcs.protobuf.Pvalue.AcquisitionStatus;
import org.yamcs.protobuf.Pvalue.MonitoringResult;
import org.yamcs.protobuf.Pvalue.ParameterStatus;
import org.yamcs.utils.DecodingException;
import org.yamcs.xtce.FloatRange;

import com.google.protobuf.InvalidProtocolBufferException;

public  class ParameterStatusSegment extends ObjectSegment<ParameterStatus> {
    static ParameterStatusSerializer serializer = new ParameterStatusSerializer();
    
    public ParameterStatusSegment( boolean buildForSerialisation) {
        super(serializer, buildForSerialisation);
    }


    static public final ParameterStatus getStatus(ParameterValue pv) {
        ParameterStatus.Builder pvfb =  ParameterStatus.newBuilder();
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


    static private void addAlarmRange(ParameterStatus.Builder pvfb, AlarmLevelType level, FloatRange range) {
        if(range==null) return;

        AlarmRange.Builder rangeb = AlarmRange.newBuilder();
        rangeb.setLevel(level);
        if (Double.isFinite(range.getMinInclusive()))
            rangeb.setMinInclusive(range.getMinInclusive());
        if (Double.isFinite(range.getMaxInclusive()))
            rangeb.setMaxInclusive(range.getMaxInclusive());
        pvfb.addAlarmRange(rangeb.build());
    }

    

    public void addParameterValue(int pos, ParameterValue pv) {
        add(pos, getStatus(pv));
    }
    public void addParameterValue(ParameterValue pv) {
       add(getStatus(pv)); 
    }
    
    ParameterStatusSegment consolidate() {
        return (ParameterStatusSegment) super.consolidate();
    }
    
    
    static class ParameterStatusSerializer implements ObjectSerializer<ParameterStatus>  {
        @Override
        public byte getFormatId() {
            return BaseSegment.FORMAT_ID_ParameterStatusSegment;
        }

        @Override
        public ParameterStatus deserialize(byte[] b) throws DecodingException {
            try {
                return ParameterStatus.parseFrom(b);
            } catch (InvalidProtocolBufferException e) {
                throw new DecodingException("Cannto deserialzie ParameterStatus", e);
            }
        }

        @Override
        public byte[] serialize(ParameterStatus e) {
            return e.toByteArray();
        }
    }
}
