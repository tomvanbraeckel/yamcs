package org.yamcs.parameterarchive;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.Test;
import org.yamcs.utils.ValueUtility;

public class Uint32ValueSegmentTest {
    @Test
    public void test() throws IOException {
        UInt32ValueSegment fvs = UInt32ValueSegment.consolidate(Arrays.asList(ValueUtility.getUint32Value(1), ValueUtility.getUint32Value(2), ValueUtility.getUint32Value(3)));
        assertEquals(16, fvs.getMaxSerializedSize());
        
        ByteBuffer bb = ByteBuffer.allocate(28);
        fvs.writeTo(bb);
        
        bb.rewind();
        UInt32ValueSegment fvs1 = new UInt32ValueSegment();
        fvs1.parseFrom(bb);
        
        assertEquals(ValueUtility.getUint32Value(1), fvs1.get(0));
        assertEquals(ValueUtility.getUint32Value(2), fvs1.get(1));
        assertEquals(ValueUtility.getUint32Value(3), fvs1.get(2));
    }
}
