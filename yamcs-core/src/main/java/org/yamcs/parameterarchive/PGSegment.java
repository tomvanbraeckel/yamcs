package org.yamcs.parameterarchive;

import java.util.List;

import org.yamcs.ParameterValue;

/**
 * Parameter Group segment - keeps references to Time and Value segments. 
 *  
 * 
 * @author nm
 *
 */
public class PGSegment {
    final int pgSegmentId;
    TimeSegment timeSegment;
    List<ValueSegment> valueSegments;
    
    
    public PGSegment(int id) {
        this.pgSegmentId = id;
    }
    
    /**
     * Add a new record 
     *  timestamp goes into the timeSegment
     *  the values goes each into a value segment
     *  
     *  the pvalues list has to be already sorted according to the definition of the ParameterGroup 
     * 
     * 
     * @param instant
     * @param pvalues
     */
    public void addRecord(long instant, List<ParameterValue> pvalues) {        
        int pos = timeSegment.add(instant);
        for(int i =0;i<valueSegments.size(); i++) {
            valueSegments.get(i).add(pos, pvalues.get(i));
        }
    }
    
}
