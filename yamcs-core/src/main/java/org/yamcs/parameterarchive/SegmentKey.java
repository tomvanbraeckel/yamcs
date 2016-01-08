package org.yamcs.parameterarchive;

import java.nio.ByteBuffer;

/**
 * Holder, encoder and decoder for the segment keys (in the sense of key,value storage used for RocksDb)
 *   
 * @author nm
 *
 */
class SegmentKey {
    final int parameterId;
    final int parameterGroupId;
    final long segmentStart;
    byte type;
    public static final byte TYPE_ENG_VALUE = 0;
    public static final byte TYPE_RAW_VALUE = 1;
    public static final byte TYPE_FLAGS = 2;
    
    public SegmentKey(int parameterId, int parameterGroupId, long segmentStart, byte type) {
        this.parameterId = parameterId;
        this.parameterGroupId = parameterGroupId;
        this.segmentStart = segmentStart;
        this.type = type;
    }
    
    public SegmentKey(int parameterId, int parameterGroupId, long segmentStart) {
        this(parameterId, parameterGroupId, segmentStart, TYPE_ENG_VALUE);
    }

    public byte[] encode() {
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putInt(parameterId);
        bb.put(type);
        bb.putInt(parameterGroupId);
        bb.putLong(segmentStart);
        return bb.array();
    }

    public static SegmentKey decode(byte[] b) {
        ByteBuffer bb = ByteBuffer.wrap(b);
        int parameterId = bb.getInt();
        byte type = bb.get();
        int parameterGroupId = bb.getInt();
        long segmentStart = bb.getLong();
        return new SegmentKey(parameterId, parameterGroupId, segmentStart, type);
    }
}