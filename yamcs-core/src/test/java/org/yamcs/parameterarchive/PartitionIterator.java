package org.yamcs.parameterarchive;

import org.rocksdb.RocksIterator;

/**
 * Iterates over the segments of one partition for a parameter_id, ParameterGroup_id, between a start and stop
 * 
 * 
 * @author nm
 *
 */
public class PartitionIterator {
    private final RocksIterator iterator;
    private boolean valid=false;
    private SegmentKey currentKey;
    private final int parameterId, parameterGroupId;
    private final long start, stop;
    private final boolean ascending;
    ValueSegmentEncoderDecoder vsEncoder = new ValueSegmentEncoderDecoder();
    
    public PartitionIterator(RocksIterator iterator, int parameterId, int parameterGroupId, long start, long stop, boolean ascending) {
        this.parameterId = parameterId;
        this.parameterGroupId = parameterGroupId;
        this.start = start;
        this.stop = stop;
        this.ascending = ascending;
        
        this.iterator = iterator;
        if(ascending) {
            goToFirstAscending();
        } else {
            goToFirstDescending();
        }
    }



    private void goToFirstDescending() {
        iterator.seek(new SegmentKey(parameterId, parameterGroupId, SortedTimeSegment.getSegmentStart(stop)).encode());
        if(!iterator.isValid()) {
            iterator.seekToLast();
        } else {
            currentKey = SegmentKey.decode(iterator.key());
            if((currentKey.parameterGroupId == parameterGroupId) && (currentKey.parameterId==parameterId) && (currentKey.segmentStart<=stop)) {
                valid = true;
                return;
            }  else {
                iterator.prev();
            }
        }

        if(!iterator.isValid()) {
            valid = false;
            return;
        } 
        currentKey = SegmentKey.decode(iterator.key());
        if((currentKey.parameterGroupId == parameterGroupId) && (currentKey.parameterId==parameterId)) {
            valid = true;
        } else {
            valid = false;
        }
    }

    private void goToFirstAscending() {
        iterator.seek(new SegmentKey(parameterId, parameterGroupId, SortedTimeSegment.getSegmentStart(start)).encode());
        if(!iterator.isValid()) {
            valid = false;
            return;
        }
        currentKey = SegmentKey.decode(iterator.key());
        if((currentKey.parameterGroupId==parameterGroupId) && (currentKey.parameterId==parameterId) && (currentKey.segmentStart<=stop)) {
            valid = true;
        } else {
            valid = false;
        }
    }

    void next() {
        if(ascending) {
            iterator.next();
        } else {
            iterator.prev();
        }

        if(!iterator.isValid()) {
            valid = false;
            return;
        }
        currentKey = SegmentKey.decode(iterator.key());

        if((currentKey.parameterGroupId != parameterGroupId) || (currentKey.parameterId!=parameterId)) {
            valid = false;
            return;
        }

        if(ascending) {
            if(currentKey.segmentStart>stop) {
                valid = false;
                return;
            }
        } else {
            if(currentKey.segmentStart < SortedTimeSegment.getSegmentStart(start)) {
                valid = false;
                return;
            }
        }
    }
    
    SegmentKey key() {
        return currentKey;
    }
    
    ValueSegment value() throws DecodingException {
        return vsEncoder.decode(iterator.value(), currentKey.segmentStart);
    } 
    
    boolean isValid() {
        return valid;
    }



    public int getParameterGroupId() {
        return parameterGroupId;
    }



    public int getParameterId() {
        return parameterId;
    }
}
