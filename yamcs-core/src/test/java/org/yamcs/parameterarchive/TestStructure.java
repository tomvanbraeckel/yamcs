package org.yamcs.parameterarchive;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;

import org.junit.Test;
import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksIterator;
import org.rocksdb.TableFormatConfig;
import org.yamcs.utils.FileUtils;

import static org.junit.Assert.*;

public class TestStructure {
    int numParams = 1000; //number of separate parameters

    int numSamplesPerBloc = 900; //number of samples per block

    int numBlocks = 30*24*3600/numSamplesPerBloc; // 30 days of data
    String path = "/tmp/testparamsinonecf";
    String datacf = "data";
    
    RocksDB rdb;
    ColumnFamilyHandle  cfh;
    
    void populate() throws Exception {
        rdb = RocksDB.open(path);
        ColumnFamilyOptions cfo = new ColumnFamilyOptions();

        cfo.optimizeLevelStyleCompaction();
        ColumnFamilyDescriptor cfd = new ColumnFamilyDescriptor(datacf.getBytes(), cfo);
        ColumnFamilyHandle cfh = rdb.createColumnFamily(cfd);


        for(int t=0;t<numBlocks; t++) {
            if(t%100==0)      System.out.println("numBlock: "+t+" "+100.0*t/numBlocks+"%");
            for(int pid=0; pid<numParams; pid++) {
                rdb.put(cfh, key(pid, t), getBlock(numSamplesPerBloc));
            }
        }
        // rdb.compactRange(cfh);
        System.out.println("stats: "+rdb.getProperty(cfh, "rocksdb.stats"));
        System.out.println("cur-size-all-mem-tables: "+rdb.getProperty(cfh, "rocksdb.cur-size-all-mem-tables"));
        rdb.close();
    }

    void openDbForReading() throws Exception{
        ColumnFamilyOptions cfo = new ColumnFamilyOptions();
        cfo.optimizeLevelStyleCompaction();
        ColumnFamilyDescriptor cfd = new ColumnFamilyDescriptor(datacf.getBytes(), cfo);
        ArrayList<ColumnFamilyDescriptor> cfdList = new ArrayList<ColumnFamilyDescriptor>();
        cfdList.add(cfd);
        cfdList.add(new ColumnFamilyDescriptor("default".getBytes()));
        ArrayList<ColumnFamilyHandle> cfhList = new ArrayList<ColumnFamilyHandle>();

        rdb = RocksDB.open(path, cfdList, cfhList);
        cfh =  cfhList.get(0);
        System.out.println("stats: "+rdb.getProperty(cfh, "rocksdb.stats"));
        
    }
    
    void scanParameters() throws Exception {
        openDbForReading();
        
      //  Thread.sleep(100000);
        for(int pid=0; pid<numParams; pid++) {
            if(pid%100==0) 
                System.out.println("pid: "+pid);
            RocksIterator it = rdb.newIterator(cfh);        
            it.seek(key(pid, 0));
            int length =0;
            while(it.isValid()) {
                byte[] k = it.key();
                byte[] v = it.value();

                int pid1 = ByteBuffer.wrap(k).getInt();
                if(pid1!=pid) break;
                length+=v.length;
                it.next();
            }
            assertEquals(4*numSamplesPerBloc*numBlocks, length);
            it.dispose();
        }

        rdb.close();
    }
    
    void compact()  throws Exception  {
        openDbForReading();
        rdb.compactRange(cfh);
        rdb.close();
    }

    @Test
    public void testAllParamsInOneCf() throws Exception {
        System.out.println("------------ numParams: "+numParams+" numBlocks: "+numBlocks+" numSamplesPerBloc: "+ numSamplesPerBloc);        
        
        FileUtils.deleteRecursively(new File(path).toPath());
        
        long t0=System.currentTimeMillis();
      //  populate();
        long timeToPopulate = System.currentTimeMillis() - t0;                 
        System.out.println("time to populate: "+(timeToPopulate/(1000))+" sec");
        
        Thread.sleep(10000);       
        t0=System.currentTimeMillis();
        compact();
        long timeToCompact = System.currentTimeMillis() - t0;        
        System.out.println("time to compact: "+(timeToCompact/(1000))+" sec");
        
        
        Thread.sleep(10000);
        t0=System.currentTimeMillis();
        scanParameters();
        long timeToScan = System.currentTimeMillis() - t0;
        System.out.println("time to scan all params "+(timeToScan/(1000))+" sec");
    }


    byte[] key(int pid, long t) {
        return ByteBuffer.allocate(12).putInt(pid).putLong(t).array();

    }

    byte[] getBlock(int numSamplesPerBlock) {
        byte[] buf = new byte[4*numSamplesPerBlock];
        ByteBuffer bb = ByteBuffer.wrap(buf);
        float x = 20;
        Random rand = new Random();
        for(int i=0; i<numSamplesPerBlock; i++) {
            bb.putFloat(x+5*rand.nextFloat());
        }
        return buf;
    }
}
