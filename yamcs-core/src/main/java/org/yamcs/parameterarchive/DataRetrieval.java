package org.yamcs.parameterarchive;

import java.util.Comparator;
import java.util.NavigableMap;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamcs.parameterarchive.ParameterArchive.Partition;


public class DataRetrieval {
    final ParameterArchive parchive;
    int[] parameterIds;
    int[] parameterGroupIds;
    long start, stop;
    boolean ascending;

    ValueSegmentEncoderDecoder vsEncoder = new ValueSegmentEncoderDecoder();
    private final Logger log = LoggerFactory.getLogger(DataRetrieval.class);

    public DataRetrieval(ParameterArchive parchive) {
        this.parchive = parchive;
    }

    public void retrieve(Consumer<ParameterIdValueList> consumer) throws RocksDBException, DecodingException {
        NavigableMap<Long, Partition> parts = parchive.getPartitions(start, stop);
        for(Partition p: parts.values()) {
            retrieveFromPartition(p, consumer);
        }
    }

    private void retrieveFromPartition(Partition p, Consumer<ParameterIdValueList> consumer) throws RocksDBException, DecodingException {
       
        RocksIterator[] its = new RocksIterator[parameterIds.length];
        
        PriorityQueue<PartitionIterator> queue = new PriorityQueue<PartitionIterator>(new PartitionIteratorComparator());
        SegmentMerger merger = null;
        
        for(int i =0 ; i<parameterIds.length; i++) {
            its[i] = parchive.getIterator(p);
            PartitionIterator pi = new PartitionIterator(its[i], parameterIds[i],  parameterGroupIds[i], start, stop, ascending);
            if(pi.isValid()) {
                queue.add(pi);
            }
        }
        while(!queue.isEmpty()) {
            PartitionIterator pit = queue.poll();
            SegmentKey key = pit.key();
            if(merger ==null) {
                merger = new SegmentMerger(key);
            } else {
                if(key.segmentStart!=merger.key.segmentStart) {
                    sendAllData(merger, consumer);
                    merger = new SegmentMerger(key);
                }
            }
            
            SortedTimeSegment timeSegment = parchive.getTimeSegment(p, key.segmentStart, pit.getParameterGroupId());
            if(timeSegment==null) {
                String msg = "Cannot find a time segment for parameterGroupId="+ pit.getParameterGroupId()+" segmentStart = "+key.segmentStart+" despite having a value segment for parameterId: "+pit.getParameterId();
                log.error(msg);
                throw new RuntimeException(msg);
            }
            ValueSegment valueSegment = pit.value();
            merger.currentParameterGroupId = pit.getParameterGroupId();
            merger.currentParameterId = pit.getParameterId();
            
            new SegmentIterator(timeSegment, valueSegment, start, stop, ascending).forEachRemaining(merger);
            pit.next();
            if(pit.isValid()) {
                queue.add(pit);
            }
        }
        
        for(int i =0 ; i<parameterIds.length; i++) {
            its[i].dispose();
        }
    }

    private void sendAllData(SegmentMerger merger, Consumer<ParameterIdValueList> consumer) {
        merger.values.values().forEach(consumer);
    }

    static class SegmentMerger implements Consumer<TimedValue>{
        final SegmentKey key;
        TreeMap<Long,ParameterIdValueList> values = new TreeMap<>();  
        int currentParameterId;
        int currentParameterGroupId;
        
        public SegmentMerger(SegmentKey key) {
            this.key = key;
        }
        
        @Override
        public void accept(TimedValue tv) {
            long k = k(currentParameterGroupId, tv.instant);
            ParameterIdValueList vlist = values.get(k);
            if(vlist==null) {
                vlist = new ParameterIdValueList(tv.instant, currentParameterGroupId);
                values.put(k, vlist);
            }
            vlist.add(currentParameterId, tv.value);
        }
        
        private long k(int parameterGroupId, long instant) {
            return ((long)parameterGroupId)<<SortedTimeSegment.NUMBITS_MASK | (instant & SortedTimeSegment.TIMESTAMP_MASK);
        }
        
    }
    static class PartitionIteratorComparator implements Comparator<PartitionIterator> {
        @Override
        public int compare(PartitionIterator pit1, PartitionIterator pit2) {
            return Long.compare(pit1.key().segmentStart, pit2.key().segmentStart);
        }
    }
}
