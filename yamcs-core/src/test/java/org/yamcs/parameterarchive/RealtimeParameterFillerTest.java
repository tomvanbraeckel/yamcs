package org.yamcs.parameterarchive;

import static org.junit.Assert.*;
import static org.yamcs.parameterarchive.TestUtils.*;

import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.yamcs.ParameterValue;
import org.yamcs.YamcsServer;
import org.yamcs.utils.FileUtils;
import org.yamcs.utils.TimeEncoding;
import org.yamcs.xtce.Parameter;
import org.yamcs.yarch.YarchDatabase;

public class RealtimeParameterFillerTest {
    String instance = "RealtimeParameterFillerTest";
    static Parameter p1, p2,p3,p4,p5;
    static MockupTimeService timeService;

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
    public void testSingleParameter() throws Exception{
        String dbroot = YarchDatabase.getInstance(instance).getRoot();
        FileUtils.deleteRecursively(dbroot+"/"+instance);

        ParameterArchive parchive = new ParameterArchive(instance);
        timeService.missionTime = 0;

        RealtimeParameterFiller filler = new RealtimeParameterFiller(parchive);
        filler.startAsync().awaitRunning();

        ParameterValue pv1_0 = getParameterValue(p1, 100, "blala0");


        int p1id = parchive.getParameterIdMap().get(p1.getQualifiedName(), pv1_0.getEngValue().getType());

        int pg1id = parchive.getParameterGroupIdMap().get(new int[]{p1id});


        //ascending request on empty data
        MyValueConsummer c0a = new MyValueConsummer();
        SingleParameterValueRequest pvr0a = new SingleParameterValueRequest(0, 1000, pg1id, p1id, true, c0a);
        filler.retrieveValues(pvr0a);
        assertEquals(0, c0a.times.size());

        //descending request on empty data
        MyValueConsummer c0d = new MyValueConsummer();
        SingleParameterValueRequest pvr0d = new SingleParameterValueRequest(0, 1000, pg1id, p1id, true, c0d);
        filler.retrieveValues(pvr0d);
        assertEquals(0, c0d.times.size());


        //add one value
        filler.doUpdateItems(0, Arrays.asList(pv1_0));
        
        //ascending request on one value
        MyValueConsummer c1a = new MyValueConsummer();       
        SingleParameterValueRequest pvr1a = new SingleParameterValueRequest(0, 1000, pg1id, p1id, true, c1a);
        filler.retrieveValues(pvr1a);
        checkEquals(c1a, pv1_0);
        
        //descending request on one value
        MyValueConsummer c1d = new MyValueConsummer();       
        SingleParameterValueRequest pvr1d = new SingleParameterValueRequest(0, 1000, pg1id, p1id, false, c1d);
        filler.retrieveValues(pvr1d);
        checkEquals(c1d, pv1_0);
        
        //ascending request on one value with start fixed on the value timestamp
        MyValueConsummer c2a = new MyValueConsummer();       
        SingleParameterValueRequest pvr2a = new SingleParameterValueRequest(100, 1000, pg1id, p1id, true, c2a);
        filler.retrieveValues(pvr2a);
        checkEquals(c2a, pv1_0);
        
        //descending request on one value with start fixed on the value timestamp
        MyValueConsummer c2d = new MyValueConsummer();       
        SingleParameterValueRequest pvr2d = new SingleParameterValueRequest(0, 100, pg1id, p1id, false, c2d);
        filler.retrieveValues(pvr2d);
        checkEquals(c2d, pv1_0);
        
        
        //ascending request on one value with stop fixed on the value timestamp
        MyValueConsummer c3a = new MyValueConsummer();       
        SingleParameterValueRequest pvr3a = new SingleParameterValueRequest(0, 100, pg1id, p1id, true, c3a);
        filler.retrieveValues(pvr3a);
        assertEquals(0, c3a.times.size());
        
        //descending request on one value with stop fixed on the value timestamp
        MyValueConsummer c3d = new MyValueConsummer();       
        SingleParameterValueRequest pvr3d = new SingleParameterValueRequest(1000, 100, pg1id, p1id, false, c3d);
        filler.retrieveValues(pvr3d);
        assertEquals(0, c3d.times.size());
        
        //one more value
        ParameterValue pv1_1 = getParameterValue(p1, 200, "blala200");
        filler.doUpdateItems(0, Arrays.asList(pv1_1));

        //ascending request on two value
        MyValueConsummer c4a = new MyValueConsummer();       
        SingleParameterValueRequest pvr4a = new SingleParameterValueRequest(0, 1000, pg1id, p1id, true, c4a);
        filler.retrieveValues(pvr4a);
        checkEquals(c4a, pv1_0, pv1_1);
        
        //descending request on two value
        MyValueConsummer c4d = new MyValueConsummer();       
        SingleParameterValueRequest pvr4d = new SingleParameterValueRequest(0, 1000, pg1id, p1id, false, c4d);
        filler.retrieveValues(pvr4d);
        checkEquals(c4d, pv1_1, pv1_0);
        
        //ascending request on two value with start on first value
        MyValueConsummer c5a = new MyValueConsummer();       
        SingleParameterValueRequest pvr5a = new SingleParameterValueRequest(100, 1000, pg1id, p1id, true, c5a);
        filler.retrieveValues(pvr5a);
        checkEquals(c5a, pv1_0, pv1_1);
        
        //descending request on two value with start on second value
        MyValueConsummer c5d = new MyValueConsummer();       
        SingleParameterValueRequest pvr5d = new SingleParameterValueRequest(0, 200, pg1id, p1id, false, c5d);
        filler.retrieveValues(pvr5d);
        checkEquals(c5d, pv1_1, pv1_0);
        
        //ascending request on two value with start on the first value and stop on second 
        MyValueConsummer c6a = new MyValueConsummer();       
        SingleParameterValueRequest pvr6a = new SingleParameterValueRequest(100, 200, pg1id, p1id, true, c6a);
        filler.retrieveValues(pvr6a);
        checkEquals(c6a, pv1_0);
        
        //descending request on two value with start on the second value and stop on first 
        MyValueConsummer c6d = new MyValueConsummer();       
        SingleParameterValueRequest pvr6d = new SingleParameterValueRequest(100, 200, pg1id, p1id, false, c6d);
        filler.retrieveValues(pvr6d);
        checkEquals(c6d, pv1_1);
        
    }


    @Test
    public void testWithTwoParams() throws Exception{
        String dbroot = YarchDatabase.getInstance(instance).getRoot();
        FileUtils.deleteRecursively(dbroot+"/"+instance);

        ParameterArchive parchive = new ParameterArchive(instance);
        timeService.missionTime = 0;

        RealtimeParameterFiller filler = new RealtimeParameterFiller(parchive);
        filler.startAsync().awaitRunning();

        ParameterValue pv1_0 = getParameterValue(p1, 0, "blala0");
        ParameterValue pv2_0 = getParameterValue(p2, 0, 10);


        List<ParameterValue> pvList = Arrays.asList(pv1_0, pv2_0);
        int p1id = parchive.getParameterIdMap().get(p1.getQualifiedName(), pv1_0.getEngValue().getType());
        int p2id = parchive.getParameterIdMap().get(p2.getQualifiedName(), pv2_0.getEngValue().getType());

        MyValueConsummer c0 = new MyValueConsummer();
        int pg12id = parchive.getParameterGroupIdMap().get(new int[]{p1id, p2id});
        SingleParameterValueRequest pvr0 = new SingleParameterValueRequest(0, 1000, pg12id, p1id, true, c0);
        filler.retrieveValues(pvr0);


        assertEquals(0, c0.times.size());


        filler.doUpdateItems(0, pvList);
        MyValueConsummer c1 = new MyValueConsummer();       
        SingleParameterValueRequest pvr1 = new SingleParameterValueRequest(0, 1000, pg12id, p1id, true, c1);
        filler.retrieveValues(pvr1);
        checkEquals(c1, pv1_0);

        MyValueConsummer c1d = new MyValueConsummer();       
        SingleParameterValueRequest pvr1d = new SingleParameterValueRequest(-1, 1000, pg12id, p1id, false, c1d);
        filler.retrieveValues(pvr1d);
        checkEquals(c1d, pv1_0);


        MyValueConsummer c2 = new MyValueConsummer();
        SingleParameterValueRequest pvr2 = new SingleParameterValueRequest(0, 1000, pg12id, p2id, true, c2);
        filler.retrieveValues(pvr2);
        checkEquals(c2, pv2_0);


        //add two values with different timestamps
        ParameterValue pv1_1 = getParameterValue(p1, 100, "blala1");
        ParameterValue pv2_1 =  getParameterValue(p2, 200, 12);
        filler.doUpdateItems(0, Arrays.asList(pv2_1, pv1_1));

        MyValueConsummer c3 = new MyValueConsummer();
        int pg1id = parchive.getParameterGroupIdMap().get(new int[]{p1id});
        SingleParameterValueRequest pvr3 = new SingleParameterValueRequest(0, 1000, pg1id, p1id, true, c3);
        filler.retrieveValues(pvr3);
        checkEquals(c3, pv1_1);


        MyValueConsummer c4 = new MyValueConsummer();
        int pg2id = parchive.getParameterGroupIdMap().get(new int[]{p2id});
        SingleParameterValueRequest pvr4 = new SingleParameterValueRequest(0, 1000, pg2id, p2id, true, c4);
        filler.retrieveValues(pvr4);
        checkEquals(c4, pv2_1);

        MyValueConsummer c5 = new MyValueConsummer();
        SingleParameterValueRequest pvr5 = new SingleParameterValueRequest(0, 1000, pg2id, p2id, false, c5);
        filler.retrieveValues(pvr5);
        checkEquals(c5, pv2_1);


        //two more parameters with the same timestamp
        ParameterValue pv1_2 = getParameterValue(p1, 100, "blala2");
        ParameterValue pv2_2 =  getParameterValue(p2, 100, 14);
        filler.doUpdateItems(0, Arrays.asList(pv2_2, pv1_2));

        MyValueConsummer c6 = new MyValueConsummer();
        SingleParameterValueRequest pvr6 = new SingleParameterValueRequest(0, 1000, pg12id, p2id, true, c6);
        filler.retrieveValues(pvr6);        
        checkEquals(c6, pv2_0, pv2_2);


        MyValueConsummer c7 = new MyValueConsummer();
        SingleParameterValueRequest pvr7 = new SingleParameterValueRequest(-1, 1000, pg12id, p2id, false, c7);
        filler.retrieveValues(pvr7);        
        checkEquals(c7, pv2_2, pv2_0);


        MyValueConsummer c8 = new MyValueConsummer();
        SingleParameterValueRequest pvr8 = new SingleParameterValueRequest(0, 100, pg12id, p2id, true, c8);
        filler.retrieveValues(pvr8);        
        checkEquals(c8, pv2_0);

        MyValueConsummer c9 = new MyValueConsummer();
        SingleParameterValueRequest pvr9 = new SingleParameterValueRequest(0, 100, pg12id, p2id, false, c9);
        filler.retrieveValues(pvr9);        
        checkEquals(c9, pv2_2);


        MyValueConsummer c10 = new MyValueConsummer();
        SingleParameterValueRequest pvr10 = new SingleParameterValueRequest(1, 100, pg12id, p2id, true, c10);
        filler.retrieveValues(pvr10);        
        assertEquals(0, c10.times.size());

        MyValueConsummer c11 = new MyValueConsummer();
        SingleParameterValueRequest pvr11 = new SingleParameterValueRequest(0, 99, pg12id, p2id, false, c11);
        filler.retrieveValues(pvr11);
        assertEquals(0, c11.times.size());


        MyValueConsummer c12 = new MyValueConsummer();
        SingleParameterValueRequest pvr12 = new SingleParameterValueRequest(1, 101, pg12id, p2id, true, c12);
        filler.retrieveValues(pvr12);        
        checkEquals(c12, pv2_2);

        MyValueConsummer c13 = new MyValueConsummer();
        SingleParameterValueRequest pvr13 = new SingleParameterValueRequest(1, 101, pg12id, p2id, false, c13);
        filler.retrieveValues(pvr13);        
        checkEquals(c13, pv2_2);

        timeService.missionTime = RealtimeParameterFiller.CONSOLIDATE_OLDER_THAN;
        filler.doHouseKeeping();

        MyValueConsummer c14 = new MyValueConsummer();
        SingleParameterValueRequest pvr14 = new SingleParameterValueRequest(1, 101, pg12id, p2id, false, c14);
        filler.retrieveValues(pvr14);        
        assertEquals(0, c14.times.size());
    }

   

}
