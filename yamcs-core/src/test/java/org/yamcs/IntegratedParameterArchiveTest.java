package org.yamcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.yamcs.parameterarchive.ParameterArchive;
import org.yamcs.utils.TimeEncoding;

public class IntegratedParameterArchiveTest extends AbstractIntegrationTest {
    
    private void generateData(String utcStart, int numPackets) {
        long t0 = TimeEncoding.parse(utcStart);
        for (int i=0;i <numPackets; i++) {
                packetGenerator.setGenerationTime(t0+1000*i);
            packetGenerator.generate_PKT1_1();
            packetGenerator.generate_PKT1_3();
        }
    }
    
    
    @Test
    public void testReplayFillup() throws Exception {
        generateData("2015-01-02T10:00:00", 3600);
        ParameterArchive parameterArchive = YamcsServer.getService(yamcsInstance, ParameterArchive.class);
        Future<?> f = parameterArchive.scheduleFilling(TimeEncoding.parse("2015-01-02T10:00:00"), TimeEncoding.parse("2015-01-02T11:00:00"));
        f.get(10, TimeUnit.SECONDS);
        
        
        
    }

}
