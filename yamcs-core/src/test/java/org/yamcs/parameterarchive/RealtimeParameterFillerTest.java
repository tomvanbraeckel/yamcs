package org.yamcs.parameterarchive;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.yamcs.ParameterValue;
import org.yamcs.YamcsServer;
import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.time.TimeService;
import org.yamcs.utils.FileUtils;
import org.yamcs.utils.TimeEncoding;
import org.yamcs.utils.ValueUtility;
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
    public void test1() throws Exception{
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
        ParameterValueRequest pvr0 = new ParameterValueRequest(0, 1000, pg12id, p1id, true, c0);
        filler.retrieveValues(pvr0);
        
        
        assertEquals(0, c0.times.size());
        
        
        filler.doUpdateItems(0, pvList);
        MyValueConsummer c1 = new MyValueConsummer();
        
        ParameterValueRequest pvr1 = new ParameterValueRequest(0, 1000, pg12id, p1id, true, c1);
        filler.retrieveValues(pvr1);
        checkEquals(c1, pv1_0);

        
        MyValueConsummer c2 = new MyValueConsummer();
        ParameterValueRequest pvr2 = new ParameterValueRequest(0, 1000, pg12id, p2id, true, c2);
        filler.retrieveValues(pvr2);
        checkEquals(c2, pv2_0);
        
        
        ParameterValue pv1_1 = getParameterValue(p1, 100, "blala1");
        ParameterValue pv2_1 =  getParameterValue(p2, 200, 12);
        filler.doUpdateItems(0, Arrays.asList(pv2_1, pv1_1));
        
        MyValueConsummer c3 = new MyValueConsummer();
        int pg1id = parchive.getParameterGroupIdMap().get(new int[]{p1id});
        ParameterValueRequest pvr3 = new ParameterValueRequest(0, 1000, pg1id, p1id, true, c3);
        filler.retrieveValues(pvr3);
        checkEquals(c3, pv1_1);
        
        
        MyValueConsummer c4 = new MyValueConsummer();
        int pg2id = parchive.getParameterGroupIdMap().get(new int[]{p2id});
        ParameterValueRequest pvr4 = new ParameterValueRequest(0, 1000, pg2id, p2id, true, c4);
        filler.retrieveValues(pvr4);
        checkEquals(c4, pv2_1);
        
        MyValueConsummer c5 = new MyValueConsummer();
        ParameterValueRequest pvr5 = new ParameterValueRequest(0, 1000, pg2id, p2id, false, c5);
        filler.retrieveValues(pvr5);
        checkEquals(c5, pv2_1);
        
        
        ParameterValue pv1_2 = getParameterValue(p1, 100, "blala2");
        ParameterValue pv2_2 =  getParameterValue(p2, 100, 14);
        filler.doUpdateItems(0, Arrays.asList(pv2_2, pv1_2));
        
        MyValueConsummer c6 = new MyValueConsummer();
        ParameterValueRequest pvr6 = new ParameterValueRequest(0, 1000, pg12id, p2id, true, c6);
        filler.retrieveValues(pvr6);        
        checkEquals(c6, pv2_0, pv2_2);
        
        
        MyValueConsummer c7 = new MyValueConsummer();
        ParameterValueRequest pvr7 = new ParameterValueRequest(0, 1000, pg12id, p2id, false, c7);
        filler.retrieveValues(pvr7);        
        checkEquals(c7, pv2_2, pv2_0);
        
        
        MyValueConsummer c8 = new MyValueConsummer();
        ParameterValueRequest pvr8 = new ParameterValueRequest(0, 100, pg12id, p2id, true, c8);
        filler.retrieveValues(pvr8);        
        checkEquals(c8, pv2_0);
        
        MyValueConsummer c9 = new MyValueConsummer();
        ParameterValueRequest pvr9 = new ParameterValueRequest(0, 100, pg12id, p2id, false, c9);
        filler.retrieveValues(pvr9);        
        checkEquals(c9, pv2_0);
        
        
        MyValueConsummer c10 = new MyValueConsummer();
        ParameterValueRequest pvr10 = new ParameterValueRequest(1, 100, pg12id, p2id, true, c10);
        filler.retrieveValues(pvr10);        
        assertEquals(0, c10.times.size());
        
        MyValueConsummer c11 = new MyValueConsummer();
        ParameterValueRequest pvr11 = new ParameterValueRequest(1, 100, pg12id, p2id, false, c11);
        filler.retrieveValues(pvr11);
        assertEquals(0, c11.times.size());
        
        
        MyValueConsummer c12 = new MyValueConsummer();
        ParameterValueRequest pvr12 = new ParameterValueRequest(1, 101, pg12id, p2id, true, c12);
        filler.retrieveValues(pvr12);        
        checkEquals(c12, pv2_2);
        
        MyValueConsummer c13 = new MyValueConsummer();
        ParameterValueRequest pvr13 = new ParameterValueRequest(1, 101, pg12id, p2id, false, c13);
        filler.retrieveValues(pvr13);        
        checkEquals(c13, pv2_2);
        
        timeService.missionTime = RealtimeParameterFiller.CONSOLIDATE_OLDER_THAN;
        filler.doHouseKeeping();
        
        MyValueConsummer c14 = new MyValueConsummer();
        ParameterValueRequest pvr14 = new ParameterValueRequest(1, 101, pg12id, p2id, false, c14);
        filler.retrieveValues(pvr14);        
        assertEquals(0, c14.times.size());
    }
    
    void checkEquals(MyValueConsummer c, ParameterValue...pvs) {
        assertEquals(pvs.length, c.times.size());
        for(int i=0; i<pvs.length; i++) {
            ParameterValue pv = pvs[i];
            assertEquals(pv.getAcquisitionTime(), (long)c.times.get(i));
            assertEquals(pv.getEngValue(), c.values.get(i));
        }
    }
    ParameterValue getParameterValue(Parameter p, long instant, int intv) {
        ParameterValue pv = new ParameterValue(p);
        pv.setAcquisitionTime(instant);
        Value v = ValueUtility.getSint32Value(intv);
        pv.setEngineeringValue(v);
        return pv;
    }
    
    ParameterValue getParameterValue(Parameter p, long instant, String sv) {
        ParameterValue pv = new ParameterValue(p);
        pv.setAcquisitionTime(instant);
        Value v = ValueUtility.getStringValue(sv);
        pv.setEngineeringValue(v);
        return pv;
    }
    
    private static class MyValueConsummer implements ValueConsumer {
        ArrayList<Long> times = new ArrayList<>();
        ArrayList<Value> values = new ArrayList<>();
       
        @Override
        public void addValue(long t, Value v) {
            times.add(t);
            values.add(v);
        }
        
    }
    
    private static class MockupTimeService implements TimeService {
        long missionTime=0;
        @Override
        public long getMissionTime() {
            return missionTime;
        }
    }
}
