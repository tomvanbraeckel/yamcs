package org.yamcs.parameterarchive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamcs.ParameterValue;
import org.yamcs.parameter.ParameterConsumer;
import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.utils.SortedIntArray;

import com.google.common.util.concurrent.AbstractService;

/**
 * Injects into the parameter archive, parameters from replay processors
 * 
 * 
 * @author nm
 *
 */
public abstract class AbstractParameterFiller extends AbstractService {
    protected final ParameterArchive parameterArchive;
    private final Logger log = LoggerFactory.getLogger(AbstractParameterFiller.class);

    //segment id -> ParameterGroup_id -> PGSegment
    protected TreeMap<Long, Map<Integer, PGSegment>> pgSegments = new TreeMap<>();
    protected final ParameterIdDb parameterIdMap;
    protected final ParameterGroupIdDb parameterGroupIdMap;


    ScheduledThreadPoolExecutor executor ;




    public AbstractParameterFiller(ParameterArchive parameterArchive) {
        this.parameterArchive = parameterArchive;
        parameterIdMap = parameterArchive.getParameterIdMap();
        parameterGroupIdMap = parameterArchive.getParameterGroupIdMap();
    }



    protected void doUpdateItems(int subscriptionId, List<ParameterValue> items) {
        Map<Long, SortedParameterList> m = new HashMap<>();
        for(ParameterValue pv: items) {
            long t = pv.getAcquisitionTime();

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
    }


    protected void consolidateAllAndClear() {
        log.info("Starting a consolidation process, number of intervals: "+pgSegments.size());
        for(Map<Integer, PGSegment> m: pgSegments.values()) {
            consolidateAndWriteToArchive(m.values());
        }
        pgSegments.clear();
    }
    /**
     * writes data into the archive
     * @param pgs
     */
    protected void consolidateAndWriteToArchive(Collection<PGSegment> pgList) {
        for(PGSegment pgs: pgList) {
            pgs.consolidate();
        }
        try {
            parameterArchive.writeToArchive(pgList);
        } catch (RocksDBException e) {
            log.error("failed to write data to the archive", e);
        }
    }

    protected void processUpdate(long t, SortedParameterList pvList) {
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
