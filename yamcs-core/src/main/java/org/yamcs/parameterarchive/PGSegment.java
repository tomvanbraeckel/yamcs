package org.yamcs.parameterarchive;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.yamcs.ParameterValue;
import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.utils.SortedIntArray;

/**
 * Parameter Group segment - keeps references to Time and Value segments for a given parameter group and segment. 
 *  
 *  This class is used during the parameter archive buildup
 *   - it uses GenericValueSegment to store any kind of Value
 *   - once the segment is full, the consolidate method will turn each GenericValueSegment into an storage optimised one.
 *   
 * 
 * @author nm
 *
 */
public class PGSegment {
    final int parameterGroupId;
    final SortedIntArray parameterIds;
    private SortedTimeSegment timeSegment;
    private List<GenericValueSegment> valueSegments;
    private List<GenericValueSegment> rawValueSegments;
    private List<BasicParameterStatusSegment> parameterStatusSegments;
    
    private List<ValueSegment> consolidatedValueSegments;
    private List<ValueSegment> consolidatedRawValueSegments;
    private List<AbstractParameterStatusSegment> consolidatedParameterStatusSegments;
    
    private boolean consolidated = false;
    private final boolean storeRawValues = ParameterArchive.STORE_RAW_VALUES;
    
    public PGSegment(int parameterGroupId, long segmentStart, SortedIntArray parameterIds) {
        this.parameterGroupId = parameterGroupId;
        this.parameterIds = parameterIds;
        timeSegment = new SortedTimeSegment(segmentStart);
        valueSegments = new ArrayList<>(parameterIds.size());
        parameterStatusSegments = new ArrayList<>(parameterIds.size());
        if(storeRawValues) {
            rawValueSegments = new ArrayList<>(parameterIds.size());
        }
        
        for(int i=0; i<parameterIds.size(); i++) {
            valueSegments.add(new GenericValueSegment());
            parameterStatusSegments.add(new BasicParameterStatusSegment());
            if(storeRawValues) {
                rawValueSegments.add(new GenericValueSegment());
            }
        }
    }
    
    /**
     * Add a new record 
     *  instant goes into the timeSegment
     *  the values goes each into a value segment
     *  
     *  the sortedPvList list has to be already sorted according to the definition of the ParameterGroup 
     * 
     * 
     * @param instant
     * @param sortedPvList
     */
    public void addRecord(long instant, List<ParameterValue> sortedPvList) {
        if(consolidated) {
            throw new IllegalStateException("PGSegment is consolidated");
        }
        if(sortedPvList.size()!=valueSegments.size()) {
            throw new IllegalArgumentException("Wrong number of values passed: "+sortedPvList.size()+";expected "+valueSegments.size());
        }
        
        int pos = timeSegment.add(instant);
        for(int i = 0; i<valueSegments.size(); i++) {
            ParameterValue pv = sortedPvList.get(i);
            valueSegments.get(i).add(pos, pv.getEngValue());
            Value rawValue = pv.getRawValue();
            if(storeRawValues && (rawValue!=null)) {
                rawValueSegments.get(i).add(pos, rawValue);
            }
            parameterStatusSegments.get(i).addParameterValue(pos, pv);
        }
    }

    public void retrieveValues(SingleParameterValueRequest pvr, Consumer<TimedValue> consumer) {
        int pidx = parameterIds.search(pvr.parameterId);
        if(pidx<0) {
            throw new IllegalArgumentException("Received a parameter id "+pvr.parameterId+" that is not part of this parameter group");
        }
        ValueSegment vs = valueSegments.get(pidx);
        
        new SegmentIterator(timeSegment, vs, pvr.start, pvr.stop, pvr.ascending).forEachRemaining(consumer);
    }
 
    
    public void consolidate() {
        consolidated = true;
        consolidatedValueSegments  = new ArrayList<ValueSegment>(valueSegments.size());
        for(GenericValueSegment gvs: valueSegments) {
            consolidatedValueSegments.add(gvs.consolidate());
        }
        if(storeRawValues) {
            consolidatedRawValueSegments  = new ArrayList<ValueSegment>(valueSegments.size());
            
            //the raw values will only be stored if they are different than the engineering values
            for(int i=0;i<valueSegments.size(); i++) {
                GenericValueSegment rvs = rawValueSegments.get(i);
                GenericValueSegment vs = valueSegments.get(i);
                if((rvs.size()==0) || rvs.equals(vs)) {
                    consolidatedRawValueSegments.add(null);
                } else {
                    consolidatedRawValueSegments.add(rvs.consolidate());
                }
            }
        }
        
        consolidatedParameterStatusSegments =  new ArrayList<>(parameterStatusSegments.size());
        for(int i=0;i<valueSegments.size(); i++) {
            consolidatedParameterStatusSegments.add(parameterStatusSegments.get(i).consolidate());
        }
    }

    public long getSegmentStart() {
        return timeSegment.getSegmentStart();
    }

    public SortedTimeSegment getTimeSegment() {
       return timeSegment;
    }

    public int getParameterGroupId() {
        return parameterGroupId;
    }
    
    public int getParameterId(int index) {
        return parameterIds.get(index);
    }

    public List<ValueSegment> getConsolidatedValueSegments() {
        return consolidatedValueSegments;
    }

    public List<ValueSegment> getConsolidatedRawValueSegments() {
        return consolidatedRawValueSegments;
    }

    public List<AbstractParameterStatusSegment> getConsolidatedParameterStatusSegments() {
        return consolidatedParameterStatusSegments;
    }
}
