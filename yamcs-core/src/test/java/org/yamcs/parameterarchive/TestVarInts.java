package org.yamcs.parameterarchive;

import static org.junit.Assert.*;

import org.junit.Test;
import org.yamcs.parameterarchive.VarIntList.IntIterator;

public class TestVarInts {
    @Test
    public void test1() {
        int n = 3600;
        int[] a = new int[n];
        for(int i=0;i<n;i++) {
            a[i]=1000+i;
        }
        
        VarIntList vil = new VarIntList(a);
        assertEquals(2*n, vil.arraySize());

        IntIterator it = vil.iterator();
        for(int i = 0; i<n; i++) {
            assertTrue(it.hasNext());
            int v = it.next();
            assertEquals(a[i], v); 
        }
        assertFalse(it.hasNext());
    }
    
    
    @Test
    public void test2() {
        int n = 100;
        int[] a = new int[n];
        for(int i=0;i<n;i++) {
            a[i]=i;
        }
        
        VarIntList vil = new VarIntList(a);
        assertEquals(n, vil.arraySize());
        
        IntIterator it = vil.iterator();
        for(int i = 0; i<n; i++) {
            assertTrue(it.hasNext());
            int v = it.next();
            assertEquals(a[i], v); 
        }
        assertFalse(it.hasNext());
    }
            
    @Test
    public void test3() {
        int n = 100;
        int[] a = new int[n];
        for(int i=0;i<n;i++) {
            a[i]=100000 + i;
        }
        
        VarIntList vil = new VarIntList(a);
        assertEquals(3*n, vil.arraySize());
        
        IntIterator it = vil.iterator();
        for(int i = 0; i<n; i++) {
            assertTrue(it.hasNext());
            int v = it.next();
            assertEquals(a[i], v); 
        }
        assertFalse(it.hasNext());
    }
    
    
}
