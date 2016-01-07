package org.yamcs.parameterarchive;

import static org.junit.Assert.*;
import static org.yamcs.parameterarchive.TestUtils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

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
    
    
    @Test
    public void testMultipleParameter() throws Exception{
        String dbroot = YarchDatabase.getInstance(instance).getRoot();
        FileUtils.deleteRecursively(dbroot+"/ParameterArchive");

        ParameterArchive parchive = new ParameterArchive(instance);
        
        ParameterValue pv1_0 = getParameterValue(p1, 100, "p1_blala100");
        ParameterValue pv2_0 = getParameterValue(p2, 100, "p2_blala100");


        int p1id = parchive.getParameterIdMap().get(p1.getQualifiedName(), pv1_0.getEngValue().getType());
        int p2id = parchive.getParameterIdMap().get(p2.getQualifiedName(), pv2_0.getEngValue().getType());
        
        int pg1id = parchive.getParameterGroupIdMap().get(new int[]{p1id, p2id});
        int pg2id = parchive.getParameterGroupIdMap().get(new int[]{p1id});
        
        //ascending on empty db
        MultipleParameterValueRequest mpvr0a = new MultipleParameterValueRequest(0, TimeEncoding.MAX_INSTANT, new int[]{p1id, p2id}, new int[]{pg1id, pg1id}, true);
        DataRetrieval dr0a = new DataRetrieval(parchive, mpvr0a);
        MultiValueConsumer c0a = new MultiValueConsumer();
        dr0a.retrieve(c0a);
        assertEquals(0, c0a.list.size());
        
        //descending on empty db
        MultipleParameterValueRequest mpvr0d = new MultipleParameterValueRequest(0, TimeEncoding.MAX_INSTANT, new int[]{p1id, p2id}, new int[]{pg1id, pg1id}, false);
        DataRetrieval dr0d = new DataRetrieval(parchive, mpvr0d);
        MultiValueConsumer c0d = new MultiValueConsumer();
        dr0d.retrieve(c0d);
        assertEquals(0, c0d.list.size());
        
        ParameterValue pv1_1 = getParameterValue(p1, 200, "p1_blala200");
        ParameterValue pv1_2 = getParameterValue(p1, 300, "p1_blala300");
        
        PGSegment pgSegment1 = new PGSegment(pg1id, 0, new SortedIntArray(new int[] {p1id, p2id}));
        pgSegment1.addRecord(100, Arrays.asList(pv1_0, pv2_0));
        pgSegment1.consolidate();
        
        PGSegment pgSegment2 = new PGSegment(pg2id, 0, new SortedIntArray(new int[] {p1id}));
        pgSegment2.addRecord(200, Arrays.asList(pv1_1));
        pgSegment2.addRecord(300, Arrays.asList(pv1_2));
        pgSegment2.consolidate();
        
        parchive.writeToArchive(Arrays.asList(pgSegment1, pgSegment2));
        
        //ascending, retrieving one parameter from he group of two
        MultipleParameterValueRequest mpvr1a = new MultipleParameterValueRequest(0, TimeEncoding.MAX_INSTANT, new int[]{p1id}, new int[]{pg1id}, true);
        DataRetrieval dr1a = new DataRetrieval(parchive, mpvr1a);
        MultiValueConsumer c1a = new MultiValueConsumer();
        dr1a.retrieve(c1a);
        assertEquals(1, c1a.list.size());
        checkEquals(c1a.list.get(0), 100, pv1_0);
        
        //descending, retrieving one parameter from the group of two
        MultipleParameterValueRequest mpvr1d = new MultipleParameterValueRequest(0, TimeEncoding.MAX_INSTANT, new int[]{p1id}, new int[]{pg1id}, false);
        DataRetrieval dr1d = new DataRetrieval(parchive, mpvr1d);
        MultiValueConsumer c1d = new MultiValueConsumer();
        dr1d.retrieve(c1d);
        assertEquals(1, c1d.list.size());
        checkEquals(c1a.list.get(0), 100, pv1_0);
        
        //ascending, retrieving one para from the group of one
        MultipleParameterValueRequest mpvr2a = new MultipleParameterValueRequest(0, TimeEncoding.MAX_INSTANT, new int[]{p1id}, new int[]{pg2id}, true);
        DataRetrieval dr2a = new DataRetrieval(parchive, mpvr2a);
        MultiValueConsumer c2a = new MultiValueConsumer();
        dr2a.retrieve(c2a);
        assertEquals(2, c2a.list.size());
        checkEquals(c2a.list.get(0), 200, pv1_1);
        checkEquals(c2a.list.get(1), 300, pv1_2);
        
        //descending, retrieving one para from the group of one
        MultipleParameterValueRequest mpvr2d = new MultipleParameterValueRequest(0, TimeEncoding.MAX_INSTANT, new int[]{p1id}, new int[]{pg2id}, false);
        DataRetrieval dr2d = new DataRetrieval(parchive, mpvr2d);
        MultiValueConsumer c2d = new MultiValueConsumer();
        dr2d.retrieve(c2d);
        assertEquals(2, c2d.list.size());
        checkEquals(c2d.list.get(0), 300, pv1_2);
        checkEquals(c2d.list.get(1), 200, pv1_1);
        
        
        
        //ascending retrieving two para
        MultipleParameterValueRequest mpvr3a = new MultipleParameterValueRequest(0, TimeEncoding.MAX_INSTANT, new int[]{p1id, p2id}, new int[]{pg1id, pg1id}, true);
        DataRetrieval dr3a = new DataRetrieval(parchive, mpvr3a);
        MultiValueConsumer c3a = new MultiValueConsumer();
        dr3a.retrieve(c3a);
        assertEquals(1, c3a.list.size());
        checkEquals(c3a.list.get(0), 100, pv1_0, pv2_0);
        
        //descending retrieving two para
        MultipleParameterValueRequest mpvr3d = new MultipleParameterValueRequest(0, TimeEncoding.MAX_INSTANT, new int[]{p2id, p1id}, new int[]{pg1id, pg1id}, false);
        DataRetrieval dr3d = new DataRetrieval(parchive, mpvr3d);
        MultiValueConsumer c3d = new MultiValueConsumer();
        dr3d.retrieve(c3d);
        assertEquals(1, c3d.list.size());
        checkEquals(c3d.list.get(0), 100, pv1_0, pv2_0);
        
        //new value in a different segment but same partition
        long t2 = SortedTimeSegment.getSegmentEnd(0)+100;
        PGSegment pgSegment3 = new PGSegment(pg1id, SortedTimeSegment.getSegmentStart(t2), new SortedIntArray(new int[] {p1id, p2id}));
        ParameterValue pv1_3 = getParameterValue(p1, t2, "p1_blala_t2");
        ParameterValue pv2_1 = getParameterValue(p1, t2, "p1_blala_t2");
        pgSegment3.addRecord(t2, Arrays.asList(pv1_3, pv2_1));
        pgSegment3.consolidate();
        parchive.writeToArchive(Arrays.asList(pgSegment3));
                
        //new value in a different partition
        long t3 = ParameterArchive.Partition.getPartitionEnd(0)+100;
        PGSegment pgSegment4 = new PGSegment(pg1id, SortedTimeSegment.getSegmentStart(t3), new SortedIntArray(new int[] {p1id, p2id}));
        ParameterValue pv1_4 = getParameterValue(p1, t3, "p1_blalat3");
        ParameterValue pv2_2 = getParameterValue(p1, t3, "p2_blalat3");
        pgSegment4.addRecord(t3, Arrays.asList(pv1_4, pv2_2));
        pgSegment4.consolidate();
        parchive.writeToArchive(Arrays.asList(pgSegment4));
       
        //ascending retrieving two para
        MultipleParameterValueRequest mpvr4a = new MultipleParameterValueRequest(0, TimeEncoding.MAX_INSTANT, new int[]{p1id, p2id}, new int[]{pg1id, pg1id}, true);
        DataRetrieval dr4a = new DataRetrieval(parchive, mpvr4a);
        MultiValueConsumer c4a = new MultiValueConsumer();
        dr4a.retrieve(c4a);
        assertEquals(3, c4a.list.size());
        checkEquals(c4a.list.get(0), 100, pv1_0, pv2_0);
        checkEquals(c4a.list.get(1), t2, pv1_3, pv2_1);
        checkEquals(c4a.list.get(2), t3, pv1_4, pv2_2);
        
        //descending retrieving two para
        MultipleParameterValueRequest mpvr4d = new MultipleParameterValueRequest(0, TimeEncoding.MAX_INSTANT, new int[]{p2id, p1id}, new int[]{pg1id, pg1id}, false);
        DataRetrieval dr4d = new DataRetrieval(parchive, mpvr4d);
        MultiValueConsumer c4d = new MultiValueConsumer();
        dr4d.retrieve(c4d);
        assertEquals(3, c4d.list.size());
        checkEquals(c4d.list.get(0), t3, pv1_4, pv2_2);
        checkEquals(c4d.list.get(1), t2, pv1_3, pv2_1);
        checkEquals(c4d.list.get(2), 100, pv1_0, pv2_0);
        
    }
    
    
   

    class MultiValueConsumer implements Consumer<ParameterIdValueList> {
        List<ParameterIdValueList> list = new ArrayList<>();
        @Override
        public void accept(ParameterIdValueList x) {
            list.add(x);
        }
    }
}
