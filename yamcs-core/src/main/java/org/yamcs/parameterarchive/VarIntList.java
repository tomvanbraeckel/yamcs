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
    byte[] buf;
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

    /**
     * 
     * @return the size of the backing array
     */
    public int arraySize() {
        return buf.length;
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
  /*
    public final void write(int pos, int v) {
        while ((v & ~0x7F) != 0) {
            buf[pos++] = ((byte)((v & 0x7F) | 0x80));
            v >>>= 7;
        }
        buf[pos++] = (byte) (v|0x80);
    }
*/

    
}
