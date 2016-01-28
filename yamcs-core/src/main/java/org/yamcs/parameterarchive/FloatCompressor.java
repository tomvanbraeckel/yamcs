package org.yamcs.parameterarchive;

/**
 * Implements the floating point compression scheme described here:
 * http://www.vldb.org/pvldb/vol8/p1816-teller.pdf
 * 
 * @author nm
 *
 */
public class FloatCompressor {
   
    byte[] compress(float[] fa) {
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
        return bb.toByteArray();
    }

    float[] decompress(byte[] b, int n) {
        BitBuffer bb = new BitBuffer(b);
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
}
