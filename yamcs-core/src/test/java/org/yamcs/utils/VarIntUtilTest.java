package org.yamcs.utils;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;


import org.junit.Test;

public class VarIntUtilTest {
    
    @Test
    public void testVarInt32() throws Exception{
        ByteBuffer bb = ByteBuffer.allocate(10);
        VarIntUtil.writeVarInt32(bb, 3);
        assertEquals(1, bb.position());
        
        bb.rewind();
        assertEquals(3, VarIntUtil.readVarInt32(bb));
        
        bb.rewind();
        VarIntUtil.writeVarInt32(bb, 200);
        assertEquals(2, bb.position());
        bb.rewind();
        assertEquals(200, VarIntUtil.readVarInt32(bb));
        
        bb.rewind();
        VarIntUtil.writeVarInt32(bb, 0xFFFF);
        assertEquals(3, bb.position());
        bb.rewind();
        assertEquals(0xFFFF, VarIntUtil.readVarInt32(bb));
        
        bb.rewind();
        VarIntUtil.writeVarInt32(bb, 0xFFFFFFFF);
        assertEquals(5, bb.position());
        bb.rewind();
        assertEquals(0xFFFFFFFF, VarIntUtil.readVarInt32(bb));
    }
    
    @Test
    public void testInvalid() {
        ByteBuffer bb = ByteBuffer.wrap(StringConvertors.hexStringToArray("8182838485"));
        try {
            VarIntUtil.readVarInt32(bb);
            fail("Should have thrown an exception");
        } catch (DecodingException e) {
        }
        
    }
}
