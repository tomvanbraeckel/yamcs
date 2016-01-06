package org.yamcs.parameterarchive;

import java.util.ArrayList;
import java.util.List;

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
    SortedTimeSegment timeSegment;
    List<GenericValueSegment> valueSegments;
    List<ValueSegment> consolidatedValueSegments;
    
    private boolean consolidated = false;
    
    
    public PGSegment(int parameterGroupId, long segmentStart, SortedIntArray parameterIds) {
        this.parameterGroupId = parameterGroupId;
        this.parameterIds = parameterIds;
        timeSegment = new SortedTimeSegment(segmentStart);
        valueSegments = new ArrayList<>(parameterIds.size());
        for(int i=0; i<parameterIds.size(); i++) {
            int parameterId = parameterIds.get(i);
            valueSegments.add(new GenericValueSegment(parameterId));
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
        if(consolidated) throw new IllegalStateException("PGSegment is consolidated");
        
        int pos = timeSegment.add(instant);
        for(int i = 0;i<valueSegments.size(); i++) {
            valueSegments.get(i).add(pos, sortedPvList.get(i).getEngValue());
        }
    }

    public void retrieveValues(ParameterValueRequest pvr) {
        int pidx = parameterIds.search(pvr.parameterId);
        if(pidx<0) {
            throw new IllegalArgumentException("Received a parameter id "+pvr.parameterId+" that is not part of this parameter group");
        }
        ValueSegment vs = valueSegments.get(pidx);
        
        if(pvr.ascending) {
            extractAscending(pvr, vs);
        } else {
            extractDescending(pvr, vs);
        }
    }
    
   private void extractAscending(ParameterValueRequest pvr, ValueSegment vs) {
        int pos = timeSegment.search(pvr.start);
        if(pos<0) pos = -pos-1;
        if(pos>=timeSegment.size()) return;
        
                
        while(pos<timeSegment.size()) {
            long t = timeSegment.getTime(pos);
            if(t>=pvr.stop) break;
            
            Value v = vs.get(pos);
            pvr.consumer.addValue(t, v);
            pos++;
        }
    }
   
   
    private void extractDescending(ParameterValueRequest pvr, ValueSegment vs) {
        int pos = timeSegment.search(pvr.stop);
        if(pos<0) {
            pos = -pos-2;
        }
        
        if(pos<0) return;
                
        while(pos>=0) {
            long t = timeSegment.getTime(pos);
            if(t<=pvr.start) break;
            Value v = vs.get(pos);
            pvr.consumer.addValue(t, v);
            pos--;
        }
        
    }
    
    
    public void consolidate() {
        consolidated = true;
        consolidatedValueSegments  = new ArrayList<ValueSegment>(valueSegments.size());
        for(GenericValueSegment gvs: valueSegments) {
            consolidatedValueSegments.add(gvs.consolidate());
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

    public List<ValueSegment> getConsolidatedValueSegments() {
        return consolidatedValueSegments;
    }
}
