package org.yamcs.parameterarchive;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.ByteBuffer;
import java.util.Random;

import me.lemire.integercompression.Composition;
import me.lemire.integercompression.FastPFOR;
import me.lemire.integercompression.IntWrapper;
import me.lemire.integercompression.IntegerCODEC;
import me.lemire.integercompression.VariableByte;

import org.junit.Test;
import org.yamcs.utils.FloatArray;
import org.yamcs.utils.VarIntUtil;

import static org.junit.Assert.*;

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
            VarIntUtil.writeVarInt64(bb, xor);
        }

        System.out.println("bb.size: "+bb.position()+" percentage: "+100*bb.position()/bb.capacity() +"%");
    }

    float[] readFile(String file) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        br.readLine();//skip first line
        FloatArray farray = new FloatArray();
        int c=0;
        while((line=br.readLine())!=null) {
            String[] a = line.split("\\s+");
            float f = Float.parseFloat(a[1]);
            farray.add(f);
            //if(c++>5) break;
        }
        br.close();
        return farray.toArray();
    }

    BitBuffer compress(float[] fa) {
        BitBuffer bb=new BitBuffer(fa.length/2);

        int xor;
        int prevV = Float.floatToRawIntBits(fa[0]);
        bb.write(prevV, 32);
        
        int prevLz = 100; //such that the first comparison lz>=prevLz will fail
        int prevTz = 0;

        for(int i=1; i<fa.length; i++) {
            int v = Float.floatToRawIntBits(fa[i]);
            xor = v^prevV;

            //If XOR with the previous is zero (same value), store single ‘0’ bit
            if(xor==0) {
                bb.write(0, 1);
            } else {

                //When XOR is non-zero, calculate the number of leading and trailing zeros in the XOR, store bit ‘1’ followed
                // by either a) or b):
                bb.write(1, 1);
                int lz = Integer.numberOfLeadingZeros(xor);
                int tz = Integer.numberOfTrailingZeros(xor);
                
//           /     System.out.println("ei:"+i+" lz: "+lz+" tz: "+tz+" prevLz: "+prevLz+" prevTz: "+prevTz);
                if((lz>=prevLz)&&(tz>=prevTz)) {
                    //(a) (Control bit ‘0’) If the block of meaningful bits falls within the block of previous meaningful bits,
                    //i.e., there are at least as many leading zeros and as many trailing zeros as with the previous value,
                    //use that information for the block position and just store the meaningful XORed value.
                    bb.write(0, 1);
                    bb.write(xor>>prevTz, 32-prevLz-prevTz);
                } else {
                    //(b) (Control bit ‘1’) Store the length of the number  of leading zeros in the next 5 bits, then store the
                    // length of the meaningful XORed value in the next 6 bits. Finally store the meaningful bits of the XORed value.
                    int mb = 32-lz-tz; //meaningful bits
                    bb.write(1, 1);
                    bb.write(lz, 5);
                    bb.write(mb, 5);
                    bb.write(xor>>tz, mb);
                    prevLz = lz;
                    prevTz = tz;
                }

            }
            prevV = v;
        }
        return bb;
    }

    float[] decompress(BitBuffer bb, int n) {
        bb.rewind();
        float[] fa = new float[n];
        int xor;
        int v = (int)bb.read(32);
        System.out.println("v: "+v);
        fa[0] = Float.intBitsToFloat(v);
        
        int lz = 0; //leading zeros
        int tz = 0; //trailing zeros
        int mb = 0; //meaningful bits
        for(int i=1; i<fa.length; i++) {
            int bit = bb.read(1);
            if(bit==0) {
                //same with the previous value
                fa[i]=fa[i-1];
            } else {
                bit = bb.read(1);
                if(bit==0) {//the block of meaningful bits falls within the block of previous meaningful bits,
                    xor = bb.read(mb)<<tz;
                    v = xor^v;
                } else {
                    lz = bb.read(5);
                    mb = bb.read(5);
                    tz = 32-lz-mb;
                    xor = bb.read(mb)<<tz;
                    v = xor^v;
                }
                fa[i] = Float.intBitsToFloat(v);
              //  System.out.println("di:"+i+" mb: "+mb+" lz: "+lz+" tz: "+tz+" bit: "+bit);
            }
        }
        
        return fa;
    }
    
    
    public void testFloatCompression(String file) throws Exception {
        float[] fa = readFile(file);
        BitBuffer bb = compress(fa);
        int us = fa.length*4;
        int cs = 8*bb.getSize();
        System.out.println("result for "+file+" size: "+cs+" fa.size: "+us+" ratio: "+(100*cs/us)+"% bitsPerValue: "+8*cs/(float)fa.length);
        
        float[] fa1=decompress(bb, fa.length);
        assertArrayEquals(fa, fa1, 1e-10f);
    }

    public void testFloatToInteger(String file) throws Exception {
        float[] fa = readFile(file);
        int n = fa.length;
        
        int[] ia = new int[n];
        for(int i=0;i<n;i++) {
            ia[i]= Float.floatToRawIntBits(fa[i]);
        }

        int[] zia = VarIntUtil.encodeDeltaDeltaZigZag(ia);
        int[] compressed = new int [zia.length];
        IntegerCODEC codec =  new Composition(new FastPFOR(), new VariableByte());
        // compressing
        IntWrapper inputoffset = new IntWrapper(0);
        IntWrapper outputoffset = new IntWrapper(0);
        codec.compress(zia,inputoffset, zia.length, compressed,outputoffset);
        System.out.println("compressed unsorted integers from "+zia.length*4+"B to "+outputoffset.intValue()*4+"B");

     //   System.out.println("result for "+file+" size: "+cs+" fa.size: "+us+" ratio: "+(100*cs/us)+"% bitsPerValue: "+8*cs/(float)fa.length);
    }

    @Test
    public void testSOLAR_PB1_5V_VME_Outlet_Current() throws Exception {
      //  testFloatToInteger("/storage/solar-tm/SOLAR_PB1_5V_VME_Outlet_Current.txt");
        //testFloatCompression("/storage/solar-tm/SOLAR_PB1_5V_VME_Outlet_Current.txt");
        testFloatCompression("/storage/solar-tm/SOLAR_PB1_12V_Service_Outlet_Voltage.txt");
    }

    @Test
    public void testSOLAR_PB1_12V_Service_Outlet_Voltage() throws Exception {

    }


}
