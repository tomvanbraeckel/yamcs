package org.yamcs.utils;

import java.util.Arrays;

/**
 * sorted int array
 * 
 * 
 * copy on write
 * @author nm
 *
 */
public class SortedIntArray {
    public static int DEFAULT_CAPACITY = 10;
    private int[] a;
    private int length;

    /**
     * Creates a sorted int array with a default initial capacity
     * 
     * @param capacity
     */
    public SortedIntArray() {
        a = new int[DEFAULT_CAPACITY];
    }
    
    /**
     * Creates a sorted int array with a given initial capacity
     * 
     * @param capacity
     */
    public SortedIntArray(int capacity) {
        a = new int[capacity];
    }

    /**
     * add value to the array and return the position on which has been added
     * 
     * @param id
     */
    public int add(int x) {
        int pos = Arrays.binarySearch(a, 0, length, x);
        if( pos<0 ) pos = -pos-1;

        ensureCapacity(length+1);
        
        System.arraycopy(a, pos, a, pos+1, length-pos);
        a[pos] = x;
        length++;
        
        return pos;
    }

    /**
     * performs a binary search in the array. 
     * 
     * @see java.util.Arrays#binarySearch(int[], int)
     * @param x
     * @return
     */
    public int search(int x) {
        return  Arrays.binarySearch(a, 0, length, x);
    }
    /**
     * get element at position
     * @param pos
     * @return
     */
    public int get(int pos) {
        if(pos >= length) throw new IndexOutOfBoundsException("Index: "+pos+" length: "+length);
        
        return a[pos];
    }
    
    private void ensureCapacity(int minCapacity) {
        if(minCapacity<=a.length) return;

        int capacity = a.length;
        int newCapacity = capacity + (capacity >> 1);
        if(newCapacity<minCapacity) newCapacity = minCapacity;
        
        a = Arrays.copyOf(a, newCapacity);
    }
  

    public boolean isEmpty() {	
        return a.length==0;
    }

    public int[] getArray() {
        return a;
    }
    public int size() {
        return length;
    }

    public String toString() {
        return Arrays.toString(a);
    }
}
