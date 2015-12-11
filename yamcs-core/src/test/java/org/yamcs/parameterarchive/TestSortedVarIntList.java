package org.yamcs.parameterarchive;

import static org.junit.Assert.*;

import org.junit.Test;
import org.yamcs.parameterarchive.VarIntList.IntIterator;

public class TestSortedVarIntList {

    @Test
    public void testNotSorted() {
        Exception e = null;
        try {
            new SortedVarIntList(new int[]{3,1});
        } catch (IllegalArgumentException e1) {
            e = e1;
        }
        assertNotNull(e);
    }
    
    @Test
    public void testSorted() {
        int n = 1000;
        int[] a = new int[n];
        for(int i=0;i<n;i++) {
            a[i]=1000+i;
        }
        
        
        SortedVarIntList vil = new SortedVarIntList(a);
        assertEquals(n+1, vil.arraySize());

        IntIterator it = vil.iterator();
        for(int i = 0; i<n; i++) {
            assertTrue(it.hasNext());
            int v = it.next();
            assertEquals(a[i], v); 
        }
        assertFalse(it.hasNext());
    }
    

}
