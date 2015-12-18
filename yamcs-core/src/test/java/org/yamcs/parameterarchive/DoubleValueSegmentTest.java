package org.yamcs.parameterarchive;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.Test;
import org.yamcs.utils.ValueUtility;

public class DoubleValueSegmentTest {
    @Test
    public void test() throws IOException {
        DoubleValueSegment fvs = DoubleValueSegment.consolidate(Arrays.asList(ValueUtility.getDoubleValue(1.2), ValueUtility.getDoubleValue(2.3), ValueUtility.getDoubleValue(3)));
        assertEquals(28, fvs.getMaxSerializedSize());
        
        ByteBuffer bb = ByteBuffer.allocate(28);
        fvs.writeTo(bb);
        
        bb.rewind();
        DoubleValueSegment fvs1 = new DoubleValueSegment();
        fvs1.parseFrom(bb);
        
        assertEquals(ValueUtility.getDoubleValue(1.2), fvs1.get(0));
        assertEquals(ValueUtility.getDoubleValue(2.3), fvs1.get(1));
        assertEquals(ValueUtility.getDoubleValue(3), fvs1.get(2));
    }
}
