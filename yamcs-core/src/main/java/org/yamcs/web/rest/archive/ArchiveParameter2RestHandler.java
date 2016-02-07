package org.yamcs.web.rest.archive;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamcs.YamcsServer;
import org.yamcs.api.MediaType;
import org.yamcs.parameterarchive.MultiParameterDataRetrieval;
import org.yamcs.parameterarchive.MultipleParameterValueRequest;
import org.yamcs.parameterarchive.ParameterArchive;
import org.yamcs.parameterarchive.ParameterGroupIdDb;
import org.yamcs.parameterarchive.ParameterIdDb;
import org.yamcs.parameterarchive.ParameterIdValueList;
import org.yamcs.parameterarchive.ParameterValueArray;
import org.yamcs.parameterarchive.ParameterIdDb.ParameterId;
import org.yamcs.parameterarchive.SingleParameterDataRetrieval;
import org.yamcs.parameterarchive.SingleParameterValueRequest;
import org.yamcs.parameterarchive.ConsumerAbortException;
import org.yamcs.protobuf.Pvalue.ParameterData;
import org.yamcs.protobuf.Pvalue.ParameterValue;
import org.yamcs.protobuf.Pvalue.TimeSeries;
import org.yamcs.protobuf.SchemaPvalue;
import org.yamcs.protobuf.Yamcs.NamedObjectId;
import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.protobuf.Yamcs.Value.Type;
import org.yamcs.utils.DecodingException;
import org.yamcs.utils.IntArray;
import org.yamcs.utils.ParameterFormatter;
import org.yamcs.utils.TimeEncoding;
import org.yamcs.web.BadRequestException;
import org.yamcs.web.HttpException;
import org.yamcs.web.InternalServerErrorException;
import org.yamcs.web.NotFoundException;
import org.yamcs.web.rest.RestHandler;
import org.yamcs.web.rest.RestParameterReplayListener;
import org.yamcs.web.rest.RestRequest;
import org.yamcs.web.rest.Route;
import org.yamcs.web.rest.archive.RestDownsampler.Sample;
import org.yamcs.xtce.FloatParameterType;
import org.yamcs.xtce.IntegerParameterType;
import org.yamcs.xtce.Parameter;
import org.yamcs.xtce.ParameterType;
import org.yamcs.xtce.XtceDb;
import org.yamcs.xtceproc.XtceDbFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelFuture;

public class ArchiveParameter2RestHandler extends RestHandler {

    private static final Logger log = LoggerFactory.getLogger(ArchiveParameter2RestHandler.class);

    /**
     * A series is a list of samples that are determined in one-pass while processing a stream result.
     * Final API unstable.
     * <p>
     * If no query parameters are defined, the series covers *all* data.
     */
    @Route(path = "/api/archive/:instance/parameters2/:name*/samples")
    public ChannelFuture getParameterSamples(RestRequest req) throws HttpException {
        String instance = verifyInstance(req, req.getRouteParam("instance"));

        XtceDb mdb = XtceDbFactory.getInstance(instance);


        Parameter p = verifyParameter(req, mdb, req.getRouteParam("name"));

        ParameterType ptype = p.getParameterType();
        if (ptype == null) {
            throw new BadRequestException("Requested parameter has no type");
        } else if (!(ptype instanceof FloatParameterType) && !(ptype instanceof IntegerParameterType)) {
            throw new BadRequestException("Only integer or float parameters can be sampled. Got " + ptype.getTypeAsString());
        }

        long start = req.getQueryParameterAsDate("start", 0);
        long stop = req.getQueryParameterAsDate("stop", TimeEncoding.getWallclockTime());

        RestDownsampler sampler = new RestDownsampler(stop);

        ParameterArchive parchive = getParameterArchive(instance);
        ParameterIdDb piddb = parchive.getParameterIdDb();

        ParameterId[] pids = piddb.get(p.getQualifiedName());
        if(pids.length==0) {
            log.warn("No parameter id found in the parmaeter archive for {}", p.getQualifiedName());
            throw new NotFoundException(req);
        }
        ParameterGroupIdDb pgidDb = parchive.getParameterGroupIdDb();
        for(ParameterId pid: pids) {
            int parameterId = pid.pid;
            Value.Type engType = pids[0].engType;

            int[] pgids = pgidDb.getAllGroups(parameterId);
            if(pgids.length ==0 ){
                log.error("Found no parameter group for parameter Id {}", parameterId);
                continue;
            }
            log.info("Executing a single parameter value request for time interval [{} - {}] parameterId: {} and parameter groups: {}", TimeEncoding.toString(start), TimeEncoding.toString(stop), parameterId, Arrays.toString(pgids));
            SingleParameterValueRequest spvr = new SingleParameterValueRequest(start, stop, parameterId, pgids, true);
            retrieveDataForParameterId(parchive, engType, spvr, sampler);
        }

        TimeSeries.Builder series = TimeSeries.newBuilder();
        for (Sample s : sampler.collect()) {
            series.addSample(ArchiveHelper.toGPBSample(s));
        }

        return sendOK(req, series.build(), SchemaPvalue.TimeSeries.WRITE);
    }

    private void retrieveDataForParameterId(ParameterArchive parchive, Value.Type engType, SingleParameterValueRequest spvr, RestDownsampler sampler) throws HttpException {
        spvr.setRetrieveEngineeringValues(true);
        spvr.setRetrieveParameterStatus(false);
        spvr.setRetrieveRawValues(false);
        SingleParameterDataRetrieval spdr = new SingleParameterDataRetrieval(parchive, spvr);
        try {
            spdr.retrieve(new Consumer<ParameterValueArray>() {
                @Override
                public void accept(ParameterValueArray t) {

                    Object o = t.getEngValues();
                    long[] timestamps = t.getTimestamps();
                    int n = timestamps.length;
                    if(o instanceof float[]) {
                        float[] values = (float[])o;
                        for(int i=0;i<n;i++) {
                            sampler.process(timestamps[i], values[i]);
                        }
                    } else if(o instanceof double[]) {
                        double[] values = (double[])o;
                        for(int i=0;i<n;i++) {
                            sampler.process(timestamps[i], values[i]);
                        }
                    } else if(o instanceof long[]) {
                        long[] values = (long[])o;
                        for(int i=0;i<n;i++) {
                            if(engType==Type.UINT64) {
                                sampler.process(timestamps[i], unsignedLongToDouble(values[i]));
                            } else {
                                sampler.process(timestamps[i], values[i]);
                            }
                        }
                    } else if(o instanceof int[]) {
                        int[] values = (int[])o;
                        for(int i=0;i<n;i++) {
                            if(engType==Type.UINT32) {
                                sampler.process(timestamps[i], values[i]&0xFFFFFFFFL);
                            } else {
                                sampler.process(timestamps[i], values[i]);
                            }
                        }
                    } else {
                        log.warn("Unexpected value type " + o.getClass());
                    }

                }
            });
        } catch (RocksDBException | DecodingException e) {
            log.warn("Received exception during parmaeter retrieval ", e);
            throw new InternalServerErrorException(e.getMessage());
        }

    }
    private static ParameterArchive getParameterArchive(String instance) throws BadRequestException {
        ParameterArchive parameterArchive = YamcsServer.getService(instance, ParameterArchive.class);
        if (parameterArchive == null) {
            throw new BadRequestException("ParameterArchive not configured for this instance");
        }
        return parameterArchive;
    }

    /**copied from guava*/
    double unsignedLongToDouble(long x) {
        double d = (double) (x & 0x7fffffffffffffffL);
        if (x < 0) {
            d += 0x1.0p63;
        }
        return d;
    }
    @Route(path = "/api/archive/:instance/parameters2/:name*")
    public ChannelFuture listParameterHistory(RestRequest req) throws HttpException {
        String instance = verifyInstance(req, req.getRouteParam("instance"));

        XtceDb mdb = XtceDbFactory.getInstance(instance);
        Parameter p = verifyParameter(req, mdb, req.getRouteParam("name"));
        NamedObjectId id = NamedObjectId.newBuilder().setName(p.getQualifiedName()).build();

        if(req.hasQueryParameter("pos")) throw new BadRequestException("pos not supported");
        int limit = req.getQueryParameterAsInt("limit", 100);
        boolean noRepeat = req.getQueryParameterAsBoolean("norepeat", false);
        long start = req.getQueryParameterAsDate("start", 0);
        long stop = req.getQueryParameterAsDate("stop", TimeEncoding.getWallclockTime());
        boolean ascending = !req.asksDescending(true);

        ParameterArchive parchive = getParameterArchive(instance);
        ParameterIdDb piddb = parchive.getParameterIdDb();

        ParameterId[] pids = piddb.get(p.getQualifiedName());
        if(pids.length==0) {
            log.warn("No parameter id found in the parmaeter archive for {}", p.getQualifiedName());
            throw new NotFoundException(req);
        }
        ParameterGroupIdDb pgidDb = parchive.getParameterGroupIdDb();
        IntArray pidArray = new IntArray();
        IntArray pgidArray = new IntArray();

        for(ParameterId pid:pids) {
            int[] pgids = pgidDb.getAllGroups(pid.pid);
            for(int pgid: pgids) {
                pidArray.add(pid.pid);
                pgidArray.add(pgid);
            }
        }
        if(pidArray.isEmpty()) {
            log.error("No parameter group id found in the parameter archive for {}", p.getQualifiedName());
            throw new NotFoundException(req);
        } 
        NamedObjectId[] pnames = new NamedObjectId[pidArray.size()];
        Arrays.fill(pnames, id);
        MultipleParameterValueRequest mpvr = new MultipleParameterValueRequest(start, stop, pnames, pidArray.toArray(), pgidArray.toArray(), ascending);
        mpvr.setStoreUtcTime(true);
        // do not use set limit because the data can be filtered down (e.g. noRepeat) and the limit applies the final filtered data not to the input
        // one day the parameter archive will be smarter and do the filtering inside
        //mpvr.setLimit(limit);

        MultiParameterDataRetrieval mpdr = new MultiParameterDataRetrieval(parchive, mpvr);

        if (req.asksFor(MediaType.CSV)) {
            ByteBuf buf = req.getChannelHandlerContext().alloc().buffer();
            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new ByteBufOutputStream(buf)))) {
                List<NamedObjectId> idList = Arrays.asList(id);
                ParameterFormatter csvFormatter = new ParameterFormatter(bw, idList);
                limit++; // Allow one extra line for the CSV header
                RestParameterReplayListener replayListener = new RestParameterReplayListener(0, limit) {
                    @Override
                    public void onParameterData2(List<ParameterValue> params) {
                        try {
                            csvFormatter.writeParameters(params);
                        } catch (IOException e) {
                            log.error("Error while writing parameter line", e);
                        }
                    }
                };

                Consumer<ParameterIdValueList> consumer = new Consumer<ParameterIdValueList>() {
                    @Override
                    public void accept(ParameterIdValueList t) {
                        replayListener.update2(t.getValues());
                        if(replayListener.isReplayAbortRequested()) throw new ConsumerAbortException();
                    }
                }; 
                replayListener.setNoRepeat(noRepeat);
                mpdr.retrieve(consumer);
            } catch (IOException|DecodingException|RocksDBException e) {
                throw new InternalServerErrorException(e);
            }
            return sendOK(req, MediaType.CSV, buf);
        } else {
            ParameterData.Builder resultb = ParameterData.newBuilder();
            try {
                RestParameterReplayListener replayListener = new RestParameterReplayListener(0, limit) {
                    @Override
                    public void onParameterData2(List<ParameterValue> params) {
                        resultb.addAllParameter(params);
                    }
                };

                Consumer<ParameterIdValueList> consumer = new Consumer<ParameterIdValueList>() {
                    @Override
                    public void accept(ParameterIdValueList t) {
                        ParameterData pdata = ParameterData.newBuilder().addAllParameter(t.getValues()).build();
                        replayListener.update2(t.getValues());
                        if(replayListener.isReplayAbortRequested()) throw new ConsumerAbortException();
                    }
                }; 
                replayListener.setNoRepeat(noRepeat);
                mpdr.retrieve(consumer);
            } catch (DecodingException|RocksDBException e) {
                throw new InternalServerErrorException(e);
            }
            return sendOK(req, resultb.build(), SchemaPvalue.ParameterData.WRITE);
        }
    }
}
