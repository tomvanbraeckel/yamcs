package org.yamcs.xtce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class EnumeratedDataType extends BaseDataType {
    private static final long serialVersionUID = 201002231432L;
  

    protected HashMap<Long,ValueEnumeration> enumeration = new HashMap<>();
    protected List<ValueEnumeration> enumerationList = new ArrayList<>();//this keeps track of the duplicates but is not really used 
    protected List<ValueEnumerationRange> ranges = null;

    protected String initialValue = null;

    
    EnumeratedDataType(String name) {
        super(name);
    }
    /**
     * performs a shallow copy of this object into t
     * @param t
     */
    protected EnumeratedDataType(EnumeratedDataType t) {
        super(t);
        this.enumeration = t.enumeration;
        this.enumerationList = t.enumerationList;
        this.ranges = t.ranges;
    }
    /**
     * Set initial value
     * @param initialValue
     */
    public void setInitialValue(String initialValue) {
        this.initialValue = initialValue;
    }

    public String getInitialValue() {
        return initialValue;
    }

    public ValueEnumeration enumValue(Long key) {
        if ( enumeration.containsKey(key) ) {
            return enumeration.get(key);
        } else if ( ranges != null ) {
            for (ValueEnumerationRange range:ranges) {
                if (range.isValueInRange(key)) {
                    return new ValueEnumeration(key, range.getLabel());
                }
            }
        }
        return null;
    }

    public ValueEnumeration enumValue(String label) {
        for(ValueEnumeration enumeration:enumerationList) {
            if(enumeration.getLabel().equals(label)) {
                return enumeration;
            }
        }
        return null;
    }
    
    public boolean hasLabel(String label) {
        for(ValueEnumeration enumeration:enumerationList) {
            if(enumeration.getLabel().equals(label)) {
                return true;
            }
        }
        if ( ranges != null ) {
            for (ValueEnumerationRange range:ranges) {
                if (range.getLabel().equals(label)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Add value to enumeration list
     * @param value Integer value
     * @param label Label associated with value
     */
    public void addEnumerationValue(long value, String label) {
        ValueEnumeration valEnum = new ValueEnumeration(value, label);
        enumerationList.add(valEnum);
        enumeration.put(value, valEnum);
    }

    /**
     * Add range to enumeration list
     */
    public void addEnumerationRange(double min, double max, boolean isMinInclusive, boolean isMaxInclusive, String label) {
        assert(min < max);
        ValueEnumerationRange range = new ValueEnumerationRange(min, max, isMinInclusive, isMaxInclusive, label);
        ranges.add(range);
    }

    public void addEnumerationRange(ValueEnumerationRange range) {
        if ( ranges == null ) {
            ranges = new ArrayList<>(2);
        }
        ranges.add(range);
    }

    public List<ValueEnumeration> getValueEnumerationList() {
        return Collections.unmodifiableList(enumerationList);
    }

    /**
     * returns stringValue
     */
    @Override
    public Object parseString(String stringValue) {
        return stringValue;
    }
    
   
}

