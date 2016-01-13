package org.yamcs.parameterarchive;

import org.junit.Ignore;
import org.junit.Test;
import org.yamcs.utils.TimeEncoding;

public class PrintStats {
    @Test
    @Ignore
    public void test1() throws Exception {
        TimeEncoding.setUp();
        ParameterArchive parchive = new ParameterArchive("IntegrationTest");
        parchive.printKeys(System.out);
        parchive.printStats(System.out);
        parchive.close();
    }
}
