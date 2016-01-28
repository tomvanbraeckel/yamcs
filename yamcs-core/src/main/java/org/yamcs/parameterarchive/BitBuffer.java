package org.yamcs.parameterarchive;

/**
 * BitBuffer allows to write individual bits or group of bits into a buffer backed by an long[] array.
 * 
 * All the writings/readings are performed to/from a temporary field which is stored/retrieve from the backing array when it is "full"
 *  
 * @author nm
 *
 */
public class BitBuffer {
    
    private int bitShift; //bit offset from the right inside the current int
    private long[] a;
    
    //we put all the bits in the b, when it is full we save it in the array a and increase the offset
    private long b; //current element
    private int offset; //the offset of the current element in the array
    
    /**
     * Constructs a buffer of size 8*n backed by an long[n] array
     *
     * @param n
     */
    BitBuffer(int n) {
        a = new long[n];
        offset = 0;
        bitShift = 64;
        b = 0;
    }
  
    /**
     * write the least significant numBits of x into the BitBuffer
     * 
     * Note that there is no check that the bits will actually fit into the allocated buffer, they will be stored in the temporary field. 
     *  A buffer overflow exception will happen when the temporary field is full and flushed to the array (so the buffer is exceeded by 64 bits) 
     * 
     * @param x
     * @param numBits
     */
    public void write(int x, int numBits) {
        int k = numBits-bitShift;
        if(k<0) {
            doWrite(x, numBits);
        } else {
            doWrite(x>>k, bitShift);
            bitShift = 64;
            a[offset] = b;
            b = 0;
            offset++;
            doWrite(x, k);
        } 
    }
    
    //here we know that numBits<bitShift
    private void doWrite(long x, int numBits) {     
        bitShift-=numBits;
        long mask = (1L<<numBits) -1;
        b |= ((x&mask) << bitShift);
    }
    
    public long read(int numBits) {
        int k = numBits-bitShift;
        if(k<0) {
            return doRead(numBits);
        } else {
            long x= doRead(bitShift)<<k;
            bitShift = 64;
            offset++;
            b = a[offset];
            return x|doRead(k);
        }
    }
    
    private long doRead(int numBits) {
        bitShift-=numBits;
        long mask = (1L<<numBits) -1;
        return (b>>bitShift)&mask;
    }

    /**
     * flush the temporary field to the array and return the backing array 
     * @return
     */
    public long[] getArray() {
        if(offset<a.length) {
            a[offset] = b;
        }
        return a;
    }
    
    /**
     * get the size of the backing array containing data
     * 
     * */
    public int getSize() {
        int r = offset;
        if(bitShift<64) r++;
        
        return r;
    }

    public void rewind() {
        if(offset<a.length) {
            a[offset] = b;
        }
        offset = 0;
        bitShift = 64;
        b = a[0];
    }
}
