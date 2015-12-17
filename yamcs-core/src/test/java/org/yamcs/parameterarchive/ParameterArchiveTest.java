package org.yamcs.parameterarchive;

import static org.junit.Assert.*;

import org.junit.Test;
import org.yamcs.protobuf.Yamcs.Value.Type;
import org.yamcs.utils.FileUtils;
import org.yamcs.yarch.YarchDatabase;

public class ParameterArchiveTest {
    String instance = "ParameterArchiveTest";
    
    
    @Test
    public void test1() throws Exception {
        String dbroot = YarchDatabase.getInstance(instance).getRoot();
        FileUtils.deleteRecursively(dbroot+"/ParameterArchive");
        ParameterArchive parchive = new ParameterArchive(instance);
        ParameterIdMap pidMap = parchive.getParameterIdMap();
        ParameterGroupIdMap pgidMap = parchive.getParameterGroupIdMap();
        assertNotNull(pidMap);
        assertNotNull(pgidMap);
        int p1id = pidMap.get("/test/p1", Type.BINARY);
        
        parchive.close();
        
        parchive = new ParameterArchive(instance);
        pidMap = parchive.getParameterIdMap();
        pgidMap = parchive.getParameterGroupIdMap();
        assertNotNull(pidMap);
        assertNotNull(pgidMap);
        int p2id = pidMap.get("/test/p2", Type.SINT32);
        
        assertFalse(p1id==p2id);
        assertEquals(p1id, pidMap.get("/test/p1", Type.BINARY));
    }
    
}
