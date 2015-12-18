package org.yamcs.parameterarchive;

import java.io.IOException;
import java.util.Arrays;
import java.util.PrimitiveIterator;

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
    public static final long SEGMENT_MASK = ~TIMESTAMP_MASK;
    
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
     * get timestamp at position idx
     * @param idx
     * @return
     */
    public long get(int idx) {
        return tsarray.get(idx) | t0;
    }

    /**
     * Constructs an ascending iterator starting from a specified value (inclusive) 
     * 
     * @param startFrom
     * @return
     */
    public PrimitiveIterator.OfLong getAscendingIterator(long startFrom) {
        return new PrimitiveIterator.OfLong() {
            PrimitiveIterator.OfInt it = tsarray.getAscendingIterator((int)(startFrom&TIMESTAMP_MASK));

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }
            
            @Override
            public long nextLong() {
                return t0+ it.nextInt();
                
            }
        };
    }
    
    /**
     * Constructs an descending iterator starting from a specified value (exclusive) 
     * 
     * @param startFrom
     * @return
     */
    public PrimitiveIterator.OfLong getDescendingIterator(long startFrom) {
        return new PrimitiveIterator.OfLong() {
            PrimitiveIterator.OfInt it = tsarray.getDescendingIterator((int)(startFrom&TIMESTAMP_MASK));

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }
            
            @Override
            public long nextLong() {
                return t0 + it.nextInt();
                
            }
        };
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
        int pos = VarIntUtil.writeVarint32(buf, 0, tsarray.size());
        int x = tsarray.get(0);
        pos = VarIntUtil.writeVarint32(buf, pos, x);
        for(int i=1; i<tsarray.size(); i++) {
            int y = tsarray.get(i);
            pos = VarIntUtil.writeVarint32(buf, pos, (y-x));
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
    
    /**
     * returns the start of the segment where instant fits
     * @param instant
     * @return
     */
    public static long getSegmentStart(long instant) {
        return instant & SEGMENT_MASK;
    }
    
    /**
     * returns the ID of the segment where the instant fits - this is the same with segment start
     * @param instant
     * @return
     */
    public static long getSegmentId(long instant) {
        return instant & SEGMENT_MASK;
    }
    /**
     * returns the end of the segment where the instant fits
     * @param segmentId
     * @return
     */
    public static long getSegmentEnd(long segmentId) {
        return segmentId  | TIMESTAMP_MASK;
    }
    
    /**
     * returns true if the segment overlaps the [start,stop) interval
     * @param segmentId
     * @param start
     * @param stop
     * @return
     */
    public static boolean overlap(long segmentId, long start, long stop) {
        long segmentStart = segmentId;
        long segmentStop = getSegmentEnd(segmentId);
        
        return start<segmentStop && stop>segmentStart;
        
    }

   

    public int search(long t) {
        return tsarray.search((int)(t&TIMESTAMP_MASK));
    }

    public int size() {
        return tsarray.size();
    }
    
    public String toString() {
        return "[TimeSegment: t0:"+t0+", relative times: "+ tsarray.toString()+"]";
    }
  
}
