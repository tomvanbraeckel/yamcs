package org.yamcs.parameterarchive;

import org.yamcs.ParameterValue;
import org.yamcs.protobuf.Mdb.AlarmLevelType;
import org.yamcs.protobuf.Mdb.AlarmRange;
import org.yamcs.protobuf.Pvalue.AcquisitionStatus;
import org.yamcs.protobuf.Pvalue.MonitoringResult;
import org.yamcs.protobuf.Pvalue.ParameterStatus;
import org.yamcs.xtce.FloatRange;

public abstract class AbstractParameterStatusSegment extends BaseSegment {

    public AbstractParameterStatusSegment(byte formatId) {
        super(formatId);
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
    public abstract ParameterStatus[] getRange(int posStart, int posStop, boolean ascending) ;
}
