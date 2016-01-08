package org.yamcs.parameterarchive;

import java.nio.ByteBuffer;
import java.util.PrimitiveIterator;

import org.yamcs.protobuf.ValueHelper;
import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.utils.SortedIntArray;
import org.yamcs.utils.VarIntUtil;

/**
 * TimeSegment stores timestamps relative to a t0. 
 * The timestamps are stored in a sorted int array.
 * 
 * @author nm
 *
 */
public class SortedTimeSegment extends ValueSegment {
    public static final int NUMBITS_MASK=22; //2^22 millisecons =~ 70 minutes per segment    
    public static final int TIMESTAMP_MASK = (0xFFFFFFFF>>>(32-NUMBITS_MASK));
    public static final long SEGMENT_MASK = ~TIMESTAMP_MASK;
    
    public static final int VERSION = 0;
    
    final private long segmentStart;    
    private SortedIntArray tsarray;
    
    
    public SortedTimeSegment(long segmentStart) {
        super(FORMAT_ID_SortedTimeValueSegment);
        if((segmentStart & TIMESTAMP_MASK) !=0) throw new IllegalArgumentException("t0 must be 0 in last "+NUMBITS_MASK+" bits");
        
        tsarray = new SortedIntArray();
        this.segmentStart = segmentStart;
    }
    
    /**
     * Insert instant into the array and return the position at which it has been inserted.
     * 
     * @param instant
     */
    public int add(long instant) {
        if((instant&SEGMENT_MASK) != segmentStart) {
            throw new IllegalArgumentException("This timestamp does not fit into this segment");
        }
        return tsarray.insert((int)(instant & TIMESTAMP_MASK));
    }
    
    /**
     * get timestamp at position idx
     * @param idx
     * @return
     */
    public long getTime(int idx) {
        return tsarray.get(idx) | segmentStart;
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
                return segmentStart+ it.nextInt();
                
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
                return segmentStart + it.nextInt();
                
            }
        };
    }
    
    
    public long getT0() {
        return segmentStart;
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

   
    /**
     * performs a binary search in the time segment and returns the position of t or where t would fit in. 
     * 
     * @see java.util.Arrays#binarySearch(int[], int)
     * @param x
     * @return
     */
    public int search(long instant) {
        if((instant&SEGMENT_MASK) != segmentStart) {
            throw new IllegalArgumentException("This timestamp does not fit into this segment");
        }
        return tsarray.search((int)(instant&TIMESTAMP_MASK));
    }

    public int size() {
        return tsarray.size();
    }
    
    public long getSegmentStart() {
        return segmentStart;
    }
    
    public String toString() {
        return "[TimeSegment: t0:"+segmentStart+", relative times: "+ tsarray.toString()+"]";
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
    @Override
    public void writeTo(ByteBuffer buf) {
        if(tsarray.size()==0) throw new IllegalStateException(" the time segment has no data");
        
        VarIntUtil.writeVarInt32(buf, tsarray.size());
        int x = tsarray.get(0);
        VarIntUtil.writeVarInt32(buf, x);
        for(int i=1; i<tsarray.size(); i++) {
            int y = tsarray.get(i);
            VarIntUtil.writeVarInt32(buf, (y-x));
            x = y;
        }
    }

    /**
     * Creates a TimeSegment by decoding the buffer 
     * this is the reverse of the {@link #encode()} operation
     * 
     * @param buf
     * @return
     */
    @Override
    public void parseFrom(ByteBuffer buf) {
        int size = VarIntUtil.readVarInt32(buf);
        tsarray = new SortedIntArray(size);
        int s=0;
        for(int i=0;i<size;i++) {
            s+=VarIntUtil.readVarInt32(buf);
            tsarray.insert(s);
        }
    }

    @Override
    public int getMaxSerializedSize() {
        return 4*(tsarray.size())+3;
    }

    @Override
    public Value get(int index) {        
        return ValueHelper.newTimestampValue(getTime(index));
    }

    public long getSegmentEnd() {
        return getSegmentEnd(segmentStart);
    }

    public long[] getRange(int posStart, int posStop, boolean ascending) {
        long[] r = new long[posStop-posStart];
        if(ascending) {
            for(int i = posStart; i<posStop; i++) {
                r[i-posStart] = tsarray.get(i)|segmentStart;
            }
        } else {
            for(int i = posStop; i>posStart; i--) {
                r[posStop-i] = tsarray.get(i)|segmentStart;
            }
        }
        return r;
    }
}
