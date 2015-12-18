package org.yamcs.parameterarchive;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.Test;
import org.yamcs.utils.ValueUtility;

public class BooleanValueSegmentTest {

    @Test
    public void test() throws IOException {
        BooleanValueSegment bvs = BooleanValueSegment.consolidate(Arrays.asList(ValueUtility.getBooleanValue(true), ValueUtility.getBooleanValue(true), ValueUtility.getBooleanValue(false)));
        assertEquals(12, bvs.getMaxSerializedSize());
        
        ByteBuffer bb = ByteBuffer.allocate(12);
        bvs.writeTo(bb);
        
        bb.rewind();
        BooleanValueSegment bvs1 = new BooleanValueSegment();
        bvs1.parseFrom(bb);
        
        assertEquals(ValueUtility.getBooleanValue(true), bvs1.get(0));
        assertEquals(ValueUtility.getBooleanValue(true), bvs1.get(1));
        assertEquals(ValueUtility.getBooleanValue(false), bvs1.get(2));
    }

}
