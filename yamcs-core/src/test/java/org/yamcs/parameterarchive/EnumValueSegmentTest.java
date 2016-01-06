package org.yamcs.parameterarchive;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

public class EnumValueSegmentTest {
    @Test
    public void test1() throws DecodingException {
        EnumValueSegment evs = new EnumValueSegment(Arrays.asList("on", "on", "on", "off", "off", "on", "off"));
        int s = evs.getMaxSerializedSize();
        System.out.println("max size: "+s);
        ByteBuffer bb = ByteBuffer.allocate(s);
        evs.writeTo(bb);
        int length = bb.position();
        System.out.println("encoded size: "+length);
        
        EnumValueSegment evs1 = new EnumValueSegment();
        bb.rewind();
        evs1.parseFrom(bb);
        assertEquals(length, bb.position());
        
        assertEquals(evs.values, evs1.values);
        
    }
    
    
    @Ignore
    @Test
    public void test2() throws DecodingException {
        String on="on";
        String off="off";
        List<String> list= new ArrayList<String>();
        for(int i=0;i<1000; i+=2) {
            list.add(on);
        }

        for(int i=0;i<1000; i+=2) {
            list.add(off);
        }
        
        EnumValueSegment evs = new EnumValueSegment(list);
        int s = evs.getMaxSerializedSize();
        System.out.println("max size: "+s);
        ByteBuffer bb = ByteBuffer.allocate(s);
        evs.writeTo(bb);
        int length = bb.position();
        System.out.println("encoded size: "+length);
        
        EnumValueSegment evs1 = new EnumValueSegment();
        bb.rewind();
        evs1.parseFrom(bb);
        assertEquals(length, bb.position());
        
        assertEquals(evs.values, evs1.values);
        
    }
}
