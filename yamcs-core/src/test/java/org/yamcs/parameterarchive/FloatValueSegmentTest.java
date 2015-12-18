package org.yamcs.parameterarchive;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.Test;
import org.yamcs.utils.ValueUtility;

public class FloatValueSegmentTest {
    @Test
    public void test() throws IOException {
        FloatValueSegment fvs = FloatValueSegment.consolidate(Arrays.asList(ValueUtility.getFloatValue((float)1.2), ValueUtility.getFloatValue((float)2.3), ValueUtility.getFloatValue((float) 3)));
        assertEquals(16, fvs.getMaxSerializedSize());
        
        ByteBuffer bb = ByteBuffer.allocate(16);
        fvs.writeTo(bb);
        
        bb.rewind();
        FloatValueSegment fvs1 = new FloatValueSegment();
        fvs1.parseFrom(bb);
        
        assertEquals(ValueUtility.getFloatValue((float)1.2), fvs1.get(0));
        assertEquals(ValueUtility.getFloatValue((float)2.3), fvs1.get(1));
        assertEquals(ValueUtility.getFloatValue((float)3), fvs1.get(2));
    }
}
