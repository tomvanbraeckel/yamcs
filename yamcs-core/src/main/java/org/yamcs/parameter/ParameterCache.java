package org.yamcs.parameter;
import java.util.ArrayList;
import java.util.Arrays;
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
 * The cache will contain the parameters for a predefined 
 * 
 * 
 * We keep delivery consisting of lists of parameter values together such that
 *  if two parameters have been acquired in the same delivery, they will be given from the same delivery to the clients.
 * 
 * @author nm
 *
 */
public class ParameterCache {
    ConcurrentHashMap<Parameter, CacheEntry> cache = new ConcurrentHashMap<Parameter, CacheEntry>();

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
                ce = new CacheEntry();
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
                    pv = pvlist.getLastInserted(p);
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
     * Returns cached value for parameter or null if there is no value in the cache
     * @param plist
     * @return
     */
    ParameterValue getValue(Parameter p) {
        CacheEntry ce = cache.get(p);
        if(ce==null) return null;
        ParameterValueList pvlist = ce.getLast();
        return pvlist.getLastInserted(p);
    }

    static final class CacheEntry {
        volatile ParameterValueList[] pvlistArray;
        volatile int head;
        static int DEFAULT_CAPACITY = 100;

        public CacheEntry() {
            pvlistArray = new ParameterValueList[DEFAULT_CAPACITY];
        }

       

        ParameterValueList getLast() {
            return pvlistArray[head];
        }
        
        public synchronized void add(ParameterValueList pvlist) {
            
        }
        
        private void ensureCapacity(int minCapacity) {
            if(minCapacity<=pvlistArray.length) return;

            int capacity = pvlistArray.length;
            int newCapacity = capacity + (capacity >> 1);
            if(newCapacity<minCapacity) newCapacity = minCapacity;

            pvlistArray = Arrays.copyOf(pvlistArray, newCapacity);
        }
    }
}
