package org.yamcs.utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class VarIntUtil {
    /**
     * Encodes x as varint in the buffer at position pos and returns the new position
     * 
     * @param buf
     * @param x
     * @return
     */
    static public int writeVarint32(byte[] buf, int pos, int x) {
        while ((x & ~0x7F) != 0) {
            buf[pos++] = ((byte)((x & 0x7F) | 0x80));
            x >>>= 7;
        }
        buf[pos++] = (byte)(x & 0x7F);
        
        return pos;
    }
    
    public static void writeVarInt32(ByteBuffer bb, int x) {
        while ((x & ~0x7F) != 0) {
            bb.put((byte)((x & 0x7F) | 0x80));
            x >>>= 7;
        }
        bb.put((byte)(x & 0x7F));
    }
    
    public static void writeVarInt64(ByteBuffer bb, long x) {
        while ((x & ~0x7F) != 0) {
            bb.put((byte)((x & 0x7F) | 0x80));
            x >>>= 7;
        }
        bb.put((byte)(x & 0x7F));
    }
    
    public static int readVarInt32(ByteBuffer bb) {
        byte b = bb.get();
        int v = b &0x7F;
        for (int shift = 7; (b & 0x80) != 0; shift += 7) {
            b = bb.get();
            v |= (b & 0x7F) << shift;
        }
        return v;
    }
    
    public static long readVarInt64(ByteBuffer bb) {
        byte b = bb.get();
        long v = b &0x7F;
        for (int shift = 7; (b & 0x80) != 0; shift += 7) {
            b = bb.get();
            v |= (b & 0x7F) << shift;
        }
        return v;
    }

    
    public static void writeSignedVarint32(ByteBuffer bb, int x) {
        writeVarInt32(bb, encodeZigZag(x));
    }
    
    public static int readSignedVarInt32(ByteBuffer bb) {
        return decodeZigZag(readVarInt32(bb));
    }
    
    //same as above but better for negative numbers
    static public int encodeSigned(byte[] buf, int pos, int x) {
    	return writeVarint32(buf, pos, encodeZigZag(x));       
    }
    
    /**
     * decodes an array of varints
     * 
     * @author nm
     *
     */
    public static class ArrayDecoder {
        int pos=0; 
        final byte[] buf;
        private ArrayDecoder(byte[] buf) {
            this.buf = buf;
        }
        
        /**
         * Returns true if the array contains another element. 
         *  If the array is corrupted, this will return true and next() will throw an BufferOverflow exception
         * @return
         */
        public boolean hasNext() {
            return pos < buf.length;
        }
        
        public int next() {
            byte b = buf[pos++];
            int v = b &0x7F;
            for (int shift = 7; (b & 0x80) != 0; shift += 7) {
                b = buf[pos++];
                v |= (b & 0x7F) << shift;
            }
            return v;
        }
    }
    
    
    
    public static class SignedArrayDecoder extends ArrayDecoder{
         private SignedArrayDecoder(byte[] buf) {
             super(buf);
         }
         
         public int next() {
             return decodeZigZag(super.next());
         }
    }
    
    static public ArrayDecoder newArrayDecoder(byte[] buf) {
        return new ArrayDecoder(buf);
    }
    
    
    
    //used to transform small signed integers into unsigned (see protobuf docs)
    public static int decodeZigZag(int x) {
        return (x >>> 1) ^ -(x & 1);
    }
    
    public static int encodeZigZag(int x) {
        return (x << 1) ^ (x >> 31);
    }

    public static void writeSizeDelimitedString(ByteBuffer bb, String s) {
        byte[]b = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt32(bb,  b.length);
        bb.put(b);
    }

    public static String readSizeDelimitedString(ByteBuffer bb) {
        int l = readVarInt32(bb);
        byte[] b = new byte[l];
        bb.get(b);
        return new String(b, StandardCharsets.UTF_8);
    }

   
   

  

  
  
}
