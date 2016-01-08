package org.yamcs.parameterarchive;

import java.lang.reflect.Array;
import java.util.NavigableMap;
import java.util.PriorityQueue;
import java.util.function.Consumer;

import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamcs.parameterarchive.MultiParameterDataRetrieval.PartitionIteratorComparator;
import org.yamcs.parameterarchive.ParameterArchive.Partition;
import org.yamcs.utils.DecodingException;

public class SingleParameterDataRetrieval {
    final private SingleParameterValueRequest spvr;
    final private ParameterArchive parchive;
    private final Logger log = LoggerFactory.getLogger(SingleParameterDataRetrieval.class);

    public SingleParameterDataRetrieval(ParameterArchive parchive, SingleParameterValueRequest spvr) {
        this.spvr = spvr;
        this.parchive = parchive;
    }



    public void retrieve(Consumer<ParameterValueArray> consumer) throws RocksDBException, DecodingException {
        long startPartition = Partition.getPartitionId(spvr.start);
        long stopPartition = Partition.getPartitionId(spvr.stop);

        NavigableMap<Long,Partition> parts = parchive.getPartitions(startPartition, stopPartition);
        if(!spvr.ascending) {
            parts = parts.descendingMap();
        }
        if(spvr.parameterGroupIds.length==1) {
            for(Partition p:parts.values()) {
                retrieveValuesFromPartitionSingleGroup(p, consumer);
            }
        } else {
            for(Partition p:parts.values()) {
                retrieveValuesFromPartitionMultiGroup(p, consumer);
            }
        }
    }


    //this is the easy case, one single parameter group -> no merging of segments necessary
    private void retrieveValuesFromPartitionSingleGroup(Partition p, Consumer<ParameterValueArray> consumer) throws DecodingException, RocksDBException {
        int parameterGroupId = spvr.parameterGroupIds[0];
        RocksIterator it = parchive.getIterator(p);
        try {
            PartitionIterator pit = new PartitionIterator(it, spvr.parameterId, parameterGroupId, spvr.start, spvr.stop, spvr.ascending);

            while(pit.isValid()) {
                SegmentKey key = pit.key();

                SortedTimeSegment timeSegment = parchive.getTimeSegment(p, key.segmentStart, parameterGroupId );
                if(timeSegment==null) {
                    String msg = "Cannot find a time segment for parameterGroupId="+parameterGroupId+" segmentStart = "+key.segmentStart+" despite having a value segment for parameterId: "+spvr.parameterId;
                    log.error(msg);
                    throw new RuntimeException(msg);
                }
                BaseSegment valueSegment = pit.value();
                retriveValuesFromSegment(timeSegment, valueSegment, spvr, consumer);
                pit.next();
            }
        } finally {
            it.dispose();
        }
    }

    //multiple parameter groups -> merging of segments necessary
    private void retrieveValuesFromPartitionMultiGroup(Partition p, Consumer<ParameterValueArray> consumer) throws DecodingException, RocksDBException {
        RocksIterator[] its = new RocksIterator[spvr.parameterGroupIds.length];
        PriorityQueue<PartitionIterator> queue = new PriorityQueue<PartitionIterator>(new PartitionIteratorComparator(spvr.ascending));

        for(int i =0 ; i<spvr.parameterGroupIds.length; i++) {
            its[i] = parchive.getIterator(p);
            PartitionIterator pi = new PartitionIterator(its[i], spvr.parameterId,  spvr.parameterGroupIds[i], spvr.start, spvr.stop, spvr.ascending);
            if(pi.isValid()) {
                queue.add(pi);
            }
        }
        SegmentMerger merger = new SegmentMerger(spvr.ascending, consumer);
        while(!queue.isEmpty()) {
            PartitionIterator pit = queue.poll();
            SegmentKey key = pit.key();
            SortedTimeSegment timeSegment = parchive.getTimeSegment(p, key.segmentStart, pit.getParameterGroupId());
            if(timeSegment==null) {
                String msg = "Cannot find a time segment for parameterGroupId="+pit.getParameterGroupId()+" segmentStart = "+key.segmentStart+" despite having a value segment for parameterId: "+spvr.parameterId;
                log.error(msg);
                throw new RuntimeException(msg);
            }
            BaseSegment valueSegment = pit.value();
            retriveValuesFromSegment(timeSegment, valueSegment, spvr, merger);
            pit.next();
            if(pit.isValid()) {
                queue.add(pit);
            }
        } 
        merger.flush();

        for(RocksIterator it:its) {
            it.dispose();
        }

    }



    private void retriveValuesFromSegment(SortedTimeSegment timeSegment, BaseSegment valueSegment, SingleParameterValueRequest pvr,
            Consumer<ParameterValueArray> consumer) {
        int posStart, posStop;
        if(pvr.ascending) {
            if(pvr.start < timeSegment.getSegmentStart()) {
                posStart = 0;
            } else {                          
                posStart = timeSegment.search(pvr.start);
                if(posStart<0) posStart = -posStart-1;
            }

            if(pvr.stop>timeSegment.getSegmentEnd()) {
                posStop = timeSegment.size();
            } else {                          
                posStop = timeSegment.search(pvr.stop);
                if(posStop<0) posStop = -posStop-1;
            }
        } else {
            if(pvr.stop > timeSegment.getSegmentEnd()) {
                posStop = timeSegment.size()-1;
            } else {                          
                posStop = timeSegment.search(pvr.stop);
                if(posStop<0)  posStop = -posStop-2;
            }

            if(pvr.start < timeSegment.getSegmentStart()) {
                posStart = -1;
            } else {                          
                posStart = timeSegment.search(pvr.start);
                if(posStart<0) posStart = -posStart-2;
            }
        }
        
        if(posStart>=posStop) return;
        
        long[] timestamps = timeSegment.getRange(posStart, posStop, pvr.ascending);
        Object values = valueSegment.getRange(posStart, posStop, pvr.ascending);
        ParameterValueArray pva = new ParameterValueArray(pvr.parameterId, timestamps, values);
        consumer.accept(pva);
    }

    /**
     * Merges ParameterValueArray for same parameter and sends the result to the final consumer
     * @author nm
     *
     */
    static class SegmentMerger implements Consumer<ParameterValueArray>{
        final Consumer<ParameterValueArray> finalConsumer;
        final boolean ascending;
        ParameterValueArray mergedPva;

        public SegmentMerger(final boolean ascending, Consumer<ParameterValueArray> finalConsumer) {
            this.finalConsumer = finalConsumer;
            this.ascending = ascending;
        }

        @Override
        public void accept(ParameterValueArray pva) {
            if(mergedPva==null) {
                mergedPva = pva;
            } else {
                if(SortedTimeSegment.getSegmentId(mergedPva.timestamps[0]) != SortedTimeSegment.getSegmentId(pva.timestamps[0])) {
                    finalConsumer.accept(mergedPva);
                    mergedPva = pva;
                } else {
                    merge(pva);
                }
            }
        }
        //merge the pva with the mergedPva into a new pva
        private void merge(ParameterValueArray pva) {

            long[] timestamps = new long[pva.timestamps.length+mergedPva.timestamps.length];
            Object values = merge(mergedPva.timestamps, pva.timestamps, mergedPva.values, pva.values, timestamps);

            mergedPva = new ParameterValueArray(mergedPva.parameterId, timestamps, values);
        }




        private Object merge(long[] timestamps1, long[] timestamps2,  Object values1, Object values2, long[] mergedTimestamps) {
            int i = 0; 
            int j = 0;
            int k = 0;
            Object mergedValues = Array.newInstance(values1.getClass().getComponentType(), mergedTimestamps.length);
            while(true) {
                long t1, t2;
                if(i<timestamps1.length) {
                    t1 = timestamps1[i];
                } else {
                    int n = timestamps2.length-j;
                    System.arraycopy(timestamps2, j, mergedTimestamps, k, n);
                    System.arraycopy(values2, j, mergedValues, k, n);
                    break;
                }

                if(j<timestamps2.length) {
                    t2 = timestamps2[j];
                } else {
                    int n = timestamps1.length - i;
                    System.arraycopy(timestamps1, i, mergedTimestamps, k, n);
                    System.arraycopy(values1, i, mergedValues, k, n);
                    break;
                }
                if((ascending && t1<=t2) || (!ascending && t1>=t2)) {
                    mergedTimestamps[k] = t1;
                    //this is about 6 times slower than a direct assignment but I don't know other trick to avoid duplicating the merge method for each primitive type
                    System.arraycopy(values1, i, mergedValues, k, 1);
                    k++; i++;
                } else {
                    mergedTimestamps[k] = t2;
                    System.arraycopy(values2, j, mergedValues, k, 1);
                    k++; j++;
                }
            }

            return mergedValues;
        }
        

        /**
         * sends the last segment
         */
        public void flush() {
            if(mergedPva!=null) {
                finalConsumer.accept(mergedPva);
            }
            mergedPva = null;
        }
    }

}
