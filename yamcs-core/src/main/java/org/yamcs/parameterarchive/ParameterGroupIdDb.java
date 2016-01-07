package org.yamcs.parameterarchive;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.yamcs.utils.SortedIntArray;

/**
 * Stores a map between 
 * List<parameter_id> and ParameterGroup_id.
 * 
 * Stores data one column family:
 * pgid2pg 
 *     key = ParameterGroup_id
 *     value = SortedVarArray of parameter_id
 *           
 * 
 * Backed by RocksDB
 * @author nm
 *
 */
public class ParameterGroupIdDb {
    final RocksDB db;
    final ColumnFamilyHandle pgid2pg_cfh;
    int highestPgId=0;
    Map<SortedIntArray, Integer> pg2pgidCache = new HashMap<>();
    
    
    ParameterGroupIdDb(RocksDB db, ColumnFamilyHandle pgid2pg_cfh) {
        this.db = db;
        this.pgid2pg_cfh = pgid2pg_cfh;
        readDb();
    }
    
    public synchronized int get(SortedIntArray s) throws RocksDBException {
        Integer pgid = pg2pgidCache.get(s);
        if(pgid == null) {
            int x = ++highestPgId;;
            pgid = x;
            db.put(encodeInt(x), s.encodeToVarIntArray());
            pg2pgidCache.put(s, pgid);
        }
        
        return pgid;
    }
    
    public int get(int[] parameterIdArray) throws RocksDBException {
        return get(new SortedIntArray(parameterIdArray));
    }
    
    private byte[] encodeInt(int x) {
        return ByteBuffer.allocate(4).putInt(x).array();
    }
    
    private void readDb() {
        RocksIterator it = db.newIterator(pgid2pg_cfh);
        it.seekToFirst();
        while(it.isValid()) {
            
            int pgid = ByteBuffer.wrap(it.key()).getInt();
            
            if(highestPgId < pgid) highestPgId = pgid;
            
            SortedIntArray svil = SortedIntArray.decodeFromVarIntArray(it.value());
            pg2pgidCache.put(svil, pgid);
            it.next();
        }
    }
    
    
    public String toString() {
        return pg2pgidCache.toString();
    }
}
