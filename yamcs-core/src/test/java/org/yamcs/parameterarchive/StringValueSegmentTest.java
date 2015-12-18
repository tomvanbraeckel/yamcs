package org.yamcs.parameterarchive;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.utils.ValueUtility;

public class StringValueSegmentTest {
    @Test
    public void test1() throws IOException {
        StringValueSegment svs = new StringValueSegment(Arrays.asList("on", "on", "on", "off", "off", "on", "off"));
        int s = svs.getMaxSerializedSize();
        System.out.println("max size: "+s);
        ByteBuffer bb = ByteBuffer.allocate(s);
        svs.writeTo(bb);
        int length = bb.position();
        System.out.println("encoded size: "+length);
        
        StringValueSegment evs1 = new StringValueSegment();
        bb.rewind();
        evs1.parseFrom(bb);
        assertEquals(length, bb.position());
        
        assertEquals(svs.values, evs1.values);
        
    }
    
    @Ignore
    @Test
    public void test2() throws IOException {
        String on="on";
        String off="off";
        List<String> list= new ArrayList<String>();
        for(int i=0;i<1000; i+=2) {
            list.add(on);
        }

        for(int i=0;i<1000; i+=2) {
            list.add(off);
        }
        
        StringValueSegment evs = new StringValueSegment(list);
        int s = evs.getMaxSerializedSize();
        System.out.println("max size: "+s);
        ByteBuffer bb = ByteBuffer.allocate(s);
        evs.writeTo(bb);
        int length = bb.position();
        System.out.println("encoded size: "+length);
        
        StringValueSegment evs1 = new StringValueSegment();
        bb.rewind();
        evs1.parseFrom(bb);
        assertEquals(length, bb.position());
        
        assertEquals(evs.values, evs1.values);
        
    }
    
    @Test
    public void testStringVsEnum() throws IOException {
        List<Value> values= new ArrayList<Value>();
        for(int i=0;i<1000; i++) {
            values.add(ValueUtility.getStringValue("random "+i+" value"));
        }
        StringValueSegment svs = StringValueSegment.consolidate(values);
        assertTrue(svs.getClass()==StringValueSegment.class);
        int maxSize = svs.getMaxSerializedSize();
        
        ByteBuffer bb = ByteBuffer.allocate(maxSize);
        svs.writeTo(bb);
        
        List<Value> values1= new ArrayList<Value>();
        for(int i=0;i<1000; i++) {
            values1.add(ValueUtility.getStringValue("not so random "+(i%10)+" value"));
        }
        
        StringValueSegment svs1 = StringValueSegment.consolidate(values1);
        assertTrue(svs1.getClass()==EnumValueSegment.class);
        int maxSize1 = svs1.getMaxSerializedSize();
        
        ByteBuffer bb1 = ByteBuffer.allocate(maxSize1);
        svs1.writeTo(bb1);
    }    
    
}
