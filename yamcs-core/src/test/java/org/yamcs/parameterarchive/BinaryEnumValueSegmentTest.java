package org.yamcs.parameterarchive;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.yamcs.utils.DecodingException;

public class BinaryEnumValueSegmentTest {
    @Test
    public void test1() throws DecodingException {
        BinaryEnumValueSegment evs = new BinaryEnumValueSegment(Arrays.asList("on".getBytes(), "on".getBytes(), "on".getBytes(), "off".getBytes(), "off".getBytes(), "on".getBytes(), "off".getBytes()));
        int s = evs.getMaxSerializedSize();
        ByteBuffer bb = ByteBuffer.allocate(s);
        evs.writeTo(bb);
        int length = bb.position();
        
        BinaryEnumValueSegment evs1 = new BinaryEnumValueSegment();
        bb.rewind();
        evs1.parseFrom(bb);
        assertEquals(length, bb.position());
        
        asserBinaryArraysEquals(evs.values, evs1.values);
        
        asserBinaryArraysEquals(new byte[][]{"on".getBytes(), "on".getBytes(), "on".getBytes(), "off".getBytes()}, evs.getRange(0, 4, true));
        asserBinaryArraysEquals(new byte[][]{"on".getBytes(), "off".getBytes()}, evs1.getRange(3, 5, false));
    }
    

    private void asserBinaryArraysEquals(byte[][] expected, byte[][] actual) {
        assertEquals(expected.length, actual.length);
        for(int i = 0; i<expected.length; i++) {
            byte[] e = expected[i];
            byte[] a = actual[i];
            assertArrayEquals(e, a);
        }
        
    }
    
    private void asserBinaryArraysEquals(List<byte[]> expected, List<byte[]> actual) {
        assertEquals(expected.size(), actual.size());
        for(int i = 0; i<expected.size(); i++) {
            byte[] e = expected.get(i);
            byte[] a = actual.get(i);
            assertArrayEquals(e, a);
        }
    }
}
