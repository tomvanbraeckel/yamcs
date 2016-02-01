package org.yamcs.parameterarchive;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.utils.DecodingException;
import org.yamcs.utils.IntArray;
import org.yamcs.utils.VarIntUtil;

/**
 * Segment for all non primitive types.
 *
 * It assumes that each object is encoded to a binary that is not compressed. The compression of the segment (if any) is realized by not repeating elements.
 *  
 * Finds best encoding among:
 *  - raw - list of values stored verbatim, each preceded by its size
 *  - enum - the list of unique values are stored at the beginning of the segment - each value has implicitly an id (the order in the list) 
 *    - the rest of the segment is a list of ids and can be encoded in one of the following formats
 *      - raw - 4 bytes for each id
 *      - run length encoded
 *      - coded  with an integer codec
 *  
 * 
 * @author nm
 *
 */
public class ObjectSegment<E> extends BaseSegment {
    final static int SUBFORMAT_ID_RAW = 0;
    final static int SUBFORMAT_ID_ENUM_RAW = 1;
    final static int SUBFORMAT_ID_ENUM_RLE = 2;
    final static int SUBFORMAT_ID_ENUM_FPROF = 3;

    //count for each status in the status list - the sum of all counts is equal with size
    IntArray counts;
    List<E> objectList;
    List<HashableByteArray> serializedObjectList;
    Map<HashableByteArray, Integer> valuemap;
    List<E> unique;
    
    boolean runLengthEncoded = false;
    
    int size = 0;
    final ObjectSerializer<E> objSerializer;
    
    
    int rawSize = 0;
    int enumRawSize = 0;
    int enumRleSize = 0;
    int lastId = 0;

    ObjectSegment(ObjectSerializer<E> objSerializer) {
        super(objSerializer.getFormatId());
        objectList = new ArrayList<E>();
        counts = new IntArray();
        this.objSerializer = objSerializer;
    }


    /**
     * add element to the end of the segment
     * 
     * @param e
     */
    public void add(E e) {
        boolean added = false;
        byte[] b = objSerializer.serialize(e);
        HashableByteArray hba = new HashableByteArray(b);
        rawSize+=1+b.length;
        
        
        if(!objectList.isEmpty()) {
            HashableByteArray lastElement = serializedObjectList.get(serializedObjectList.size()-1);
            if(hba.equals(lastElement)) {               
                enumRleSize+=1;
            } else {
                enumRleSize+=1+b.length;
            }
        } else {
            enumRleSize+=1+b.length;
        }
        
        
        if(valuemap.containsKey(hba)) {
            enumRawSize++;
        } else {
            valuemap.put(hba, lastId++);
            enumRawSize+=1+b.length;
        }
        
        
        size++;
    }

    @Override
    public void writeTo(ByteBuffer bb) {
        //first write the counts
        VarIntUtil.writeVarInt32(bb, counts.size());
        for(int i=0; i<counts.size(); i++) {
            VarIntUtil.writeVarInt32(bb, counts.get(i));
        }

        //then write the flags
        VarIntUtil.writeVarInt32(bb, objectList.size());

        for(int i=0; i<objectList.size(); i++) {
            byte[] sps = objSerializer.serialize(objectList.get(i));
            VarIntUtil.writeVarInt32(bb, sps.length);
            bb.put(sps);
        }
    }

    @Override
    public void parseFrom(ByteBuffer bb) throws DecodingException {

        int countNum = VarIntUtil.readVarInt32(bb);
        counts = new IntArray(countNum);
        size = 0;

        for(int i=0; i<countNum; i++) {
            int c = VarIntUtil.readVarInt32(bb);
            counts.add(c);
            size+=c;

        }

        int statusNum = VarIntUtil.readVarInt32(bb);
        objectList = new ArrayList<>(statusNum);
        for(int i=0; i<statusNum; i++) {
            int size = VarIntUtil.readVarInt32(bb);
            byte[] b = new byte[size];
            bb.get(b);
            E e = objSerializer.deserialize(b);
            objectList.add(e);
        }
    }

    @Override
    public int getMaxSerializedSize() {
        int size = 4*counts.size()+4*objectList.size();
        for(E e: objectList) {
            size+= objSerializer.getEstimatedSize(e);
        }
        return size;
    }

    @Override
    public E[] getRange(int posStart, int posStop, boolean ascending) {
        if(posStart>=posStop) throw new IllegalArgumentException("posStart has to be smaller than posStop");
        if(ascending) {
            return getRangeAscending(posStart, posStop);
        } else {
            return getRangeDescending(posStart, posStop);
        }
    }

    public  E[] getRangeAscending(int posStart, int posStop) {

        if(posStop>size) throw new IndexOutOfBoundsException("Index: "+posStop+" size: "+size);

        int n = posStop-posStart;
        @SuppressWarnings("unchecked")
        E[] r = (E[]) Array.newInstance(objectList.get(0).getClass(), n);

        int k = posStart;
        int i = 0;
        while(k>=counts.get(i)) {
            k-=counts.get(i++);
        }
        int pos = 0;

        while(pos<n) {
            r[pos++] = objectList.get(i);
            k++;
            if(k>=counts.get(i)) {
                i++;
                k=0;
            }
        }
        return r;
    }

    public  E[] getRangeDescending(int posStart, int posStop) {
        if(posStop>=size) throw new IndexOutOfBoundsException("Index: "+posStop+" size: "+size);

        int n = posStop-posStart;
        @SuppressWarnings("unchecked")
        E[] r = (E[]) Array.newInstance(objectList.get(0).getClass(), n);

        int k = size - posStop;
        int i = counts.size()-1;
        while(k>counts.get(i)) {
            k-=counts.get(i--);
        }
        k=counts.get(i)-k;

        int pos = 0;

        while(true) {
            r[pos++] = objectList.get(i);
            if(pos==n) break;

            k--;
            if(k<0) {
                i--;
                k=counts.get(i)-1;
            }
        }
        return r;
    }


    public E get(int index) {
        int k = 0;
        int i = 0;
        while(k<=index) {
            k+=counts.get(i);
            i++;
        }
        return objectList.get(i-1);
    }


    /**
     * the number of elements in this segment (not taking into account any compression due to run-length encoding)
     * @return
     */
    @Override
    public int size() {
        return size;
    }
}

/**
 * wrapper around byte[] to allow it to be used in HashMaps
 */
class HashableByteArray {
    private int hash =0 ;
    final byte[] b;

    public HashableByteArray(byte[] b) {
        this.b = b ;
    }

    @Override
    public int hashCode() {
        if (hash == 0) {
            hash = Arrays.hashCode(b);
        }            
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HashableByteArray other = (HashableByteArray) obj;
        if (!Arrays.equals(b, other.b))
            return false;
        return true;
    }
}

interface ObjectSerializer<E> {
    byte getFormatId();
    int getEstimatedSize(E e);
    E deserialize(byte[] b);
    byte[] serialize(E e);
    boolean equals(E e1, E e2);
}
