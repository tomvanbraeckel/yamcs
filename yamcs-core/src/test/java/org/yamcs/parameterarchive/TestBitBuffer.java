package org.yamcs.parameterarchive;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestBitBuffer {
    @Test
    public void tesSingleBit1() {
        BitBuffer bitBuffer =new BitBuffer(3);
        for(int i=0; i<128; i++) {
            bitBuffer.write(i, 1);
        }
        long[] x = bitBuffer.getArray();
        assertArrayEquals(new long[]{0x5555555555555555L, 0x5555555555555555L, 0x0}, x);
        
        bitBuffer.rewind();
        for(int i=0; i<128; i++) {
            assertEquals(i&1, bitBuffer.read(1));
        }
    }
    
    
    @Test
    public void tesVariableBits() {
        BitBuffer bitBuffer =new BitBuffer(4);
        
        for(int i=0; i<50; i++) {
            bitBuffer.write(1, 2);
            bitBuffer.write(3, 3);
        }
        bitBuffer.rewind();
        long[] x = bitBuffer.getArray();
        assertEquals(0x5ad6b5ad6b5ad6b5L, x[0]);
        for(int i=0; i<50; i++) {
            assertEquals(1, bitBuffer.read(2));
            assertEquals(3, bitBuffer.read(3));
        }
    }
    
    @Test
    public void tesV32Bits() {
        BitBuffer bitBuffer =new BitBuffer(4);
        bitBuffer.write(0x01020304, 32);
        bitBuffer.rewind();
        
        assertEquals(0x01020304, bitBuffer.read(32));
    }
}
