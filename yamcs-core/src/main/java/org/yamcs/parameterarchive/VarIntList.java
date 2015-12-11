package org.yamcs.parameterarchive;

import java.util.Arrays;


/**
 * A list of integers stored as VarInt in an array
 *  - cannot be randomly accessed but only iterated
 * 
 * @author nm
 *
 */
public class VarIntList {
    final protected byte[] buf;
    /** Cache the hash code for the array */
    private int hash;
    
    public VarIntList(int[] a) {
        byte[] tmp = new byte[4*a.length];
        int pos=0;
        for(int i=0; i<a.length; i++) {
            int v=a[i];
            while ((v & ~0x7F) != 0) {
                tmp[pos++] = ((byte)((v & 0x7F) | 0x80));
                v >>>= 7;
            }
            tmp[pos++] = (byte)(v & 0x7F);
        }
        buf = Arrays.copyOf(tmp, pos);
    }

    protected VarIntList(byte[] buf) {
        this.buf = buf;
    }

    /**
     * 
     * @return the size of the backing array
     */
    public int arraySize() {
        return buf.length;
    }
    
    public byte[] getArray() {
        return buf;
    }
    
    
    public IntIterator iterator() {
        return new IntIterator();
    }
    
    
    public class IntIterator {
        private int pos = 0;
        boolean hasNext() {
            return pos < buf.length;
        }
        
        public int next() {
            byte b = buf[pos++];
            int v = b &0x7F;
            for (int shift = 7; (b & 0x80) != 0; shift += 7) {
                b = buf[pos++];
                v |= (b & 0x7F) << shift;
            }
            return v;
        }
        
    }


    @Override
    public int hashCode() {
        if (hash == 0 && buf.length > 0) {
            hash = Arrays.hashCode(buf);
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        
        if (obj == null) return false;
        
        if (getClass() != obj.getClass()) return false;
        
        VarIntList other = (VarIntList) obj;
        if (!Arrays.equals(buf, other.buf)) return false;
        
        
        return true;
    }
    
}
