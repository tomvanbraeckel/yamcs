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


    public SegmentKey(int parameterId, int parameterGroupId, long segmentStart) {
        this.parameterId = parameterId;
        this.parameterGroupId = parameterGroupId;
        this.segmentStart = segmentStart;
    }

    public byte[] encode() {
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putInt(parameterId);
        bb.putInt(parameterGroupId);
        bb.putLong(segmentStart);
        return bb.array();
    }

    public static SegmentKey decode(byte[] b) {
        ByteBuffer bb = ByteBuffer.wrap(b);
        int parameterId = bb.getInt();
        int parameterGroupId = bb.getInt();
        long segmentStart = bb.getLong();
        return new SegmentKey(parameterId, parameterGroupId, segmentStart);
    }
}