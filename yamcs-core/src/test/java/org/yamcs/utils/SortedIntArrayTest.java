package org.yamcs.utils;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

public class SortedIntArrayTest {
    @Test
    public void test1() {
        SortedIntArray s = new SortedIntArray();
        assertEquals(0, s.size());

        s.add(2);
        assertEquals(1, s.size());
        assertEquals(2, s.get(0));

        s.add(3);
        assertEquals(2, s.size());
        assertEquals(2, s.get(0));
        assertEquals(3, s.get(1));

        s.add(1);
        assertEquals(3, s.size());
        assertEquals(1, s.get(0));
        assertEquals(2, s.get(1));
        assertEquals(3, s.get(2));

    }

    @Test
    public void test2() {
        SortedIntArray s = new SortedIntArray();
        assertEquals(0, s.size());
        int n = 1000;
        for(int i =0; i<n/2; i++) {
            s.add(i);
        }
        for(int i=n-1; i>=n/2; i--) {
            s.add(i);
        }
        assertEquals(n, s.size());

        for(int i=0;i<n;i++) {
            assertEquals(i, s.get(i));
        }
    }
    
    @Ignore
    @Test
    public void testperf() {
        Runtime runtime = Runtime.getRuntime();
        System.out.println("allocated memory (KB): "+runtime.totalMemory()/1024);
        
        int n = 10000000;
        SortedIntArray sia = new SortedIntArray();
        long t0=System.currentTimeMillis();
        for(int i =0; i<n; i++) {
            sia.add(i);
        }
        System.out.println("Populate sortedintarray: "+(System.currentTimeMillis()-t0)+" ms");
        System.out.println("allocated memory (KB): "+runtime.totalMemory()/1024);
        
        long t1=System.currentTimeMillis();
        Set<Integer> set= new HashSet<Integer>();
        for(int i=0;i<n;i++) {
            set.add(sia.get(i));
        }
        System.out.println("Populate hashset: "+(System.currentTimeMillis()-t1)+" ms");
        System.out.println("allocated memory (KB): "+runtime.totalMemory()/1024);
        
        for (int k=0; k<20; k++) {
            long t2=System.currentTimeMillis();
            long sum =0;
            for(int i=0;i<n;i++) {
                if(sia.search(i)>=0) {
                    sum+=i;
                }
            }
            System.out.println("sum: "+sum+" Search in sorted array: "+(System.currentTimeMillis()-t2)+" ms");
            System.out.println("allocated memory (KB): "+runtime.totalMemory()/1024);
            long t3=System.currentTimeMillis();
            sum =0;
            for(int i=0;i<n;i++) {
                if(set.contains(i)) {
                    sum+=i;
                }
            }
            System.out.println("sum: "+sum+" Search in hashset: "+(System.currentTimeMillis()-t3)+" ms");
            System.out.println("allocated memory (KB): "+runtime.totalMemory()/1024);
        }
    }
}
