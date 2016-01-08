package org.yamcs.parameterarchive;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.protobuf.Yamcs.Value.Type;

/**
 * Stores a map between
 * (parameter_fqn, type) and parameter_id
 * type is a 32 bit assigned corresponding (engType, rawType)
 * 
 * engType and rawType are one of the types from protobuf Value.Type - we use the numbers assuming that no more than 2^15 will ever exist.
 * 
 * 
 * Backed by RocksDB
 * 
 * @author nm
 *
 */
public class ParameterIdDb {
    final RocksDB db;
    final ColumnFamilyHandle p2pid_cfh;
    final static int TIMESTAMP_PARA_ID=0;
    
    Map<String, Map<Integer, Integer>> p2pidCache = new HashMap<>();
    int highestParaId = TIMESTAMP_PARA_ID;

    ParameterIdDb(RocksDB db, ColumnFamilyHandle p2pid_cfh) {
        this.db = db;
        this.p2pid_cfh = p2pid_cfh;
        readDb();
    }


    /**
     * Get the mapping from parameter_name, type to parameter_id
     * 
     * It creates it if it does not exist
     * 
     * 
     * @param paramFqn
     * @param type
     * @return
     * @throws ParameterArchive if there was an error creating and storing a new parameter_id
     */
    public synchronized int get(String paramFqn, Value.Type engType, Value.Type rawType) throws ParameterArchiveException {
        int type = numericType(engType, rawType);
        
        Map<Integer, Integer> m = p2pidCache.get(paramFqn);
        if(m==null) {
            m = new HashMap<Integer, Integer>();
            p2pidCache.put(paramFqn, m);
        }
        Integer pid = m.get(type);
        if(pid==null) {
            pid = ++highestParaId;
            m.put(type, pid);
            store(paramFqn);
        }
        return pid;
    }
    
    /**
     * get a parameter id for a parameter that only has engineering value
     * @param paramFqn
     * @param engType
     * @return
     */
    public int get(String paramFqn, Type engType) {
        return get(paramFqn, engType, null);
    }

    
    //compose a numeric type from engType and rawType (we assume that no more than 2^15 types will ever exist)
    private int numericType(Value.Type engType, Value.Type rawType) {
        int et = (engType==null)? 0xFFFF:engType.getNumber();
        int rt = (rawType==null)? 0xFFFF:engType.getNumber();
        return et<<16|rt;
    }

    private void store(String paramFqn) throws ParameterArchiveException {
        Map<Integer, Integer> m = p2pidCache.get(paramFqn);
        byte[] key = paramFqn.getBytes(StandardCharsets.UTF_8);
        ByteBuffer bb = ByteBuffer.allocate(8*m.size());
        for(Map.Entry<Integer, Integer> me:m.entrySet()) {
            bb.putInt(me.getKey());
            bb.putInt(me.getValue());
        }
        try {
            db.put(p2pid_cfh, key, bb.array());
        } catch (RocksDBException e) {
            throw new ParameterArchiveException("Cannot store key for new parameter id", e);
        }
    }


    private void readDb() {
        RocksIterator it = db.newIterator(p2pid_cfh);
        it.seekToFirst();
        while(it.isValid()) {
            byte[] pfqn = it.key();
            byte[] pIdTypeList = it.value();

            String paraName = new String(pfqn, StandardCharsets.UTF_8);
            Map<Integer, Integer> m = new HashMap<Integer, Integer>();

            p2pidCache.put(paraName, m);
            ByteBuffer bb = ByteBuffer.wrap(pIdTypeList);
            while(bb.hasRemaining()) {
                int type = bb.getInt();
                int pid = bb.getInt();            
                m.put(type, pid);
                if(pid > highestParaId) {
                    highestParaId = pid;
                }
            }
            it.next();
        }
    }



}
