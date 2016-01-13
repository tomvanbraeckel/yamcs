package org.yamcs.parameterarchive;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.naming.ConfigurationException;

import org.yamcs.ParameterValue;
import org.yamcs.ProcessorFactory;
import org.yamcs.YProcessor;
import org.yamcs.YProcessorException;
import org.yamcs.YamcsServer;
import org.yamcs.archive.ReplayServer;
import org.yamcs.parameter.ParameterConsumer;
import org.yamcs.protobuf.Yamcs.EndAction;
import org.yamcs.protobuf.Yamcs.PacketReplayRequest;
import org.yamcs.protobuf.Yamcs.PpReplayRequest;
import org.yamcs.protobuf.Yamcs.ReplayRequest;
import org.yamcs.protobuf.Yamcs.ReplaySpeed;
import org.yamcs.protobuf.Yamcs.ReplaySpeed.ReplaySpeedType;
import org.yamcs.utils.TimeEncoding;


/**
 * Injects into the parameter archive, parameters from replay processors
 * 
 * 
 * @author nm
 *
 */
public class ReplayParameterFiller extends AbstractParameterFiller  {
    ReplayServer replayServer;
    long replayTime;
    ScheduledThreadPoolExecutor executor;
    long threshold = 60000;
    
    public ReplayParameterFiller(ParameterArchive parameterArchive) {
        super(parameterArchive);
    }


    @Override
    protected void doStart() {
        ReplayServer replayServer = YamcsServer.getService(parameterArchive.getYamcsInstance(), ReplayServer.class);
        
        if (replayServer == null) {
            String msg ="ReplayServer not configured for this instance"; 
            log.error(msg);
            notifyFailed(new ConfigurationException(msg));
            return;
        }
        executor = new ScheduledThreadPoolExecutor(1);
        notifyStarted();
    }
    
    public Future<?> scheduleRequest(final long start, final long stop) {
        return executor.submit(new Runnable() {
            @Override
            public void run() {
                buildArchive(start, stop);
            }
        });
    }
    
    
    private void buildArchive(long start, long stop) {
        collectionSegmentStart = SortedTimeSegment.getSegmentStart(start);
        long segmentEnd = SortedTimeSegment.getSegmentEnd(stop);
        //start ahead with one minute
        start = collectionSegmentStart-60000;
        log.info("Starting an parameter archive fillup for segment {} - {} ", TimeEncoding.toCombinedFormat(collectionSegmentStart), TimeEncoding.toCombinedFormat(segmentEnd));
        
        try {
            ParameterReplay pr = new ParameterReplay(start, stop);
            pr.run();
            flush();
        } catch (Exception e) {
           log.error("Error when creating replay" ,e);
        }
    }

    @Override
    protected void doStop() {
        executor.shutdown();
        notifyStopped();
    }

    class ParameterReplay implements ParameterConsumer {
        
        YProcessor yproc;
        public ParameterReplay(long start, long stop) throws org.yamcs.ConfigurationException, YProcessorException {
            ReplayRequest.Builder rrb = ReplayRequest.newBuilder().setSpeed(ReplaySpeed.newBuilder().setType(ReplaySpeedType.AFAP));
            rrb.setEndAction(EndAction.QUIT);
            rrb.setStart(start).setStop(stop);
            rrb.setPacketRequest(PacketReplayRequest.newBuilder().build());
            rrb.setPpRequest(PpReplayRequest.newBuilder().build());
            yproc = ProcessorFactory.create(parameterArchive.getYamcsInstance(), "ParameterArchive-buildup", "Archive", "internal", rrb.build());
            yproc.getParameterRequestManager().subscribeAll(this);
        }

        public void run() {
            yproc.start();
            yproc.awaitTerminated();
        }

        @Override
        public void updateItems(int subscriptionId, List<ParameterValue> items) {
            long t = processParameters(items);
            if(t<0)return;
            
            long nextSegmentStart = SortedTimeSegment.getNextSegmentStart(collectionSegmentStart);
            
            if(t>nextSegmentStart+threshold) {
                log.debug("Writing to archive the segment: [{} - {})", TimeEncoding.toString(collectionSegmentStart), TimeEncoding.toString(nextSegmentStart));
                Map<Integer, PGSegment> m = pgSegments.remove(collectionSegmentStart);
                if(m!=null) {
                    consolidateAndWriteToArchive(m.values());
                } else {
                    log.info("no data collected in this segment [{} - {})", TimeEncoding.toString(collectionSegmentStart), TimeEncoding.toString(nextSegmentStart));
                }
                collectionSegmentStart = nextSegmentStart;
            }
        }
    }
   
}
