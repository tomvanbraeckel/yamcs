package org.yamcs.parameterarchive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.utils.DecodingException;
import org.yamcs.utils.ValueUtility;

import me.lemire.integercompression.FastPFOR128;
import me.lemire.integercompression.IntWrapper;
import me.lemire.integercompression.IntegerCODEC;
import me.lemire.integercompression.VariableByte;

public class IntValueSegmentTest {
    @Test
    public void testShortNonRandom() throws IOException, DecodingException {
        int n =3;
        List<Value> l = new ArrayList<Value>(n);
        for(int i =0; i<n; i++) {
            l.add(ValueUtility.getSint32Value(100000+i));
        }
        IntValueSegment fvs = IntValueSegment.consolidate(l, true); 
        ByteBuffer bb = ByteBuffer.allocate(fvs.getMaxSerializedSize());
        fvs.writeTo(bb);
     
        assertEquals(IntValueSegment.SUBFORMAT_ID_DELTAZG_VB, bb.get(0)&0xF);
        bb.limit(bb.position());
        
        bb.rewind();
        IntValueSegment fvs1 = IntValueSegment.parseFrom(bb);
        
        
        for(int i =0; i<n; i++) {
            assertEquals(l.get(i), fvs1.getValue(i));
        }
    }
    
    @Test
    public void testLongNonRandom() throws IOException, DecodingException {
        int n =1000;
        List<Value> l = new ArrayList<Value>(n);
        for(int i =0; i<n; i++) {
            l.add(ValueUtility.getUint32Value(100000+i));
        }
        IntValueSegment fvs = IntValueSegment.consolidate(l, false);
        ByteBuffer bb = ByteBuffer.allocate(fvs.getMaxSerializedSize());
        fvs.writeTo(bb);
        assertEquals(IntValueSegment.SUBFORMAT_ID_DELTAZG_FPF128_VB, bb.get(0)&0xF);
        
        bb.limit(bb.position());
        bb.rewind();
        IntValueSegment fvs1 = IntValueSegment.parseFrom(bb);
        
        for(int i =0; i<n; i++) {
            assertEquals(l.get(i), fvs1.getValue(i));
        }
    }
    
    @Test
    public void testRandom() throws IOException, DecodingException {
        int n = 10;
        Random rand = new Random(0);
        List<Value> l = new ArrayList<Value>(n);
        for(int i =0; i<n; i++) {
            l.add(ValueUtility.getUint32Value(rand.nextInt()));
        }
        IntValueSegment fvs = IntValueSegment.consolidate(l, false);
        ByteBuffer bb = ByteBuffer.allocate(fvs.getMaxSerializedSize());
        fvs.writeTo(bb);
        assertEquals(IntValueSegment.SUBFORMAT_ID_RAW, bb.get(0)&0xF);
        
        //assertEquals(5, bb.position());
        bb.limit(bb.position());
        
        bb.rewind();
        IntValueSegment fvs1 = IntValueSegment.parseFrom(bb);
        
        for(int i =0; i<n; i++) {
            assertEquals(l.get(i), fvs1.getValue(i));
        }
    }
    
    
    @Test
    public void testByteInteger() {
        int n = 1000;
        int[] x = new int[n];
        for(int i=0; i<n; i++) {
            x[i]=0;
        };
        VariableByte vb =  new VariableByte();
        IntWrapper inputoffset = new IntWrapper(0);
        IntWrapper outputoffset = new IntWrapper(0);
        byte[] b = new byte[4*x.length];
        
        vb.compress(x, inputoffset, x.length, b, outputoffset);
   //     System.out.println("outputoffset: "+outputoffset.get());
        int cl = outputoffset.get();
        
        inputoffset.set(0);
        outputoffset.set(0);
        int[] y = new int[x.length];
        vb.uncompress(b, inputoffset, cl, y, outputoffset);
        
        assertArrayEquals(x, y);
    }
    
    @Test
    public void testIntegerInteger() {
        int n = (1<<SortedTimeSegment.NUMBITS_MASK)/1000; 
        int[] x = new int[n];
        for(int i=0; i<n; i++) {
            x[i]= i%10;
        };
        
        //IntegerCODEC codec =  new Composition(new FastPFOR(), new VariableByte());
        IntegerCODEC fastpfor = new FastPFOR128();
        VariableByte vb = new VariableByte();
        
        IntWrapper inputoffset = new IntWrapper(0);
        IntWrapper outputoffset = new IntWrapper(0);
        int[] xc = new int[x.length];
        
        fastpfor.compress(x, inputoffset, x.length, xc, outputoffset);
  //      System.out.println("input/output offsets after fastpfor: "+inputoffset.get()+" / "+outputoffset.get());
       
        
        int remaining =  x.length - inputoffset.get();
        vb.compress(x, inputoffset, remaining, xc, outputoffset);
//        System.out.println("input/output offsets after vb: "+inputoffset.get()+" / "+outputoffset.get());
        int cl = outputoffset.get();
        
        inputoffset.set(0);
        outputoffset.set(0);
        int[] y = new int[x.length];
        
        fastpfor.uncompress(xc, inputoffset, cl, y, outputoffset);
        vb.uncompress(xc, inputoffset, cl-inputoffset.get(), y, outputoffset);
        
     
        assertArrayEquals(x, y);
    }
    
}
