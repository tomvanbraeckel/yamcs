package org.yamcs.parameterarchive;

import java.util.List;

import org.rocksdb.RocksDB;
import org.yamcs.ParameterValue;
import org.yamcs.parameter.ParameterConsumer;

/**
 * Injects into the parameter archive, parameters from the realtime stream.
 * 
 * At the same time allows retrieval of parameters (for public consumption)
 * 
 * 
 * @author nm
 *
 */
public class RealtimeParameterFiller implements ParameterConsumer {
    final ParameterArchive paraDb;
    
    public RealtimeParameterFiller(ParameterArchive paraDb) {
        this.paraDb = paraDb;
    }
    
    @Override
    public void updateItems(int subscriptionId, List<ParameterValue> items) {
        // TODO Auto-generated method stub
        
    }
    
}
