package org.yamcs.parameterarchive;

import org.yamcs.protobuf.Yamcs.Value;

public class SgementIterator {
    
    static public void extractAscending(SingleParameterValueRequest pvr, SortedTimeSegment timeSegment, ValueSegment valueSegment) {
         int pos;
         
         if(pvr.start<timeSegment.getSegmentStart()) {
             pos = 0;
         } else {                          
             pos = timeSegment.search(pvr.start);
             if(pos<0) pos = -pos-1;
         }
         if(pos>=timeSegment.size()) return;
                 
         while(pos<timeSegment.size()) {
             long t = timeSegment.getTime(pos);
             if(t>=pvr.stop) break;
             
             Value v = valueSegment.get(pos);
             pvr.consumer.addValue(t, v);
             pos++;
         }
     }
    
    
     static public void extractDescending(SingleParameterValueRequest pvr, SortedTimeSegment timeSegment, ValueSegment valueSegment) {
         int pos;
         
         if(pvr.stop >= timeSegment.getSegmentEnd()) {
             pos = timeSegment.size()-1;
         } else {
             pos = timeSegment.search(pvr.stop);
             if(pos<0) {
                 pos = -pos-2;
             }
         }
         if(pos<0) return;
                 
         while(pos>=0) {
             long t = timeSegment.getTime(pos);
             if(t<=pvr.start) break;
             Value v = valueSegment.get(pos);
             pvr.consumer.addValue(t, v);
             pos--;
         }
         
     }
     
}
