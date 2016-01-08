package org.yamcs.parameterarchive;

import static org.junit.Assert.*;

import org.yamcs.ParameterValue;
import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.protobuf.Yamcs.Value.Type;
import org.yamcs.utils.ValueUtility;
import org.yamcs.xtce.Parameter;

public class TestUtils {
    static ParameterValue getParameterValue(Parameter p, long instant, int intv) {
        ParameterValue pv = new ParameterValue(p);
        pv.setAcquisitionTime(instant);
        Value v = ValueUtility.getSint32Value(intv);
        pv.setEngineeringValue(v);
        return pv;
    }

    static ParameterValue getParameterValue(Parameter p, long instant, String sv) {
        ParameterValue pv = new ParameterValue(p);
        pv.setAcquisitionTime(instant);
        Value v = ValueUtility.getStringValue(sv);
        pv.setEngineeringValue(v);
        return pv;
    }

    static void checkEquals(ParameterValueArray pva, ParameterValue...pvs) {
        assertEquals(pvs.length, pva.timestamps.length);
        for(int i=0; i<pvs.length; i++) {
            ParameterValue pv = pvs[i];
            assertEquals(pv.getAcquisitionTime(), pva.timestamps[i]);
        }
        Value v = pvs[0].getEngValue();
        if(v.getType()==Type.STRING) {
            String[] s = (String[]) pva.values;
            for(int i=0; i<pvs.length; i++) {
                v = pvs[i].getEngValue();
                assertEquals(v.getStringValue(), s[i]);
            }            
        } else {
            fail("check for "+v.getType()+" not implemented");
        }
                 
            //assertEquals(pv.getEngValue(), pva.values[i]);
    }


    static void checkEquals(ParameterIdValueList plist, long expectedTime, ParameterValue... expectedPv) {
        assertEquals(expectedTime, plist.instant);
        assertEquals(expectedPv.length, plist.values.size());
        for(int i=0; i<expectedPv.length; i++) {
            ParameterValue pv = expectedPv[i];
            Value v = plist.values.get(i);
            assertEquals(pv.getEngValue(), v);
        }
    }

    static void checkEquals(MyValueConsummer c, ParameterValue...pvs) {
        assertEquals(pvs.length, c.times.size());
        for(int i=0; i<pvs.length; i++) {
            ParameterValue pv = pvs[i];
            assertEquals(pv.getAcquisitionTime(), (long)c.times.get(i));
            assertEquals(pv.getEngValue(), c.values.get(i));
        }
    }
}
