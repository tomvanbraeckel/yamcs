package org.yamcs.parameterarchive;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDB;
import org.yamcs.utils.FileUtils;

public class TestParameterGroupIdMap {

    @Test
    public void test1() throws Exception {
        File f = new File("/tmp/TestParameterIdMap_test1");
        FileUtils.deleteRecursively(f.toPath());
        RocksDB db = RocksDB.open(f.getAbsolutePath());
        ColumnFamilyHandle cfh =  db.getDefaultColumnFamily();
        
        ParameterGroupIdDb pgidMap = new ParameterGroupIdDb(db, cfh);
        int[] p1 = new int[] {1,3,4};
        int[] p2 = new int[] {1,3,4};
        int[] p3 = new int[] {1,4,5};
        
        int pg1 = pgidMap.get(p1);
        int pg3 = pgidMap.get(p3);
        int pg2 = pgidMap.get(p2);

        
        
        assertEquals(pg1, pg2);
        assertTrue(pg3 > pg1);
        
        db.close();
        
        db = RocksDB.open(f.getAbsolutePath());
        cfh =  db.getDefaultColumnFamily();
        pgidMap = new ParameterGroupIdDb(db, cfh);
        int pg4 = pgidMap.get(p1);
        assertEquals(pg1, pg4);
        
        int pg5 = pgidMap.get(p3);
        assertEquals(pg3, pg5);
        int[] p4 = new int[] {1,4,7};
        
        int pg6 = pgidMap.get(p4);
        
        assertTrue(pg6 > pg3);
    }

}
