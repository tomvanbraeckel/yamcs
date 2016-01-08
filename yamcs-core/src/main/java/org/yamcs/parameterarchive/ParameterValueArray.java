package org.yamcs.parameterarchive;
/**
 * an array of values for one parameter
 * @author nm
 *
 */
public class ParameterValueArray {
    final int parameterId;
    final long[] timestamps;
    //values is an array of primitives
    Object values;
    
    public ParameterValueArray(int parameterId, long timestamps[], Object values) {
        this.parameterId = parameterId;
        this.timestamps = timestamps;
        this.values = values;
    }
}
