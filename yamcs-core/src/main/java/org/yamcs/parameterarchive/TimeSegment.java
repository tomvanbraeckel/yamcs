package org.yamcs.parameterarchive;

import java.io.IOException;
import java.util.Arrays;

import org.yamcs.utils.SortedIntArray;
import org.yamcs.utils.VarIntUtil;

/**
 * TimeSegment stores timestamps relative to a t0. 
 * The timestamps are stored in a sorted int array.
 * 
 * @author nm
 *
 */
public class TimeSegment {
    public static final int NUMBITS_MASK=22; //2^22 millisecons ~= 69 minutes per segment    
    public static final int TIMESTAMP_MASK = (0xFFFFFFFF>>>(32-NUMBITS_MASK));
    public static final int VERSION = 0;
    
    final private long t0;    
    private SortedIntArray tsarray;
    
    
    public TimeSegment(long t0) {
        if((t0 & TIMESTAMP_MASK) !=0) throw new IllegalArgumentException("t0 must be 0 in last "+NUMBITS_MASK+" bits");
        
        tsarray = new SortedIntArray();
        this.t0 = t0;
    }
    
    /**
     * Insert instant into the array and return the position at which it has been inserted.
     * 
     * @param instant
     */
    public int add(long instant) {
        return tsarray.add((int)(instant & TIMESTAMP_MASK));
    }

    /**
     * Encode the time array as a varint list composed of:
     *  version
     *  array size
     *  ts0
     *  ts1-ts0
     *  ts2-ts1
     *  ...
     * @return
     */
    public byte[] encode() {
        if(tsarray.size()==0) throw new IllegalStateException(" the time segment has no data");
        byte[] buf = new byte[4*(tsarray.size())+3];
        int pos = VarIntUtil.encode(buf, 0, tsarray.size());
        int x=tsarray.get(0);
        pos = VarIntUtil.encode(buf, pos, x);
        for(int i=1; i<tsarray.size(); i++) {
            int y = tsarray.get(i);
            pos = VarIntUtil.encode(buf, pos, (y-x));
            x = y;
        }
        if(pos < buf.length) {
            buf = Arrays.copyOf(buf, pos);
        }
        return buf;
    }
    
    /**
     * Creates a TimeSegment by decoding the buffer 
     * this is the reverse of the {@link #encode()} operation
     * 
     * @param buf
     * @return
     */
    static public TimeSegment decode(long t0, byte[] buf) throws IOException {
        VarIntUtil.ArrayDecoder ad = VarIntUtil.newArrayDecoder(buf);
        
        if(!ad.hasNext()) throw new IOException("Cannot decode time array version");
        int version = ad.next();
        if(version!=VERSION) {
            throw new IOException("Version of time array is incompatible with this class definition: version="+version+" expected="+VERSION);
        }
        
        if(!ad.hasNext()) throw new IOException("Cannot decode time array length");
        int size = ad.next();
        SortedIntArray sia = new SortedIntArray(size);
        int s=0;
        for(int i=0;i<size;i++) {
            if(!ad.hasNext()) throw new IOException("Cannot decode time array: not enough elements; required: "+size+" present: "+i);
            s+=ad.next();
            sia.add(s);
        }
        TimeSegment ts = new TimeSegment(t0);
        ts.tsarray = sia;
        return ts;
    }

    public long getT0() {
        return t0;
    }
}
