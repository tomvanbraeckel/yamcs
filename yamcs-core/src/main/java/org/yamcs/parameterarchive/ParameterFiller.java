package org.yamcs.parameterarchive;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.yamcs.ParameterValue;

/**
 * Injects into the parameter archive, parameters from replay processors
 * 
 * 
 * @author nm
 *
 */
public class ParameterFiller extends AbstractParameterFiller {
	public ParameterFiller(ParameterArchive parameterArchive) {
		super(parameterArchive);
	}


	@Override
	protected void doStart() {
		executor = new ScheduledThreadPoolExecutor(1);
		notifyStarted();
	}

	@Override
	protected void doStop() {
		executor.shutdown();
        notifyStopped();
	}


	@Override
	public void updateItems(int subscriptionId, List<ParameterValue> items) {
		executor.execute(() -> {updateItems(subscriptionId, items);});
	}

	@Override
	protected void doUpdateItems(int subscriptionId, List<ParameterValue> items) {
		Map<Long, SortedParameterList> m = new HashMap<>();
		for(ParameterValue pv: items) {
			long t = pv.getAcquisitionTime();

			SortedParameterList l = m.get(t);
			if(l==null) {
				l = new SortedParameterList();
				m.put(t, l);
			}
			l.add(pv);
		}

		for(Map.Entry<Long,SortedParameterList> entry: m.entrySet()) {
			long t = entry.getKey();
			SortedParameterList pvList = entry.getValue();
			processUpdate(t, pvList);
		}
	}
}
