package org.yamcs.web.rest;

import java.util.List;

import org.yamcs.parameter.ParameterValueWithId;
import org.yamcs.parameter.ParameterWithIdConsumer;
import org.yamcs.protobuf.Pvalue.ParameterValue;


/**
 * Expected class type for use with {@link RestReplays}
 * Adds functionality for stopping a replay, and has support for pagination
 */
public abstract class RestReplayListener implements ParameterWithIdConsumer {
    
    private final boolean paginate;
    private final long pos;
    private final int limit;
    
    private int rowNr = 0; // zero-based
    private int emitted = 0;
    
    private boolean abortReplay = false;
    
    public RestReplayListener() {
        paginate = false;
        pos = -1;
        limit = -1;
    }
    
    public RestReplayListener(long pos, int limit) {
        paginate = true;
        this.pos = Math.max(pos, 0);
        this.limit = Math.max(limit, 0);
    }
    
    public void requestReplayAbortion() {
        abortReplay = true;
    }
    
    public boolean isReplayAbortRequested() {
        return abortReplay;
    }

    
    @Override
    public void update(int subscriptionId, List<ParameterValueWithId> params) {
        List<ParameterValueWithId> filteredData = filter(params);
        if (filteredData == null) return;
        
        if (paginate) {
            if (rowNr >= pos) {
                if (emitted < limit) {
                    emitted++;
                    onParameterData(filteredData);
                } else {
                    requestReplayAbortion();
                }
            }
            rowNr++;
        } else {
            onParameterData(filteredData);
        }
    }
    
    /**
     * this is called from the parameter archive where it is easier to get a protobuf ParameterValue instead of org.yamcs.ParameterValue (which needs a reference to an Xtce Parameter)
     * since we may not know that the parameter is at all in the XtceDB
     * @param params
     */
    public void update2(List<ParameterValue> params) {
        List<ParameterValue> filteredData = filter2(params);
        if (filteredData == null) return;
        
        if (paginate) {
            if (rowNr >= pos) {
                if (emitted < limit) {
                    emitted++;
                    onParameterData2(filteredData);
                } else {
                    requestReplayAbortion();
                }
            }
            rowNr++;
        } else {
            onParameterData2(filteredData);
        }
    }
    /**
     * Override to filter out some replay data. Null means excluded.
     * (which also means it will not be counted towards the pagination.
     */
    public List<ParameterValueWithId> filter(List<ParameterValueWithId> params) {
        return params;
    }
    
    /**
     * same as above but takes GPB ParameterValue instead
     * @param params
     * @return
     */
    public List<ParameterValue> filter2(List<ParameterValue> params) {
        return params;
    }
    
    public void onParameterData(List<ParameterValueWithId> params){};
    
    public void onParameterData2(List<ParameterValue> params) {}
    
    public void replayFinished(){};
}
