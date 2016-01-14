package org.yamcs.utils;

import java.util.Arrays;

/**
 * int array
 * 
 * 
 * @author nm
 *
 */
public class IntArray {
  
    public static int DEFAULT_CAPACITY = 10;
    private int[] a;
    private int length;
    
    //caches the hashCode
    private int hash;
    
    
    /**
     * Creates a sorted int array with a default initial capacity
     * 
     * @param capacity
     */
    public IntArray() {
        a = new int[DEFAULT_CAPACITY];
    }
    
    /**
     * Creates an IntArray with a given initial capacity
     * 
     * @param capacity
     */
    public IntArray(int capacity) {
        a = new int[capacity];
    }
    
    /**
     * Creates the IntArray by copying all values from the input array and sorting them
     * 
     * @param array
     */
    public IntArray(int... array) {
        length = array.length;
        a = Arrays.copyOf(array, length);
    }

    /**
     * add value to the array 
     * 
     * @param id
     */
    public void add(int x) {
        ensureCapacity(length+1);
        a[length] = x;
        length++;
        
    }
    /**
     * get element at position
     * @param pos
     * @return
     */
    public int get(int pos) {
        rangeCheck(pos);

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

    public int[] toArray() {
        return Arrays.copyOf(a, length);
    }
    
    public int size() {
        return length;
    }

    public void set(int pos, int x) {
        rangeCheck(pos);
        a[pos] = x;
    }
    
    private void rangeCheck(int pos) {
        if(pos >= length) throw new IndexOutOfBoundsException("Index: "+pos+" length: "+length);
    }
    
    
    public String toString() {        
        StringBuilder b = new StringBuilder();
        int n = length-1;
        
        b.append('[');
        for (int i = 0;; i++) {
            b.append(a[i]);
            if(i==n)
                return b.append(']').toString();
            b.append(", ");
        }
    }
    
    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0 && length > 0) {
            h = 1;
            
            for (int i = 0; i < length; i++) {
                h = 31 * h + a[i];
            }
            hash = h;
        }
        return h;
    }
  
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        
        if (obj == null) return false;
        
        if (getClass() != obj.getClass()) return false;
        
        IntArray other = (IntArray) obj;
        if (length != other.length) return false;
        
        for(int i=0; i<length; i++) {
            if(a[i]!=other.a[i])    return false;
        }
       
        return true;
    }

   
}
