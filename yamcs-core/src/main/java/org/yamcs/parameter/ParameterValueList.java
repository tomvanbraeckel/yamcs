package org.yamcs.parameter;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.yamcs.ParameterValue;
import org.yamcs.xtce.Parameter;

/**
 * 
 * Stores a collection of ParameterValue indexed on Parameter
 * 
 * It's like a HashMap<Parameter, ParameterValue> but stores multiple ParameterValue for the same Parameter 
 * Can be iterated like a list, but the order of insertion is not the order of iteration (maybe it should be...), 
 *  except for ParameterValue of the same Parameter which are iterated in the insertion order.
 * 
 * @author nm
 *
 */
public class ParameterValueList implements Collection<ParameterValue> {
    Entry[] table;
    int size;
    int threshold; 
    float factor = 0.75f;
    
    public ParameterValueList() {
	size = 0;
	table = new Entry[16];
	threshold = (int)(table.length*factor);
    }
    
    /**     
     * @param pvs
     */
    public ParameterValueList(Collection<ParameterValue> pvs) {
	int len = roundUpToPowerOfTwo(pvs.size());
	table = new Entry[len];
	threshold = (int)(len * factor);
	size = 0;
	for(ParameterValue pv:pvs) {
	    doAdd(pv);
	}
    }
    
    @Override
    public boolean add(ParameterValue pv) {
	if(pv==null) throw new NullPointerException();
	if(size-1 >= threshold) {
	    ensureCapacity(2*table.length);
	    threshold = 2*threshold;
	}
	doAdd(pv);
	return true;
    }
    
    private void ensureCapacity(int newCapacity) {
	Entry[] oldt = table;
	Entry[] newt = new Entry[newCapacity];
	
	
	//transfer content
	for(int i = 0; i<oldt.length; i++) {
	    Entry e = oldt[i];
	    while(e!=null) {
		int hash = getHash(e.pv.getParameter());
		int index = hash & (newt.length - 1);
		
		Entry next = e.next;
		e.next = null;
		Entry e1 = newt[index];
		if(e1 == null) {
		    newt[index] = e;
		} else {
		    while(e1.next!=null) e1=e1.next;
		    e1.next = e;
		    
		}
		e = next;
	    }
	}
	table = newt;
    }
    
    
    /**
     * add a parameter to the hashtable, to the end of the list for the same parameter
     * 
     * @param pv
     */
    private void doAdd(ParameterValue pv) {
	Entry newEntry = new Entry(pv);
	Entry[] t = table;
	
	int hash = getHash(pv.getParameter());
	int index = hash & (t.length - 1);
	if(t[index] == null) {
	    t[index] = newEntry;
	} else {
	    Entry e = t[index];
	    while(e.next!=null) e=e.next;
	    e.next = newEntry;
	}
	size++;
    }

   
    
    private int getHash(Parameter p) {
	return p.hashCode();
    }

    public int getSize() {
	return size;
    }
    
    
    /**
     * Returns the last inserted value for Parameter p or null if there is no value
     * @param p
     * @return
     */
    public ParameterValue getNewest(Parameter p) {
	int index =  getHash(p) & (table.length - 1);
	ParameterValue r = null;
	for(Entry e = table[index] ; e!=null; e=e.next) {
	    if(e.pv.getParameter()==p) {
		r = e.pv;
	    }
	}
	return r;
    }


    /**
     * this is copied from http://graphics.stanford.edu/~seander/bithacks.html#RoundUpPowerOf2
     * 
     * 
     */
    static int roundUpToPowerOfTwo(int v){
	v--;
	v |= v >> 1;
	v |= v >> 2;
	v |= v >> 4;
	v |= v >> 8;
	v |= v >> 16;
	v++;
	return v;
    }



    @Override
    public Iterator<ParameterValue> iterator() {
	return new Iter();
    }

    /**
     * Adds all element to this collection
     * @param c
     * @return
     */
    @Override
    public boolean addAll(Collection<? extends ParameterValue> c) {
	int newSize = size+c.size();
	if(newSize>threshold) {
	    int newCapacity = roundUpToPowerOfTwo(newSize);
	    ensureCapacity(newCapacity);
	    threshold = (int) (newCapacity*factor);
	}
	
	for(ParameterValue pv:c) {
	    doAdd(pv);
	}
	return false;
    }

    /**
     * Throws UnsupportedOperationException
     */
    @Override
    public void clear() {
	throw new UnsupportedOperationException();
    }

    /**
     * Return true if the list contains the exact same ParameterValue.
     * That means exactly the same object. 
     * 
     * @param o
     * @return
     */
    @Override
    public boolean contains(Object o) {
	if(!(o instanceof ParameterValue)) return false;
	
	ParameterValue pv = (ParameterValue)o;
	
	int index =  getHash(pv.getParameter()) & (table.length - 1);
	for(Entry e = table[index] ; e!=null; e=e.next) {
	    if(e.pv==pv) {
		return true;
	    }
	}
	return false;
    }


    /**
     * Throws UnsupportedOperationException
     */
    @Override
    public boolean containsAll(Collection<?> c) {
	throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
	return size == 0;
    }


    /**
     * Throws UnsupportedOperationException
     */
    @Override
    public boolean remove(Object o) {
	throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException
     */
    @Override
    public boolean removeAll(Collection<?> c) {
	throw new UnsupportedOperationException();
    }


    /**
     * Throws UnsupportedOperationException
     */
    @Override
    public boolean retainAll(Collection<?> c) {
	throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
	return size;
    }

    /**
     * Throws UnsupportedOperationException
     */
    @Override
    public Object[] toArray() {
	throw new UnsupportedOperationException();
    }


    /**
     * Throws UnsupportedOperationException
     */
    @Override
    public <T> T[] toArray(T[] a) {
	throw new UnsupportedOperationException();
    }
    

    static class Entry {
	final ParameterValue pv;
	Entry next;
	Entry(ParameterValue pv) {
	    this.pv = pv;
	}
    }

    
    private final class Iter implements Iterator<ParameterValue> {
   	Entry next;
   	int index;

   	Iter() {
   	    if(size>0) { //go to the first value
   		Entry[] t = table;
   		index=0;
   		while((index<t.length)&& (next=t[index++])==null);
   	    }
   	}

   	@Override
   	public boolean hasNext() {
   	    return next!=null;
   	}

   	@Override
   	public ParameterValue next() {
   	    if(next==null) throw new NoSuchElementException();

   	    Entry r = next;
   	    next = r.next;
   	    if(next==null) {
   		Entry[] t = table;
   		while((index<t.length)&& (next=t[index++])==null);
   	    }

   	    return r.pv;
   	}

   	@Override
   	public void remove() {
   	    throw new UnsupportedOperationException();
   	}

       }
}