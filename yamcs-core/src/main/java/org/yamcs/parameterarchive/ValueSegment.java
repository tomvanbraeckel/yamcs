package org.yamcs.parameterarchive;

import java.util.ListIterator;

import org.yamcs.ParameterValue;
import org.yamcs.protobuf.Yamcs.Value;

/**
 * An array of values for a parameter.
 * 
 * @author nm
 *
 */
public abstract class ValueSegment {
    /**
     * Add the parameter value on position pos
     * @param pos
     * @param parameterValue
     */
    public abstract void add(int pos, ParameterValue parameterValue);

    public abstract ListIterator<Value> getIterator(int pos);

}
