package org.yamcs.web.rest.mdb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.plaf.synth.SynthSeparatorUI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamcs.Processor;
import org.yamcs.algorithms.AlgorithmManager;
import org.yamcs.protobuf.Mdb.AlgorithmInfo;
import org.yamcs.protobuf.Mdb.BulkGetParameterInfoRequest;
import org.yamcs.protobuf.Mdb.BulkGetParameterInfoResponse;
import org.yamcs.protobuf.Mdb.BulkGetParameterInfoResponse.GetParameterInfoResponse;
import org.yamcs.protobuf.Mdb.ChangeAlgorithmRequest;
import org.yamcs.protobuf.Mdb.ChangeParameterRequest;
import org.yamcs.protobuf.Mdb.ContainerInfo;
import org.yamcs.protobuf.Mdb.ListParametersResponse;
import org.yamcs.protobuf.Mdb.ParameterInfo;
import org.yamcs.protobuf.Mdb.ParameterTypeInfo;
import org.yamcs.protobuf.Mdb.UsedByInfo;
import org.yamcs.protobuf.Yamcs.NamedObjectId;
import org.yamcs.security.ObjectPrivilegeType;
import org.yamcs.security.SystemPrivilege;
import org.yamcs.web.BadRequestException;
import org.yamcs.web.HttpException;
import org.yamcs.web.rest.RestHandler;
import org.yamcs.web.rest.RestRequest;
import org.yamcs.web.rest.Route;
import org.yamcs.web.rest.mdb.XtceToGpbAssembler.DetailLevel;
import org.yamcs.xtce.Algorithm;
import org.yamcs.xtce.Container;
import org.yamcs.xtce.CustomAlgorithm;
import org.yamcs.xtce.EnumeratedParameterType;
import org.yamcs.xtce.NumericParameterType;
import org.yamcs.xtce.Parameter;
import org.yamcs.xtce.ParameterEntry;
import org.yamcs.xtce.ParameterType;
import org.yamcs.xtce.SequenceContainer;
import org.yamcs.xtce.XtceDb;
import org.yamcs.xtceproc.ProcessorData;
import org.yamcs.xtceproc.XtceDbFactory;

import static org.yamcs.web.rest.mdb.GbpToXtceAssembler.*;

/**
 * Handles incoming requests related to parameter info from the MDB
 */
public class MDBParameterRestHandler extends RestHandler {
    final static Logger log = LoggerFactory.getLogger(MDBParameterRestHandler.class);

    @Route(path = "/api/mdb/:instance/parameters/bulk", method = { "GET", "POST" }, priority = true)
    public void getBulkParameterInfo(RestRequest req) throws HttpException {
        checkSystemPrivilege(req, SystemPrivilege.GetMissionDatabase);

        String instance = verifyInstance(req, req.getRouteParam("instance"));
        XtceDb mdb = XtceDbFactory.getInstance(instance);

        BulkGetParameterInfoRequest request = req.bodyAsMessage(BulkGetParameterInfoRequest.newBuilder()).build();
        BulkGetParameterInfoResponse.Builder responseb = BulkGetParameterInfoResponse.newBuilder();
        for (NamedObjectId id : request.getIdList()) {
            Parameter p = mdb.getParameter(id);
            if (p == null) {
                throw new BadRequestException("Invalid parameter name specified " + id);
            }
            if (!hasObjectPrivilege(req, ObjectPrivilegeType.ReadParameter, p.getQualifiedName())) {
                log.warn("Not providing information about parameter {} because no privileges exists",
                        p.getQualifiedName());
                continue;
            }

            GetParameterInfoResponse.Builder response = GetParameterInfoResponse.newBuilder();
            response.setId(id);
            response.setParameter(XtceToGpbAssembler.toParameterInfo(p, DetailLevel.SUMMARY));
            responseb.addResponse(response);
        }

        completeOK(req, responseb.build());
    }

    @Route(path = "/api/mdb/:instance/parameters", method = "GET")
    @Route(path = "/api/mdb/:instance/parameters/:name*", method = "GET")
    public void getParameter(RestRequest req) throws HttpException {
        if (req.hasRouteParam("name")) {
            getParameterInfo(req);
        } else {
            listParameters(req);
        }
    }

    private void getParameterInfo(RestRequest req) throws HttpException {
        String instance = verifyInstance(req, req.getRouteParam("instance"));

        XtceDb mdb = XtceDbFactory.getInstance(instance);
        Parameter p = verifyParameter(req, mdb, req.getRouteParam("name"));

        ParameterInfo pinfo = XtceToGpbAssembler.toParameterInfo(p, DetailLevel.FULL);
        List<ParameterEntry> parameterEntries = mdb.getParameterEntries(p);
        if (parameterEntries != null) {
            ParameterInfo.Builder pinfob = ParameterInfo.newBuilder(pinfo);
            Set<SequenceContainer> usingContainers = new HashSet<>();
            for (ParameterEntry entry : parameterEntries) {
                Container containingContainer = entry.getContainer();
                if (containingContainer instanceof SequenceContainer) {
                    usingContainers.add((SequenceContainer) containingContainer);
                }
            }

            UsedByInfo.Builder usedByb = UsedByInfo.newBuilder();
            List<SequenceContainer> unsortedContainers = new ArrayList<>(usingContainers);
            Collections.sort(unsortedContainers, (c1, c2) -> c1.getQualifiedName().compareTo(c2.getQualifiedName()));
            for (SequenceContainer seqContainer : unsortedContainers) {
                ContainerInfo usingContainer = XtceToGpbAssembler.toContainerInfo(seqContainer, DetailLevel.LINK);
                usedByb.addContainer(usingContainer);
            }
            pinfob.setUsedBy(usedByb);
            pinfo = pinfob.build();
        }

        completeOK(req, pinfo);
    }

    private void listParameters(RestRequest req) throws HttpException {
        String instance = verifyInstance(req, req.getRouteParam("instance"));
        XtceDb mdb = XtceDbFactory.getInstance(instance);

        // Should eventually be replaced in a generic mdb search operation
        NameDescriptionSearchMatcher matcher = null;
        if (req.hasQueryParameter("q")) {
            matcher = new NameDescriptionSearchMatcher(req.getQueryParameter("q"));
        }

        boolean recurse = req.getQueryParameterAsBoolean("recurse", false);
        boolean details = req.getQueryParameterAsBoolean("details", false);

        // Support both type[]=float&type[]=integer and type=float,integer
        Set<String> types = new HashSet<>();
        if (req.hasQueryParameter("type")) {
            for (String type : req.getQueryParameterList("type")) {
                for (String t : type.split(",")) {
                    if (!"all".equalsIgnoreCase(t)) {
                        types.add(t.toLowerCase());
                    }
                }
            }
        }

        List<Parameter> matchedParameters = new ArrayList<>();
        if (req.hasQueryParameter("namespace")) {
            String namespace = req.getQueryParameter("namespace");
            for (Parameter p : mdb.getParameters()) {
                if (!hasObjectPrivilege(req, ObjectPrivilegeType.ReadParameter, p.getQualifiedName())) {
                    continue;
                }
                if (matcher != null && !matcher.matches(p)) {
                    continue;
                }

                String alias = p.getAlias(namespace);
                if (alias != null || (recurse && p.getQualifiedName().startsWith(namespace))) {
                    if (parameterTypeMatches(p, types)) {
                        matchedParameters.add(p);
                    }
                }
            }
        } else { // List all
            for (Parameter p : mdb.getParameters()) {
                if (!hasObjectPrivilege(req, ObjectPrivilegeType.ReadParameter, p.getQualifiedName())) {
                    continue;
                }
                if (matcher != null && !matcher.matches(p)) {
                    continue;
                }
                if (parameterTypeMatches(p, types)) {
                    matchedParameters.add(p);
                }
            }
        }

        Collections.sort(matchedParameters, (p1, p2) -> {
            return p1.getQualifiedName().compareTo(p2.getQualifiedName());
        });

        int totalSize = matchedParameters.size();

        String next = req.getQueryParameter("next", null);
        int pos = req.getQueryParameterAsInt("pos", 0);
        int limit = req.getQueryParameterAsInt("limit", 100);
        if (next != null) {
            NamedObjectPageToken pageToken = NamedObjectPageToken.decode(next);
            matchedParameters = matchedParameters.stream().filter(p -> {
                return p.getQualifiedName().compareTo(pageToken.name) > 0;
            }).collect(Collectors.toList());
        } else if (pos > 0) {
            matchedParameters = matchedParameters.subList(pos, matchedParameters.size());
        }

        NamedObjectPageToken continuationToken = null;
        if (limit < matchedParameters.size()) {
            matchedParameters = matchedParameters.subList(0, limit);
            Parameter lastParameter = matchedParameters.get(limit - 1);
            continuationToken = new NamedObjectPageToken(lastParameter.getQualifiedName());
        }

        ListParametersResponse.Builder responseb = ListParametersResponse.newBuilder();
        responseb.setTotalSize(totalSize);
        for (Parameter p : matchedParameters) {
            responseb.addParameter(
                    XtceToGpbAssembler.toParameterInfo(p, details ? DetailLevel.FULL : DetailLevel.SUMMARY));
        }
        if (continuationToken != null) {
            responseb.setContinuationToken(continuationToken.encodeAsString());
        }
        completeOK(req, responseb.build());
    }

    private boolean parameterTypeMatches(Parameter p, Set<String> types) {
        if (types.isEmpty()) {
            return true;
        }
        return p.getParameterType() != null
                && types.contains(p.getParameterType().getTypeAsString());
    }

    @Route(path = "/api/mdb/:instance/:processor/parameters/:name*", method = { "PATCH", "PUT",
            "POST" })
    public void setParameterCalibrators(RestRequest req) throws HttpException {
        checkSystemPrivilege(req, SystemPrivilege.ChangeMissionDatabase);

        Processor processor = verifyProcessor(req, req.getRouteParam("instance"), req.getRouteParam("processor"));
        XtceDb xtcedb = XtceDbFactory.getInstance(processor.getInstance());
        Parameter p = verifyParameter(req, xtcedb, req.getRouteParam("name"));
        ChangeParameterRequest cpr = req.bodyAsMessage(ChangeParameterRequest.newBuilder()).build();
        ProcessorData pdata = processor.getProcessorData();
        ParameterType origParamType = p.getParameterType();

        switch (cpr.getAction()) {
        case RESET:
            pdata.clearParameterOverrides(p);
            break;
        case RESET_CALIBRATORS:
            pdata.clearParameterCalibratorOverrides(p);
            break;
        case SET_CALIBRATORS:
            verifyNumericParameter(p);
            if (cpr.hasDefaultCalibrator()) {
                pdata.setDefaultCalibrator(p, toCalibrator(cpr.getDefaultCalibrator()));
            }
            pdata.setContextCalibratorList(p,
                    toContextCalibratorList(xtcedb, p.getSubsystemName(), cpr.getContextCalibratorList()));
            break;
        case SET_DEFAULT_CALIBRATOR:
            verifyNumericParameter(p);
            if (cpr.hasDefaultCalibrator()) {
                pdata.setDefaultCalibrator(p, toCalibrator(cpr.getDefaultCalibrator()));
            } else {
                pdata.removeDefaultCalibrator(p);                
            }
            break;
        case RESET_ALARMS:
            pdata.clearParameterAlarmOverrides(p);
            break;
        case SET_DEFAULT_ALARMS:
            if (!cpr.hasDefaultAlarm()) {
                pdata.removeDefaultAlarm(p);
            } else {
                if (origParamType instanceof NumericParameterType) {
                    pdata.setDefaultNumericAlarm(p, toNumericAlarm(cpr.getDefaultAlarm()));
                } else if (origParamType instanceof EnumeratedParameterType) {
                    pdata.setDefaultEnumerationAlarm(p, toEnumerationAlarm(cpr.getDefaultAlarm()));
                } else {
                    throw new BadRequestException("Can only set alarms on numeric or enumerated parameters");
                }
            }
            break;
        case SET_ALARMS:
            if (origParamType instanceof NumericParameterType) {
                if (cpr.hasDefaultAlarm()) {
                    pdata.setDefaultNumericAlarm(p, toNumericAlarm(cpr.getDefaultAlarm()));
                }
                pdata.setNumericContextAlarm(p,
                        toNumericContextAlarm(xtcedb, p.getSubsystemName(), cpr.getContextAlarmList()));
            } else if (origParamType instanceof EnumeratedParameterType) {
                if (cpr.hasDefaultAlarm()) {
                    pdata.setDefaultEnumerationAlarm(p, toEnumerationAlarm(cpr.getDefaultAlarm()));
                }
                pdata.setEnumerationContextAlarm(p,
                        toEnumerationContextAlarm(xtcedb, p.getSubsystemName(), cpr.getContextAlarmList()));
            } else {
                throw new BadRequestException("Can only set alarms on numeric or enumerated parameters");
            }
            break;
        default:
            throw new BadRequestException("Unknown action " + cpr.getAction());

        }
        ParameterType ptype = pdata.getParameterType(p);
        ParameterTypeInfo pinfo = XtceToGpbAssembler.toParameterTypeInfo(ptype, DetailLevel.FULL);
        completeOK(req, pinfo);
    }

    @Route(path = "/api/mdb/:instance/:processor/algorithms/:name*", method = { "PATCH", "PUT", "POST" })
    public void setAlgorithm(RestRequest req) throws HttpException {
        checkSystemPrivilege(req, SystemPrivilege.ChangeMissionDatabase);

        Processor processor = verifyProcessor(req, req.getRouteParam("instance"), req.getRouteParam("processor"));
        List<AlgorithmManager> l = processor.getServices(AlgorithmManager.class);
        if (l.size() == 0) {
            throw new BadRequestException("No AlgorithmManager available for this processor");
        }
        if (l.size() > 1) {
            throw new BadRequestException(
                    "Cannot patch algorithm when a processor has more than 1 AlgorithmManager services");
        }
        AlgorithmManager algMng = l.get(0);
        XtceDb xtcedb = XtceDbFactory.getInstance(processor.getInstance());
        Algorithm a = verifyAlgorithm(req, xtcedb, req.getRouteParam("name"));
        if (!(a instanceof CustomAlgorithm)) {
            throw new BadRequestException("Can only patch CustomAlgorithm instances");
        }
        CustomAlgorithm calg = (CustomAlgorithm) a;
        ChangeAlgorithmRequest car = req.bodyAsMessage(ChangeAlgorithmRequest.newBuilder()).build();
        log.debug("received ChangeAlgorithmRequest {}", car);
        switch (car.getAction()) {
        case RESET:
            algMng.clearAlgorithmOverride(calg);
            break;
        case SET:
            if (!car.hasAlgorithm()) {
                throw new BadRequestException("No algorithm info provided");
            }
            AlgorithmInfo ai = car.getAlgorithm();
            if (!ai.hasText()) {
                throw new BadRequestException("No algorithm text provided");
            }
            try {
                log.debug("Setting text for algorithm {} to {}", calg.getQualifiedName(), ai.getText());
                algMng.setAlgorithmText(calg, ai.getText());
            } catch (Exception e) {
                System.out.println("here ---------- " + e.getMessage());
                throw new BadRequestException(e.getMessage());
            }
            break;
        default:
            throw new BadRequestException("Unknown action " + car.getAction());
        }

        completeOK(req);
    }

    private static void verifyNumericParameter(Parameter p) throws BadRequestException {
        ParameterType ptype = p.getParameterType();
        if (!(ptype instanceof NumericParameterType)) {
            throw new BadRequestException(
                    "Cannot set a calibrator on a non numeric parameter type (" + ptype.getTypeAsString() + ")");
        }
    }
}
