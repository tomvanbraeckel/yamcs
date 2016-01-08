package org.yamcs.parameterarchive;

import java.util.function.Consumer;

import org.yamcs.protobuf.Yamcs.Value;

public class SegmentIterator {
    final SortedTimeSegment timeSegment;
    final ValueSegment valueSegment;
    final long start;
    final long stop;
    final boolean ascending;
    int pos;
    
    public SegmentIterator(SortedTimeSegment timeSegment, ValueSegment valueSegment, long start, long stop, boolean ascending) {
        this.start = start;
        this.stop = stop;
        this.ascending = ascending;
        this.timeSegment = timeSegment;
        this.valueSegment = valueSegment;
        init();
    }

   
    
    private void init() {
        if(ascending) {
            if(start < timeSegment.getSegmentStart()) {
                pos = 0;
            } else {                          
                pos = timeSegment.search(start);
                if(pos<0) pos = -pos-1;
            }
        } else {
            if(stop > timeSegment.getSegmentEnd()) {
                pos = timeSegment.size()-1;
            } else {                          
                pos = timeSegment.search(stop);
                if(pos<0)  pos = -pos-2;
            }
        }
    }
    
    boolean hasNext() {
        if(ascending) {
            return pos < timeSegment.size() && timeSegment.getTime(pos)<stop;
        } else {
            return pos>=0 && timeSegment.getTime(pos)>start;
        }
    }
    
    public TimedValue next() {
        long t = timeSegment.getTime(pos);
        Value v = valueSegment.get(pos);
        if(ascending) {
            pos++;
        } else {
            pos--;
        }
        return new TimedValue(t,v);
    }

    public void forEachRemaining(Consumer<TimedValue> consumer) {
        while(hasNext()) {
            long t = timeSegment.getTime(pos);
            Value v = valueSegment.get(pos);
            if(ascending) {
                pos++;
            } else {
                pos--;
            }
            consumer.accept(new TimedValue(t, v));
        }
    }
        
    public interface SimpleValueConsumer {
        public void accept(long t, Value v);
    }
}
