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
import org.yamcs.utils.DecodingException;


public class MultiParameterDataRetrieval {
    final ParameterArchive parchive;
    final MultipleParameterValueRequest mpvr;

    SegmentEncoderDecoder vsEncoder = new SegmentEncoderDecoder();
    private final Logger log = LoggerFactory.getLogger(MultiParameterDataRetrieval.class);

    public MultiParameterDataRetrieval(ParameterArchive parchive, MultipleParameterValueRequest mpvr) {
        this.parchive = parchive;
        this.mpvr = mpvr;
    }

    public void retrieve(Consumer<ParameterIdValueList> consumer) throws RocksDBException, DecodingException {
        long startPartitionId = Partition.getPartitionId(mpvr.start);
        long stopPartitionId = Partition.getPartitionId(mpvr.stop);
        
        NavigableMap<Long, Partition> parts = parchive.getPartitions(startPartitionId, stopPartitionId);
        if(!mpvr.ascending) {
            parts = parts.descendingMap();
        }
        for(Partition p: parts.values()) {
            retrieveFromPartition(p, consumer);
        }
    }

    private void retrieveFromPartition(Partition p, Consumer<ParameterIdValueList> consumer) throws RocksDBException, DecodingException {
       
        RocksIterator[] its = new RocksIterator[mpvr.parameterIds.length];
        
        PriorityQueue<PartitionIterator> queue = new PriorityQueue<PartitionIterator>(new PartitionIteratorComparator(mpvr.ascending));
        SegmentMerger merger = null;
        
        for(int i =0 ; i<mpvr.parameterIds.length; i++) {
            its[i] = parchive.getIterator(p);
            PartitionIterator pi = new PartitionIterator(its[i], mpvr.parameterIds[i],  mpvr.parameterGroupIds[i], mpvr.start, mpvr.stop, mpvr.ascending);
            if(pi.isValid()) {
                queue.add(pi);
            }
        }
        while(!queue.isEmpty()) {
            PartitionIterator pit = queue.poll();
            SegmentKey key = pit.key();
            if(merger ==null) {
                merger = new SegmentMerger(key, mpvr.ascending);
            } else {
                if(key.segmentStart!=merger.key.segmentStart) {
                    sendAllData(merger, consumer);
                    merger = new SegmentMerger(key, mpvr.ascending);
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
            
            new SegmentIterator(timeSegment, valueSegment, mpvr.start, mpvr.stop, mpvr.ascending).forEachRemaining(merger);
            pit.next();
            if(pit.isValid()) {
                queue.add(pit);
            }
        }
        if(merger!=null) {
            sendAllData(merger, consumer);
        }
        
        
        for(int i =0 ; i<mpvr.parameterIds.length; i++) {
            its[i].dispose();
        }
    }

    private void sendAllData(SegmentMerger merger, Consumer<ParameterIdValueList> consumer) {
        merger.values.values().forEach(consumer);
    }

    static class SegmentMerger implements Consumer<TimedValue>{
        final SegmentKey key;
        TreeMap<Long,ParameterIdValueList> values;
        int currentParameterId;
        int currentParameterGroupId;
        
        public SegmentMerger(SegmentKey key, final boolean ascending) {
            this.key = key;
            values = new TreeMap<>(new Comparator<Long>() {
                @Override
                public int compare(Long o1, Long o2) {
                    if(ascending){
                        return o1.compareTo(o2);
                    } else {
                        return o2.compareTo(o1);
                    }
                }
            });  
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
        final boolean ascending;
        public PartitionIteratorComparator(boolean ascending) {
            this.ascending = ascending;
        }

        @Override
        public int compare(PartitionIterator pit1, PartitionIterator pit2) {
            int c;
            if(ascending){
                c = Long.compare(pit1.key().segmentStart, pit2.key().segmentStart);
            } else {
                c= Long.compare(pit2.key().segmentStart, pit1.key().segmentStart);
            }
            
            if(c!=0) {
                return c;
            }
            //make sure the parameters are extracted in the order of their id (rather than some random order from PriorityQueue)
            return Integer.compare(pit1.getParameterId(), pit2.getParameterId());
        }
    }
}
