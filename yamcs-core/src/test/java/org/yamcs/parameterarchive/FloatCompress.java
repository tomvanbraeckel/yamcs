package org.yamcs.parameterarchive;

import java.nio.ByteBuffer;
import java.util.Random;

import org.junit.Test;
import org.yamcs.utils.VarIntUtil;

public class FloatCompress {

    @Test
    public void testFloat() {
        float f  = 200;
        Random rand = new Random();
        int n = 1000;
        ByteBuffer bb = ByteBuffer.allocate(4*n);
        
        for(int i =0;i<1000; i++) {
            float g = f + 10*rand.nextFloat();
            int xor = Float.floatToIntBits(f)^Float.floatToIntBits(g);
            VarIntUtil.writeVarInt32(bb, xor);
        }
        
        System.out.println("bb.size: "+bb.position()+" percentage: "+100*bb.position()/bb.capacity() +"%");
    }
    
    @Test
    public void testDouble() {
        double f  = 2;
        System.out.println("Float.floatToIntBits(f): "+String.format("%x",Double.doubleToLongBits((float)1.2)));
        Random rand = new Random();
        int n = 1000;
        ByteBuffer bb = ByteBuffer.allocate(8*n);
        
        for(int i =0;i<1000; i++) {
         //   double g = f + 10*rand.nextFloat();
            double g = f+10*rand.nextDouble();
            long xor = Double.doubleToLongBits(f)^Double.doubleToLongBits(g);
            VarIntUtil.writeVarint64(bb, xor);
        }
        
        System.out.println("bb.size: "+bb.position()+" percentage: "+100*bb.position()/bb.capacity() +"%");
    }
}
