package org.yamcs.parameterarchive;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

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
    TimeSegment timeSegment;
    List<GenericValueSegment> valueSegments;
    
    
    public PGSegment(int parameterGroupId, long segmentId, SortedIntArray parameterIds) {
        this.parameterGroupId = parameterGroupId;
        this.parameterIds = parameterIds;
        timeSegment = new TimeSegment(segmentId);
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
        
        int pos = timeSegment.add(instant);
        for(int i = 0;i<valueSegments.size(); i++) {
            valueSegments.get(i).add(pos, sortedPvList.get(i));
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
        
        ListIterator<Value> it = vs.getIterator(pos); 
                
        while(pos<timeSegment.size()) {
            long t = timeSegment.get(pos++);
            if(t>=pvr.stop) break;
            
            Value v = it.next();
            pvr.consumer.addValue(t, v);
        }
    }
   
   
    private void extractDescending(ParameterValueRequest pvr, ValueSegment vs) {
        int pos = timeSegment.search(pvr.stop);
        if(pos<0) pos = -pos-1;
        
        if(pos<0) return;
        ListIterator<Value> it = vs.getIterator(pos); 
                
        while(pos>0) {
            long t = timeSegment.get(--pos);
            if(t<pvr.start) break;
            Value v = it.previous();
            pvr.consumer.addValue(t, v);
        }
        
    }
    
    
    public void consolidate() {
        
    }
}
