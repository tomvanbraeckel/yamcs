package org.yamcs.parameterarchive;

import org.yamcs.ParameterValue;

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
    public abstract void add(int pos, ParameterValue parameterValue) ;      

}
