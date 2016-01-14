package org.yamcs.parameter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.yamcs.ParameterValue;
import org.yamcs.xtce.Parameter;

/**
 * 
 * 
 * Used by the {@link org.yamcs.parameter.ParameterRequestManagerImpl} to cache last values of parameters.
 * 
 * The cache will contain the parameters for a predefined time period but can exceed that period if space is available in the CacheEntry.
 * 
 * A CacheEntry also has a maximum size to prevent it accumulating parameters ad infinitum (e.g. if there are bogus parameters with the timestamp never changing)
 * 
 * 
 * We keep delivery consisting of lists of parameter values together such that
 *  if two parameters have been acquired in the same delivery, they will be given from the same delivery to the clients.
 * 
 * @author nm
 *
 */
public class ParameterCache {
    final ConcurrentHashMap<Parameter, CacheEntry> cache = new ConcurrentHashMap<Parameter, CacheEntry>();
    final long timeToCache;
    public ParameterCache(long timeToCache) {
        this.timeToCache = timeToCache;
    }
    /**
     * update the parameters in the cache

     * @param parameterList
     */
    public void update(Collection<ParameterValue> pvs) {
        //System.out.println("ParameterCache ------- updated with "+pvs);
        ParameterValueList  pvlist = new ParameterValueList(pvs);
        for (ParameterValue pv:pvs) {
            Parameter p = pv.getParameter();
            CacheEntry ce = cache.get(p);
            if(ce==null) {
                ce = new CacheEntry(p, timeToCache);
                cache.put(p, ce);
            }
            ce.add(pvlist);
        }
    }


    /**
     * Returns cached value for parameter or an empty list if there is no value in the cache
     * 
     * 
     * @param plist
     * @return
     */
    List<ParameterValue> getValues(List<Parameter> plist) {
        //use a bitset to clear out the parameters that have already been found
        BitSet bs = new BitSet(plist.size());
        List<ParameterValue> result = new ArrayList<ParameterValue>(plist.size());
        bs.set(0, plist.size(), true);

        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) {
            Parameter p = plist.get(i);
            CacheEntry ce = cache.get(p);
            if(ce!=null) { //last delivery where this parameter appears
                ParameterValueList pvlist = ce.getLast();
                ParameterValue pv = pvlist.getLastInserted(p);
                result.add(pv);
                bs.clear(i);
                //find all the other parameters that are in this delivery
                for (int j = bs.nextSetBit(i); j >= 0; j = bs.nextSetBit(j+1)) {
                    Parameter p1 = plist.get(j);
                    pv = pvlist.getLastInserted(p1);
                    if(pv!=null) {
                        result.add(pv);
                        bs.clear(j);
                    }
                }
            } else { //no value for this parameter
                bs.clear(i);
            }
        }

        return result;
    }


    /**
     * Returns last cached value for parameter or null if there is no value in the cache
     * @param plist
     * @return
     */
    ParameterValue getLastValue(Parameter p) {
        CacheEntry ce = cache.get(p);
        if(ce==null) return null;
        ParameterValueList pvlist = ce.getLast();
        return pvlist.getLastInserted(p);
    }

    /**
     * Stores a cache for one parameter as an array of the ParameterValueList in which it is part of.
     * 
     * It ensure minimum 
     * 
     * @author nm
     *
     */
    static final class CacheEntry {
        final Parameter parameter;
        private volatile ParameterValueList[] elements;
        volatile int tail = 0;
        static final int INITIAL_CAPACITY = 128;
        static final int MAX_CAPACITY = 4096;
        final long timeToCache;
        
        public CacheEntry(Parameter p, long timeToCache) {
            this.parameter = p;
            this.timeToCache = timeToCache;
            elements = new ParameterValueList[INITIAL_CAPACITY];
        }

       

        ParameterValueList getLast() {
            return elements[(tail-1)&(elements.length-1)];
        }
        
        public synchronized void add(ParameterValueList pvlist) {
            ParameterValueList pv1 = elements[tail];
            if(pv1!=null) {
                ParameterValue oldpv = pv1.getFirstInserted(parameter);
                ParameterValue newpv = pvlist.getFirstInserted(parameter);
                if((oldpv==null) || (newpv==null)) return; // shouldn't happen
                
                if(newpv.getAcquisitionTime()-oldpv.getAcquisitionTime()<timeToCache) {
                    doubleCapacity();
                }
            }
            elements[tail] = pvlist;
            tail = (tail+1) & (elements.length-1);
        }
        
        private void doubleCapacity() {
            int capacity = elements.length;
            if(capacity>=MAX_CAPACITY) return;

            int newCapacity = 2*capacity;

            ParameterValueList[]  newElements = new ParameterValueList[newCapacity];
            System.arraycopy(elements, 0, newElements, 0, tail);
            System.arraycopy(elements, tail, newElements, tail+capacity, capacity-tail);
            elements = newElements;
        }
    }
}
