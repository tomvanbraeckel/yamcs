package org.yamcs.parameterarchive;


import org.yamcs.YamcsServer;
import org.yamcs.parameterarchive.ParameterArchive;
import org.yamcs.web.BadRequestException;
import org.yamcs.web.HttpException;
import org.yamcs.web.rest.RestHandler;
import org.yamcs.web.rest.RestRequest;
import org.yamcs.web.rest.Route;
import org.yamcs.xtce.FloatParameterType;
import org.yamcs.xtce.IntegerParameterType;
import org.yamcs.xtce.Parameter;
import org.yamcs.xtce.ParameterType;
import org.yamcs.xtce.XtceDb;
import org.yamcs.xtceproc.XtceDbFactory;

import io.netty.channel.ChannelFuture;

/**
 * Provides some maitenance operations on the parameter archive
 * @author nm
 *
 */
public class ParameterArchiveMaintenanceRestHandler extends RestHandler {
    /**
     * A series is a list of samples that are determined in one-pass while processing a stream result.
     * Final API unstable.
     * <p>
     * If no query parameters are defined, the series covers *all* data.
     */
    @Route(path = "/api/archive/:instance/parametersArchive/reprocess")
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
        if(!req.hasQueryParameter("start")) {
            throw new BadRequestException("no start specified");
        }
        if(!req.hasQueryParameter("stop")) {
            throw new BadRequestException("no stop specified");
        }
        long start = req.getQueryParameterAsDate("start");
        long stop = req.getQueryParameterAsDate("stop");
        
        
        ParameterArchive parchive = getParameterArchive(instance);
        try {
            parchive.reprocess(start, stop);
        } catch (IllegalArgumentException e){
            throw new BadRequestException(e.getMessage());
        }
        
        return sendOK(req);
    }
   
    
    private static ParameterArchive getParameterArchive(String instance) throws BadRequestException {
        ParameterArchive parameterArchive = YamcsServer.getService(instance, ParameterArchive.class);
        if (parameterArchive == null) {
            throw new BadRequestException("ParameterArchive not configured for this instance");
        }
        return parameterArchive;
    }
    
}
