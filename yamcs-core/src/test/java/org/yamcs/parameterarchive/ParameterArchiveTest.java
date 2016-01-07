package org.yamcs.parameterarchive;

import static org.junit.Assert.*;
import static org.yamcs.parameterarchive.TestUtils.*;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;
import org.yamcs.ParameterValue;
import org.yamcs.YamcsServer;
import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.protobuf.Yamcs.Value.Type;
import org.yamcs.utils.FileUtils;
import org.yamcs.utils.SortedIntArray;
import org.yamcs.utils.TimeEncoding;
import org.yamcs.utils.ValueUtility;
import org.yamcs.xtce.Parameter;
import org.yamcs.yarch.YarchDatabase;

public class ParameterArchiveTest {
    String instance = "ParameterArchiveTest";
    
    static MockupTimeService timeService;
    static Parameter p1, p2,p3,p4,p5;
    
    @BeforeClass
    public static void beforeClass() {
        p1 = new Parameter("p1");
        p2 = new Parameter("p2");
        p3 = new Parameter("p3");
        p4 = new Parameter("p4");
        p5 = new Parameter("p5");
        p1.setQualifiedName("/test/p1");
        p2.setQualifiedName("/test/p2");       
        p3.setQualifiedName("/test/p3");
        p4.setQualifiedName("/test/p4");
        p5.setQualifiedName("/test/p5");
        TimeEncoding.setUp();

        timeService = new MockupTimeService();
        YamcsServer.setMockupTimeService(timeService);
    }

    
    @Test
    public void test1() throws Exception {
        String dbroot = YarchDatabase.getInstance(instance).getRoot();
        
        FileUtils.deleteRecursively(dbroot+"/ParameterArchive");
        ParameterArchive parchive = new ParameterArchive(instance);
        ParameterIdDb pidMap = parchive.getParameterIdMap();
        ParameterGroupIdDb pgidMap = parchive.getParameterGroupIdMap();
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
    
    
    @Test
    public void testSingleParameter() throws Exception{
        String dbroot = YarchDatabase.getInstance(instance).getRoot();
        FileUtils.deleteRecursively(dbroot+"/ParameterArchive");

        ParameterArchive parchive = new ParameterArchive(instance);
        timeService.missionTime = 0;

       
        
        ParameterValue pv1_0 = getParameterValue(p1, 100, "blala100");


        int p1id = parchive.getParameterIdMap().get(p1.getQualifiedName(), pv1_0.getEngValue().getType());

        int pg1id = parchive.getParameterGroupIdMap().get(new int[]{p1id});
        PGSegment pgSegment1 = new PGSegment(pg1id, 0, new SortedIntArray(new int[] {p1id}));
        
        pgSegment1.addRecord(100, Arrays.asList(pv1_0));
        ParameterValue pv1_1 = getParameterValue(p1, 200, "blala200");
        pgSegment1.addRecord(200, Arrays.asList(pv1_1));
        
        
        //ascending request on empty data
        MyValueConsummer c0a = new MyValueConsummer();
        SingleParameterValueRequest pvr0a = new SingleParameterValueRequest(0, 1000, pg1id, p1id, true);
        parchive.retrieveValues(pvr0a, c0a);
        assertEquals(0, c0a.times.size());

        //descending request on empty data
        MyValueConsummer c0d = new MyValueConsummer();
        SingleParameterValueRequest pvr0d = new SingleParameterValueRequest(0, 1000, pg1id, p1id, true);
        parchive.retrieveValues(pvr0d, c0d);
        assertEquals(0, c0d.times.size());
        
        pgSegment1.consolidate();
        parchive.writeToArchive(Arrays.asList(pgSegment1));
        
        //ascending request on two value
        MyValueConsummer c4a = new MyValueConsummer();       
        SingleParameterValueRequest pvr4a = new SingleParameterValueRequest(0, TimeEncoding.MAX_INSTANT, pg1id, p1id, true);
        parchive.retrieveValues(pvr4a, c4a);
        checkEquals(c4a, pv1_0, pv1_1);
        
        //descending request on two value
        MyValueConsummer c4d = new MyValueConsummer();       
        SingleParameterValueRequest pvr4d = new SingleParameterValueRequest(0, TimeEncoding.MAX_INSTANT, pg1id, p1id, false);
        parchive.retrieveValues(pvr4d, c4d);
        checkEquals(c4d, pv1_1, pv1_0);
        
        //ascending request on two value with start on first value
        MyValueConsummer c5a = new MyValueConsummer();       
        SingleParameterValueRequest pvr5a = new SingleParameterValueRequest(100, 1000, pg1id, p1id, true);
        parchive.retrieveValues(pvr5a, c5a);
        checkEquals(c5a, pv1_0, pv1_1);
        
        //descending request on two value with start on second value
        MyValueConsummer c5d = new MyValueConsummer();       
        SingleParameterValueRequest pvr5d = new SingleParameterValueRequest(0, 200, pg1id, p1id, false);
        parchive.retrieveValues(pvr5d, c5d);
        checkEquals(c5d, pv1_1, pv1_0);
        
        //ascending request on two value with start on the first value and stop on second 
        MyValueConsummer c6a = new MyValueConsummer();       
        SingleParameterValueRequest pvr6a = new SingleParameterValueRequest(100, 200, pg1id, p1id, true);
        parchive.retrieveValues(pvr6a, c6a);
        checkEquals(c6a, pv1_0);
        
        //descending request on two value with start on the second value and stop on first 
        MyValueConsummer c6d = new MyValueConsummer();       
        SingleParameterValueRequest pvr6d = new SingleParameterValueRequest(100, 200, pg1id, p1id, false);
        parchive.retrieveValues(pvr6d, c6d);
        checkEquals(c6d, pv1_1);

        
        //new value in a different segment but same partition
        long t2 = SortedTimeSegment.getSegmentEnd(0)+100;
        PGSegment pgSegment2 = new PGSegment(pg1id, SortedTimeSegment.getSegmentStart(t2), new SortedIntArray(new int[] {p1id}));
        ParameterValue pv1_2 = getParameterValue(p1, t2, "blala_ns0");        
        pgSegment2.addRecord(t2, Arrays.asList(pv1_2));
        pgSegment2.consolidate();
        parchive.writeToArchive(Arrays.asList(pgSegment2));
                
        //new value in a different partition
        long t3 = ParameterArchive.Partition.getPartitionEnd(0)+100;
        PGSegment pgSegment3 = new PGSegment(pg1id, SortedTimeSegment.getSegmentStart(t3), new SortedIntArray(new int[] {p1id}));
        ParameterValue pv1_3 = getParameterValue(p1, t3, "blala200");
        pgSegment3.addRecord(t3, Arrays.asList(pv1_3));
        pgSegment3.consolidate();
        parchive.writeToArchive(Arrays.asList(pgSegment3));
         

        //ascending request on four value
        MyValueConsummer c7a = new MyValueConsummer();       
        SingleParameterValueRequest pvr7a = new SingleParameterValueRequest(0, t3+1, pg1id, p1id, true);
        parchive.retrieveValues(pvr7a, c7a);
        checkEquals(c7a, pv1_0, pv1_1, pv1_2, pv1_3);
        
        //descending request on four value
        MyValueConsummer c7d = new MyValueConsummer();       
        SingleParameterValueRequest pvr7d = new SingleParameterValueRequest(0, t3+1, pg1id, p1id, false);
        parchive.retrieveValues(pvr7d, c7d);
        checkEquals(c7d, pv1_3, pv1_2, pv1_1, pv1_0);
        
        
        //ascending request on the last partition
        MyValueConsummer c8a = new MyValueConsummer();       
        SingleParameterValueRequest pvr8a = new SingleParameterValueRequest(t3, t3+1, pg1id, p1id, true);
        parchive.retrieveValues(pvr8a, c8a);
        checkEquals(c8a, pv1_3);
        
        //descending request on the last partition
        MyValueConsummer c8d = new MyValueConsummer();       
        SingleParameterValueRequest pvr8d = new SingleParameterValueRequest(t2, t3, pg1id, p1id, false);
        parchive.retrieveValues(pvr8d, c8d);
        checkEquals(c8d, pv1_3);
        
    }
    
    ParameterValue getParameterValue(Parameter p, long instant, String sv) {
        ParameterValue pv = new ParameterValue(p);
        pv.setAcquisitionTime(instant);
        Value v = ValueUtility.getStringValue(sv);
        pv.setEngineeringValue(v);
        return pv;
    }
}
