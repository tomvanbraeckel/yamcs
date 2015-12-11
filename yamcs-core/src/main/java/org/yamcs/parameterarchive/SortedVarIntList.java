package org.yamcs.parameterarchive;


/**
 * Stores a sorted list of integers as a varlist of
 * 
 * a0, a1-a0, a2-a1,...
 * 
 * 
 * @author nm
 *
 */
public class SortedVarIntList extends VarIntList {

    public SortedVarIntList(int[] a) {
        super(transform(a));
    }

    protected SortedVarIntList(byte[] buf) {
        super(buf);
    }

    /**
     * Constructor from the buffer
     * 
     * @param buf
     */
    public static SortedVarIntList fromBuffer(byte[] buf) {
        return new SortedVarIntList(buf);
    }

    //transform into deltas (also check that is sorted)
    private static int[] transform(int[]a) {
        if(a.length==0) return a;
        
        int[] b = new int[a.length];
        b[0]=a[0];
        for(int i=1; i<a.length; i++) {
            b[i]=a[i]-a[i-1];
            if(b[i] <0) throw new IllegalArgumentException("Array not sorted:  a["+i+"]="+a[i]+" is smaller than a["+(i-1)+"]="+a[i-1]);
        }
        return b;
    }
    
    
    public IntIterator iterator() {
        return new IntIterator();
    }
    
    public class IntIterator extends VarIntList.IntIterator {
        private int last = 0;
        boolean hasNext() {
            return super.hasNext();
        }
        
        public int next() {
            last += super.next();
            return last;
        }        
    }

    
}
