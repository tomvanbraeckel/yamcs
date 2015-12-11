package org.yamcs.parameterarchive;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDB;
import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.utils.FileUtils;

public class TestParameterIdMap {

    @Test
    public void test1() throws Exception {
        File f = new File("/tmp/TestParameterIdMap_test1");
        FileUtils.deleteRecursively(f.toPath());
        RocksDB db = RocksDB.open(f.getAbsolutePath());
        ColumnFamilyHandle cfh =  db.getDefaultColumnFamily();
        
        ParameterIdMap pidMap = new ParameterIdMap(db, cfh);
        int p1 = pidMap.get("/test1/bla", Value.Type.BOOLEAN);
        int p2 = pidMap.get("/test1/bla", Value.Type.BOOLEAN);
        assertEquals(p1, p2);
        
        int p3 = pidMap.get("/test1/bla", Value.Type.DOUBLE);
        assertTrue(p3 > p1);
        
        
        db.close();
        
        db = RocksDB.open(f.getAbsolutePath());
        cfh =  db.getDefaultColumnFamily();
        pidMap = new ParameterIdMap(db, cfh);
        int p4 = pidMap.get("/test1/bla", Value.Type.BOOLEAN);
        assertEquals(p1, p4);
        int p5 = pidMap.get("/test1/bla", Value.Type.DOUBLE);
        assertEquals(p3, p5);
        
        int p6 = pidMap.get("/test2/bla", Value.Type.DOUBLE);
        assertTrue(p6 > p3);
    }

}
