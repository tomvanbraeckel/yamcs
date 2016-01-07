package org.yamcs.parameterarchive;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.DBOptions;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamcs.yarch.YarchDatabase;

public class ParameterArchive {
    static Map<String, ParameterArchive> instances = new HashMap<String, ParameterArchive>();
    static final byte[] CF_NAME_meta_p2pid = "meta_p2pid".getBytes(StandardCharsets.US_ASCII);
    static final byte[] CF_NAME_meta_pgid2pg = "meta_pgid2pg".getBytes(StandardCharsets.US_ASCII); 
    static final byte[] CF_NAME_data_prefix = "data_".getBytes(StandardCharsets.US_ASCII);

    private final Logger log = LoggerFactory.getLogger(ParameterArchive.class);
    private ParameterIdDb parameterIdMap;
    private ParameterGroupIdDb parameterGroupIdMap;
    private RocksDB rdb;

    ColumnFamilyHandle p2pid_cfh;
    ColumnFamilyHandle pgid2pg_cfh;

    final String yamcsInstance;
    private TreeMap<Long, Partition> partitions = new TreeMap<Long, ParameterArchive.Partition>();
    ValueSegmentEncoderDecoder vsEncoder = new ValueSegmentEncoderDecoder();

    public ParameterArchive(String instance) throws RocksDBException {
        this.yamcsInstance = instance;
        String dbpath = YarchDatabase.getInstance(instance).getRoot() +"/ParameterArchive";
        File f = new File(dbpath+"/IDENTITY");
        if(f.exists()) {
            openExistingDb(dbpath);
        } else {
            createDb(dbpath);
        }
        parameterIdMap = new ParameterIdDb(rdb, p2pid_cfh);
        parameterGroupIdMap = new ParameterGroupIdDb(rdb, pgid2pg_cfh);
    }

    private void createDb(String dbpath) throws RocksDBException {
        ColumnFamilyDescriptor cfd_p2pid = new ColumnFamilyDescriptor(CF_NAME_meta_p2pid);
        ColumnFamilyDescriptor cfd_pgid2pg = new ColumnFamilyDescriptor(CF_NAME_meta_pgid2pg);
        ColumnFamilyDescriptor cfd_default = new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY);


        List<ColumnFamilyDescriptor> cfdList = Arrays.asList(cfd_p2pid, cfd_pgid2pg, cfd_default);
        List<ColumnFamilyHandle> cfhList = new ArrayList<ColumnFamilyHandle>(cfdList.size());
        DBOptions options = new DBOptions();
        options.setCreateIfMissing(true);
        options.setCreateMissingColumnFamilies(true);
        rdb = RocksDB.open(options, dbpath, cfdList, cfhList);
        p2pid_cfh = cfhList.get(0);
        pgid2pg_cfh = cfhList.get(1);
    }

    private void openExistingDb(String dbpath) throws RocksDBException {
        List<byte[]> cfList = RocksDB.listColumnFamilies(new Options(), dbpath);
        List<ColumnFamilyDescriptor> cfdList = new ArrayList<ColumnFamilyDescriptor>(cfList.size());
        for(byte[] cf:cfList) {
            ColumnFamilyDescriptor cfd = new ColumnFamilyDescriptor(cf);
            cfdList.add(cfd);
        }

        List<ColumnFamilyHandle> cfhList = new ArrayList<ColumnFamilyHandle>(cfList.size());
        DBOptions options = new DBOptions();
        options.setCreateIfMissing(true);
        options.setCreateMissingColumnFamilies(true);
        rdb = RocksDB.open(options, dbpath, cfdList, cfhList);
        for(int i=0; i<cfdList.size(); i++) {
            byte[] cf = cfList.get(i);

            if(Arrays.equals(CF_NAME_meta_p2pid, cf)) {
                p2pid_cfh = cfhList.get(i);
            } else if(Arrays.equals(CF_NAME_meta_pgid2pg, cf)) {
                pgid2pg_cfh = cfhList.get(i);
            }  else if(startsWith(cf, CF_NAME_data_prefix)) {
                long partitionId = decodePartitionId(CF_NAME_data_prefix, cf);
                Partition p = partitions.get(partitionId);
                if(p==null) {
                    p = new Partition(partitionId);
                    partitions.put(partitionId, p);
                }
                p.dataCfh = cfhList.get(i);
            } else {
                if(!Arrays.equals(RocksDB.DEFAULT_COLUMN_FAMILY, cf)) {
                    log.warn("Ignoring unknown column family "+new String(cf, StandardCharsets.US_ASCII));
                }
            }
        }
        if(p2pid_cfh==null) {
            throw new ParameterArchiveException("Cannot find column family '"+new String(CF_NAME_meta_p2pid, StandardCharsets.US_ASCII)+"' in database at "+dbpath);
        }
        if(pgid2pg_cfh==null) {
            throw new ParameterArchiveException("Cannot find column family '"+new String(CF_NAME_meta_pgid2pg, StandardCharsets.US_ASCII)+"' in database at "+dbpath);
        }      
    }

    private static long decodePartitionId(byte[] prefix, byte[] cf) {
        int l = prefix.length;
        try {
            return Long.decode("0x"+new String(cf, l, cf.length-l, StandardCharsets.US_ASCII));
        } catch (NumberFormatException e) {
            throw new ParameterArchiveException("Cannot decode partition id from column family: "+Arrays.toString(cf));
        }
    }

    private static byte[] encodePartitionId(byte[] prefix, long partitionId) {
        byte[] pb = ("0x"+Long.toHexString(partitionId)).getBytes(StandardCharsets.US_ASCII);
        byte[] cf = Arrays.copyOf(prefix, prefix.length+pb.length);
        System.arraycopy(pb, 0, cf, prefix.length, pb.length);
        return cf;
    }


    static synchronized ParameterArchive getInstance(String instance) throws RocksDBException {
        ParameterArchive pdb = instances.get(instance);
        if(pdb==null) {
            pdb = new ParameterArchive(instance);
            instances.put(instance, pdb);
        }
        return pdb;
    }

    public ParameterIdDb getParameterIdMap() {
        return parameterIdMap;
    }

    public ParameterGroupIdDb getParameterGroupIdMap() {
        return parameterGroupIdMap;
    }


    /**
     * returns true if a starts with prefix
     * @param a
     * @param prefix
     * @return
     */
    private boolean startsWith(byte[] a, byte[] prefix) {
        int n = prefix.length;
        if(a.length<n) return false;

        for(int i=0;i<n;i++) {
            if(a[i]!=prefix[i]) return false;
        }
        return true;
    }

    public void close() {
        rdb.close();
    }

    public String getYamcsInstance() {
        return yamcsInstance;
    }

    public void writeToArchive(List<PGSegment> pgList) throws RocksDBException {
        WriteBatch writeBatch = new WriteBatch();
        for(PGSegment pgs: pgList) {
            writeToBatch(writeBatch, pgs);
        }
        WriteOptions wo = new WriteOptions();

        rdb.write(wo, writeBatch);
    }

    private void writeToBatch(WriteBatch writeBatch, PGSegment pgs) throws RocksDBException{
        long segStart = pgs.getSegmentStart();
        long partitionId = Partition.getPartitionId(segStart);
        Partition p = createAndGetPartition(partitionId);

        //write the time segment
        SortedTimeSegment timeSegment = pgs.getTimeSegment();
        byte[] key = new SegmentKey(ParameterIdDb.TIMESTAMP_PARA_ID, pgs.getParameterGroupId(), pgs.getSegmentStart()).encode();
        byte[] value = vsEncoder.encode(timeSegment);
        writeBatch.put(p.dataCfh, key, value);

        //and then the consolidated value segments
        List<ValueSegment> consolidated = pgs.getConsolidatedValueSegments();
        for(int i=0; i<consolidated.size(); i++) {
            ValueSegment vs= consolidated.get(i);
            int parameterId = pgs.getParameterId(i);
            key = new SegmentKey(parameterId, pgs.getParameterGroupId(), pgs.getSegmentStart()).encode();
            value = vsEncoder.encode(vs);
            writeBatch.put(p.dataCfh, key, value);
        }
    }


    /**
     * get partition for id, creating it if it doesn't exist
     * @param partitionId
     * @return
     * @throws RocksDBException 
     */
    private Partition createAndGetPartition(long partitionId) throws RocksDBException {
        synchronized(partitions) {
            Partition p = partitions.get(partitionId);
            if(p==null) {
                p = new Partition(partitionId);
                byte[] cfname = encodePartitionId(CF_NAME_data_prefix, partitionId);
                ColumnFamilyDescriptor cfd = new ColumnFamilyDescriptor(cfname);
                p.dataCfh = rdb.createColumnFamily(cfd);

                partitions.put(partitionId, p);
            }
            return p;
        }
    }


    public void retrieveValues(SingleParameterValueRequest pvr, Consumer<TimedValue> consumer) throws RocksDBException, DecodingException {
        long startPartition = Partition.getPartitionId(pvr.start);
        long stopPartition = Partition.getPartitionId(pvr.stop);

        NavigableMap<Long,Partition> parts = getPartitions(startPartition, stopPartition);
        if(!pvr.ascending) {
            parts = parts.descendingMap();
        }
        for(Partition p:parts.values()) {
            retrieveValuesFromPartition(p, pvr, consumer);
        }
    }



    private void retrieveValuesFromPartition(Partition p, SingleParameterValueRequest pvr, Consumer<TimedValue> consumer) throws DecodingException, RocksDBException {
        RocksIterator it = rdb.newIterator(p.dataCfh);
        try {
            PartitionIterator pit = new PartitionIterator(it, pvr.parameterId, pvr.parameterGroupId, pvr.start, pvr.stop, pvr.ascending);

            while(pit.isValid()) {
                SegmentKey key = pit.key();

                
                SortedTimeSegment timeSegment = getTimeSegment(p, key.segmentStart, pvr.parameterGroupId);
                if(timeSegment==null) {
                    String msg = "Cannot find a time segment for parameterGroupId="+pvr.parameterGroupId+" segmentStart = "+key.segmentStart+" despite having a value segment for parameterId: "+pvr.parameterId;
                    log.error(msg);
                    throw new RuntimeException(msg);
                }
                ValueSegment valueSegment = pit.value();
                new SegmentIterator(timeSegment, valueSegment, pvr.start, pvr.stop, pvr.ascending).forEachRemaining(consumer);
                pit.next();
            }
        } finally {
            it.dispose();
        }
    }
    
    /** 
     * a copy of the partitions from start to stop inclusive
     * @param startPartition
     * @param stopPartition
     * @return
     */
    public NavigableMap<Long, Partition> getPartitions(long startPartition, long stopPartition) {
        synchronized(partitions) {
            TreeMap<Long, Partition> r = new TreeMap<Long, ParameterArchive.Partition>();
            r.putAll(partitions.subMap(startPartition, true, stopPartition, true));
            return r;
        }
    }
    
    static class Partition {
        public static final int NUMBITS_MASK=31; //2^31 millisecons =~ 24 days per partition    
        public static final long TIMESTAMP_MASK = (0xFFFFFFFF>>>(32-NUMBITS_MASK));
        public static final long PARTITION_MASK = ~TIMESTAMP_MASK;

        final long partitionId;
        ColumnFamilyHandle dataCfh;

        Partition(long partitionId) {
            this.partitionId = partitionId;
        }        


        static long getPartitionId(long instant) {
            return instant & PARTITION_MASK;
        }

        static long getPartitionStart(long instant) {
            return getPartitionId(instant);
        }

        public static long getPartitionEnd(long partitionId) {
            return partitionId  | TIMESTAMP_MASK;
        }
    }

    public RocksIterator getIterator(Partition p) {
        return rdb.newIterator(p.dataCfh);
    }

    public SortedTimeSegment getTimeSegment(Partition p, long segmentStart,  int parameterGroupId) throws RocksDBException, DecodingException {
        byte[] timeKey = new SegmentKey(ParameterIdDb.TIMESTAMP_PARA_ID, parameterGroupId, segmentStart).encode();
        byte[] tv = rdb.get(p.dataCfh, timeKey);
        if(tv==null) {
            return null;
        }
        return (SortedTimeSegment) vsEncoder.decode(tv, segmentStart);
    }
}
