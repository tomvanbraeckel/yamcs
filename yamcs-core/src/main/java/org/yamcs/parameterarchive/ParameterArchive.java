package org.yamcs.parameterarchive;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.DBOptions;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamcs.yarch.YarchDatabase;



public class ParameterArchive {
    static Map<String, ParameterArchive> instances = new HashMap<String, ParameterArchive>();
    static final byte[] CF_NAME_meta_p2pid = "meta_p2pid".getBytes(StandardCharsets.US_ASCII);
    static final byte[] CF_NAME_meta_pgid2pg = "meta_pgid2pg".getBytes(StandardCharsets.US_ASCII);
    static final byte[] CF_NAME_time_prefix = "time_".getBytes(StandardCharsets.US_ASCII);
    static final byte[] CF_NAME_data_prefix = "data_".getBytes(StandardCharsets.US_ASCII);
    
    private final Logger log = LoggerFactory.getLogger(ParameterArchive.class);
    private ParameterIdMap parameterIdMap;
    private ParameterGroupIdMap parameterGroupIdMap;
    private RocksDB rdb;
    
    ColumnFamilyHandle p2pid_cfh;
    ColumnFamilyHandle pgid2pg_cfh;
    
    final String yamcsInstance;
    private Map<Long, Partition> partitions = new HashMap<>();
    
    public ParameterArchive(String instance) throws RocksDBException {
        this.yamcsInstance = instance;
        String dbpath = YarchDatabase.getInstance(instance).getRoot() +"/ParameterArchive";
        File f = new File(dbpath+"/IDENTITY");
        if(f.exists()) {
            openExistingDb(dbpath);
        } else {
            createDb(dbpath);
        }
        parameterIdMap = new ParameterIdMap(rdb, p2pid_cfh);
        parameterGroupIdMap = new ParameterGroupIdMap(rdb, pgid2pg_cfh);
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
            } else if(startsWith(cf, CF_NAME_time_prefix)) {
                long partitionId = decodePartitionId(CF_NAME_time_prefix, cf);
                Partition p = partitions.get(partitionId);
                if(p==null) {
                    p = new Partition(partitionId);
                    partitions.put(partitionId, p);
                }
                p.timeCfh = cfhList.get(i);
            } else if(startsWith(cf, CF_NAME_data_prefix)) {
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
        //check for partitions that have only data or time cfh
        for(Partition p: partitions.values()) {
            if(p.dataCfh==null) {
                throw new ParameterArchiveException("Partition '"+Long.toHexString(p.partitionId)+" does not have the data Column Family in database at "+dbpath);
            }
            if(p.timeCfh==null) {
                throw new ParameterArchiveException("Partition '"+Long.toHexString(p.partitionId)+" does not have the time Column Family in database at "+dbpath);
            }
        }
    }
    
    private long decodePartitionId(byte[] prefix, byte[] cf) {
        int l = prefix.length;
        try {
            return Long.decode("0x"+new String(cf, l, cf.length-l, StandardCharsets.US_ASCII));
        } catch (NumberFormatException e) {
            throw new ParameterArchiveException("Cannot decode partition id from column family: "+Arrays.toString(cf));
        }
    }
    
    static synchronized ParameterArchive getInstance(String instance) throws RocksDBException {
        ParameterArchive pdb = instances.get(instance);
        if(pdb==null) {
            pdb = new ParameterArchive(instance);
            instances.put(instance, pdb);
        }
        return pdb;
    }

    public ParameterIdMap getParameterIdMap() {
        return parameterIdMap;
    }
    
    public ParameterGroupIdMap getParameterGroupIdMap() {
        return parameterGroupIdMap;
    }
    
    static class Partition {
        final long partitionId;
        Partition(long partitionId) {
            this.partitionId = partitionId;
        }
        ColumnFamilyHandle timeCfh;
        ColumnFamilyHandle dataCfh;
        
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
}
