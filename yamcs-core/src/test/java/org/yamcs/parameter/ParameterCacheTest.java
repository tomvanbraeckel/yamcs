package org.yamcs.parameter;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.yamcs.ParameterValue;
import org.yamcs.utils.ValueUtility;
import org.yamcs.xtce.Parameter;

public class ParameterCacheTest {
    Parameter p1 = new Parameter("p1");
    Parameter p2 = new Parameter("p2");
    
    @Test
    public void test1() {
        //100 ms
        ParameterCache pcache = new ParameterCache(1000); //1 second
        assertNull(pcache.getLastValue(p1));
        
        ParameterValue p1v1 = getParameterValue(p1, 10);
        ParameterValue p2v1 = getParameterValue(p2, 10);
        pcache.update(Arrays.asList(p1v1, p2v1));
        
        assertEquals(p1v1, pcache.getLastValue(p1));
        assertEquals(p2v1, pcache.getLastValue(p2));
        
        
        ParameterValue p1v2 = getParameterValue(p1, 20);
        pcache.update(Arrays.asList(p1v2));
        
        assertEquals(p1v2, pcache.getLastValue(p1));
        assertEquals(p2v1, pcache.getLastValue(p2));
        
        
        List<ParameterValue> pvlist = pcache.getValues(Arrays.asList(p1, p2));
        checkEquals(pvlist, p1v2, p2v1);
        
        pvlist = pcache.getValues(Arrays.asList(p2, p1));
        checkEquals(pvlist, p2v1, p1v1);
        
    }
    
    
    ParameterValue getParameterValue(Parameter p, long timestamp) {
        ParameterValue pv = new ParameterValue(p);
        pv.setAcquisitionTime(timestamp);
        pv.setEngineeringValue(ValueUtility.getStringValue(p.getName()+"_"+timestamp));
        return pv;
    }
    
    public static void checkEquals(List<ParameterValue> actual, ParameterValue... expected) {
        assertEquals(expected.length, actual.size());
        for(int i=0; i<expected.length; i++) {
            ParameterValue pv = expected[i];
            assertEquals(pv, actual.get(i));
        }
    }
} 
