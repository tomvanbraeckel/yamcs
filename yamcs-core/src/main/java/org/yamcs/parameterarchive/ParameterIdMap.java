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

/**
 * Stores a map between
 * (parameter_fqn, type) and parameter_id
 * 
 * Backed by RocksDB
 * 
 * @author nm
 *
 */
public class ParameterIdMap {
    final RocksDB db;
    final ColumnFamilyHandle p2pid_cfh;

    Map<String, Map<Value.Type, Integer>> p2pidCache = new HashMap<>();
    int highestParaId=0;

    ParameterIdMap(RocksDB db, ColumnFamilyHandle p2pid_cfh) {
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
    public synchronized int get(String paramFqn, Value.Type type) throws ParameterArchiveException {
        Map<Value.Type, Integer> m = p2pidCache.get(paramFqn);
        if(m==null) {
            m = new HashMap<Value.Type, Integer>();
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



    private void store(String paramFqn) throws ParameterArchiveException {
        Map<Value.Type, Integer> m = p2pidCache.get(paramFqn);
        byte[] key = paramFqn.getBytes(StandardCharsets.UTF_8);
        ByteBuffer bb = ByteBuffer.allocate(6*m.size());
        for(Map.Entry<Value.Type, Integer> me:m.entrySet()) {
            bb.putShort((short)me.getKey().getNumber());
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
            Map<Value.Type, Integer> m = new HashMap<Value.Type, Integer>();

            p2pidCache.put(paraName, m);
            ByteBuffer bb = ByteBuffer.wrap(pIdTypeList);
            while(bb.hasRemaining()) {
                Value.Type type = Value.Type.valueOf(bb.getShort());
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
