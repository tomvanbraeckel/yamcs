package org.yamcs.parameterarchive;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Test;
import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.utils.ValueUtility;

public class GenericValueSegmentTest {
	@Test
	public void test1() throws Exception {
		Value v1 = ValueUtility.getSint32Value(3);
		Value v2 = ValueUtility.getSint32Value(30);
		Value v3 = ValueUtility.getUint32Value(3);
		
		GenericValueSegment gvs = new GenericValueSegment(1);
		gvs.add(0, v1);
		gvs.add(1, v2);
		gvs.add(2, v3);
		
		ByteArrayOutputStream sout = new ByteArrayOutputStream();
		gvs.writeTo(sout);
		byte[] buf = sout.toByteArray();
		System.out.println("buf.length: "+buf.length);
		
		ByteArrayInputStream sin = new ByteArrayInputStream(buf);
		GenericValueSegment gvs1 = new GenericValueSegment(1);
		gvs1.parseFrom(sin);
		
		assertEquals(3, gvs1.values.size());
		assertEquals(v1, gvs1.values.get(0));
		assertEquals(v2, gvs1.values.get(1));
		assertEquals(v3, gvs1.values.get(2));
	}
}
