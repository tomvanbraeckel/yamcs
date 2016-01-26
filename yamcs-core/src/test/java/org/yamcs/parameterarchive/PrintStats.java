package org.yamcs.parameterarchive;

import java.io.PrintStream;

import org.junit.Ignore;
import org.junit.Test;
import org.yamcs.utils.TimeEncoding;
import org.yamcs.yarch.YarchDatabase;

public class PrintStats {
    @Test
    public void test1() throws Exception {
        TimeEncoding.setUp();
        YarchDatabase.setHome("/storage/yamcs-data");
        
        ParameterArchive parchive = new ParameterArchive("aces-ops");
        PrintStream ps = new PrintStream("/tmp/aces-ops1-paraid.txt");
        //parchive.printKeys(ps);
        parchive.getParameterIdDb().print(ps);
        ps.close();
        parchive.close();
    }
}
