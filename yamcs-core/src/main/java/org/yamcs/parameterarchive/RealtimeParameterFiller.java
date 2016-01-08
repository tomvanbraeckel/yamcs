package org.yamcs.parameterarchive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamcs.ParameterValue;
import org.yamcs.YamcsServer;
import org.yamcs.parameter.ParameterConsumer;
import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.time.TimeService;
import org.yamcs.utils.SortedIntArray;
import org.yamcs.utils.TimeEncoding;

import com.google.common.util.concurrent.AbstractService;

/**
 * Injects into the parameter archive, parameters from the realtime processor.
 * 
 * The parameter values are collected into PGSegment objects 
 * The PGSegments are consolidated into the archive when the segment end is older than current time - TIME_WINDOW_LENGTH
 * 
 * At the same time allows retrieval of parameters (for public consumption).
 * 
 * 
 * @author nm
 *
 */
public class RealtimeParameterFiller extends AbstractService implements ParameterConsumer {
    final ParameterArchive parameterArchive;

    //segment id -> ParameterGroup_id -> PGSegment
    TreeMap<Long, Map<Integer, PGSegment>> pgSegments = new TreeMap<>();
    final ParameterIdDb parameterIdMap;
    final ParameterGroupIdDb parameterGroupIdMap;
    private final Logger log = LoggerFactory.getLogger(RealtimeParameterFiller.class);
    final TimeService timeService;

    static public final long CONSOLIDATE_OLDER_THAN = 3600*1000L; 
    ReadWriteLock lock = new ReentrantReadWriteLock();
    Lock writeLock = lock.writeLock();
    Lock readLock = lock.readLock();
    ScheduledThreadPoolExecutor executor ;


    //we accept and serve parameters that fit into the time window starting with this
    volatile long timeWindowStart;


    public RealtimeParameterFiller(ParameterArchive parameterArchive) {
        this.parameterArchive = parameterArchive;
        parameterIdMap = parameterArchive.getParameterIdMap();
        parameterGroupIdMap = parameterArchive.getParameterGroupIdMap();
        timeService = YamcsServer.getTimeService(parameterArchive.getYamcsInstance());
     
    }


    @Override
    protected void doStart() {
        timeWindowStart = SortedTimeSegment.getSegmentStart(timeService.getMissionTime() - CONSOLIDATE_OLDER_THAN);
        executor = new ScheduledThreadPoolExecutor(1);
        executor.scheduleAtFixedRate(this::doHouseKeeping, 60, 60, TimeUnit.SECONDS);

        notifyStarted();
    }

    @Override
    protected void doStop() {
        executor.shutdown();
        notifyStopped();
    }
    /**
     * 
     * @return the start of the timewindow for which parameter values can be obtained from this object
     * 
     */
    public long getTimeWindowStart() {
        return timeWindowStart;
    }



    @Override
    public void updateItems(int subscriptionId, List<ParameterValue> items) {
        executor.execute(() -> {updateItems(subscriptionId, items);});
    }

    void doUpdateItems(int subscriptionId, List<ParameterValue> items) {
        writeLock.lock();
        try {
            Map<Long, SortedParameterList> m = new HashMap<>();
            for(ParameterValue pv: items) {
                long t = pv.getAcquisitionTime();
                if(t<timeWindowStart) continue;

                SortedParameterList l = m.get(t);
                if(l==null) {
                    l = new SortedParameterList();
                    m.put(t, l);
                }
                l.add(pv);
            }

            for(Map.Entry<Long,SortedParameterList> entry: m.entrySet()) {
                long t = entry.getKey();
                SortedParameterList pvList = entry.getValue();
                processUpdate(t, pvList);
            }
        } finally {
            writeLock.unlock();
        }
    }


    void doHouseKeeping() {
        long now = timeService.getMissionTime();
        long consolidateOlderThan = SortedTimeSegment.getSegmentStart(now - CONSOLIDATE_OLDER_THAN);
       
        //note that we do not need a lock here because we do not touch the pgSegments structure and 
        // because doUpdateItems is called on the same thread with us, we know it's not doing anything
        NavigableMap<Long, Map<Integer, PGSegment>> m = pgSegments.headMap(consolidateOlderThan, true);
        if(m.isEmpty()) return;
        
        List<PGSegment> listToConsolidate = new ArrayList<>();
        
        for(Map<Integer, PGSegment> m1: m.values()) {
            listToConsolidate.addAll(m1.values());
        }

        consolidateAndWriteToArchive(listToConsolidate);
        
        writeLock.lock();
        try {
            for(Long segmentId: m.keySet()) {
                pgSegments.remove(segmentId);
            }
            timeWindowStart = consolidateOlderThan;
        } finally {
            writeLock.unlock();
        }
    }



    /**
     * writes data into the archive
     * @param pgs
     */
    private void consolidateAndWriteToArchive(List<PGSegment> pgList) {
        for(PGSegment pgs: pgList) {
            pgs.consolidate();
        }
        try {
            parameterArchive.writeToArchive(pgList);
        } catch (RocksDBException e) {
           log.error("failed to write data to the archive", e);
        }
    }

    private void processUpdate(long t, SortedParameterList pvList) {
        try {
            int parameterGroupId = parameterGroupIdMap.get(pvList.parameterIdArray);
            long segmentId = SortedTimeSegment.getSegmentId(t);
            Map<Integer, PGSegment> m = pgSegments.get(segmentId);
            if(m==null) {
                m = new HashMap<Integer, PGSegment>();
                pgSegments.put(segmentId, m);
            }
            PGSegment pgs = m.get(parameterGroupId);
            if(pgs==null) {
                pgs = new PGSegment(parameterGroupId, segmentId, pvList.parameterIdArray);
                m.put(parameterGroupId, pgs);
            }

            pgs.addRecord(t, pvList.sortedPvList);

        } catch (RocksDBException e) {
            log.error("Error processing parameters", e);
        }

    }

    public void retrieveValues(SingleParameterValueRequest pvr, Consumer<TimedValue> consumer) {
        readLock.lock();
        try {
            if(pvr.stop!=TimeEncoding.INVALID_INSTANT && pvr.stop < timeWindowStart) {
                return ; //no data in this interval
            }
            doProcessRequest(pvr, consumer);
        } finally {
            readLock.unlock();
        }
    }


    private void doProcessRequest(SingleParameterValueRequest pvr, Consumer<TimedValue> consumer) {
        Set<Long> segments;
        if(pvr.ascending) {
            segments = pgSegments.navigableKeySet();
        } else {
            segments = pgSegments.descendingKeySet();
        }
        for(long segmentId: segments) {
            if(SortedTimeSegment.overlap(segmentId, pvr.start, pvr.stop)) {
                Map<Integer, PGSegment> m = pgSegments.get(segmentId);
                PGSegment pgs = m.get(pvr.parameterGroupIds[0]);
                if(pgs==null) continue;

                pgs.retrieveValues(pvr, consumer);
            }
        }
    }


    /*builds incrementally a list of parameter id and parameter value, sorted by parameter ids */
    class SortedParameterList {
        SortedIntArray parameterIdArray = new SortedIntArray();
        List<ParameterValue> sortedPvList = new ArrayList<ParameterValue>();

        void add(ParameterValue pv) {
            String fqn = pv.getParameter().getQualifiedName();
            Value.Type engType = pv.getEngValue().getType();
            Value.Type rawType = (pv.getRawValue()==null)? null: pv.getRawValue().getType();
            int parameterId = parameterIdMap.get(fqn, engType, rawType);
            
            int pos = parameterIdArray.insert(parameterId);
            sortedPvList.add(pos, pv);
        }

    }


}
