package org.yamcs.parameterarchive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamcs.ParameterValue;
import org.yamcs.YamcsServer;
import org.yamcs.parameter.ParameterConsumer;
import org.yamcs.time.TimeService;
import org.yamcs.utils.SortedIntArray;
import org.yamcs.utils.TimeEncoding;

/**
 * Injects into the parameter archive, parameters from the realtime processor.
 * 
 * At the same time allows retrieval of parameters (for public consumption)
 * 
 * 
 * @author nm
 *
 */
public class RealtimeParameterFiller implements ParameterConsumer {
    final ParameterArchive parameterArchive;
    
    //segment time -> ParameterGroup_id -> PGSegment
    TreeMap<Long, Map<Integer, PGSegment>> pgSegments = new TreeMap<>();
    final ParameterIdMap parameterIdMap;
    final ParameterGroupIdMap parameterGroupIdMap;
    private final Logger log = LoggerFactory.getLogger(RealtimeParameterFiller.class);
    final TimeService timeService;

    static public long TIME_WINDOW_LENGTH = 3600*1000L;  //ignore any parameter that does not fit in a segment overlapping with this time window
    ReadWriteLock lock = new ReentrantReadWriteLock();
    Lock writeLock = lock.writeLock();
    Lock readLock = lock.readLock();
    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    volatile long timeWindowStart;
    
    public RealtimeParameterFiller(ParameterArchive parameterArchive) {
        this.parameterArchive = parameterArchive;
        parameterIdMap = parameterArchive.getParameterIdMap();
        parameterGroupIdMap = parameterArchive.getParameterGroupIdMap();
        timeService = YamcsServer.getTimeService(parameterArchive.getYamcsInstance());
        timeWindowStart = (timeService.getMissionTime() - TIME_WINDOW_LENGTH) & TimeSegment.SEGMENT_MASK;
    }

    @Override
    public void updateItems(int subscriptionId, List<ParameterValue> items) {
        writeLock.lock();
        long t0 = getTimeWindowStart();
        try {
            Map<Long, SortedParameterList> m = new HashMap<>();
            for(ParameterValue pv: items) {
                long t = pv.getAcquisitionTime();
                if(t<t0) continue;

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

    /**
     * Get the start of the time window for which the parameters are stored.
     * 
     * It is ({@link TimeService#getMissionTime()} - {@value #TIME_WINDOW_LENGTH}) & {@value TimeSegment#SEGMENT_MASK}, rounded down to the segment start
     *
     * 
     * Any parameter whose acquisition time is before this instant is ignored by the RealtimeParameterFiller.
     * 
     * @return
     */
    private void updateTimeWindowStart() {
        long now = timeService.getMissionTime();
        this.timeWindowStart = (now-TIME_WINDOW_LENGTH) & TimeSegment.SEGMENT_MASK;
    }
    
    public long getTimeWindowStart() {
        return timeWindowStart;
    }

    private void processUpdate(long t, SortedParameterList pvList) {
        try {
            int parameterGroupId = parameterGroupIdMap.get(pvList.parameterIdArray);
            long segmentId = TimeSegment.getSegmentId(t);
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

    public void retrieveValues(ParameterValueRequest pvr) {
        readLock.lock();
        try {
            if(pvr.stop!=TimeEncoding.INVALID_INSTANT && pvr.stop < timeWindowStart) {
                return ; //no data in this interval
            }
            
            doProcessRequest(pvr);
            
        } finally {
            readLock.unlock();
        }
    }
    

    private void doProcessRequest(ParameterValueRequest pvr) {
        Set<Long> segments;
        if(pvr.ascending) {
            segments = pgSegments.navigableKeySet();
        } else {
            segments = pgSegments.descendingKeySet();
        }
        for(long segmentId: segments) {
           if(TimeSegment.overlap(segmentId, pvr.start, pvr.stop)) {
               Map<Integer, PGSegment> m = pgSegments.get(segmentId);
               PGSegment pgs = m.get(pvr.parameterGroupId);
               if(pgs==null) continue;
               
               pgs.retrieveValues(pvr);
               
               
           }
        }
    }


    /*builds incrementally a list of parameter id and parameter value, sorted by parameter ids */
    class SortedParameterList {
        SortedIntArray parameterIdArray = new SortedIntArray();
        List<ParameterValue> sortedPvList = new ArrayList<ParameterValue>();

        void add(ParameterValue pv) {
            int parameterId = parameterIdMap.get(pv.getParameter().getQualifiedName(), pv.getEngValue().getType());
            int pos = parameterIdArray.add(parameterId);
            sortedPvList.add(pos, pv);
        }

    }
}
