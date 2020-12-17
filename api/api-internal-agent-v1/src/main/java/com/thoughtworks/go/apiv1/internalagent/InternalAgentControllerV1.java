/*
 * Copyright 2020 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.apiv1.internalagent;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.apiv1.internalagent.representers.*;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.request.*;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.messaging.BuildRepositoryMessageProducer;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseForbidden;
import static java.lang.String.valueOf;
import static spark.Spark.*;

@Component
public class InternalAgentControllerV1 extends ApiController implements SparkSpringController {
    private final BuildRepositoryMessageProducer buildRepositoryMessageProducer;

    @Autowired
    public InternalAgentControllerV1(BuildRepositoryMessageProducer buildRepositoryMessageProducer) {
        super(ApiVersion.v1);
        this.buildRepositoryMessageProducer = buildRepositoryMessageProducer;
    }

    @Override
    public String controllerBasePath() {
        return Routes.InternalAgent.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", mimeType, this::verifyContentType);
            before("/*", mimeType, this::verifyContentType);

            post(Routes.InternalAgent.PING, mimeType, this::ping);
            post(Routes.InternalAgent.REPORT_CURRENT_STATUS, mimeType, this::reportCurrentStatus);
            post(Routes.InternalAgent.REPORT_COMPLETING, mimeType, this::reportCompleting);
            post(Routes.InternalAgent.REPORT_COMPLETED, mimeType, this::reportCompleted);
            post(Routes.InternalAgent.IS_IGNORED, mimeType, this::isIgnored);
            post(Routes.InternalAgent.GET_COOKIE, mimeType, this::getCookie);
            post(Routes.InternalAgent.GET_WORK, mimeType, this::getWork);
        });
    }

    public String ping(Request request, Response response) {
        PingRequest pingRequest = PingRequestRepresenter.fromJSON(request.body());
        ensureAgentIsMakingARequestForItself(pingRequest, request);

        AgentInstruction agentInstruction = buildRepositoryMessageProducer.ping(pingRequest.getAgentRuntimeInfo());

        return AgentInstructionRepresenter.toJSON(agentInstruction);
    }

    public String reportCurrentStatus(Request request, Response response) {
        ReportCurrentStatusRequest req = ReportCurrentStatusRequestRepresenter.fromJSON(request.body());
        ensureAgentIsMakingARequestForItself(req, request);

        buildRepositoryMessageProducer.reportCurrentStatus(req.getAgentRuntimeInfo(), req.getJobIdentifier(),
                req.getJobState());

        return NOTHING;
    }

    public String reportCompleting(Request request, Response response) {
        ReportCompleteStatusRequest req = ReportCompleteStatusRequestRepresenter.fromJSON(request.body());
        ensureAgentIsMakingARequestForItself(req, request);

        buildRepositoryMessageProducer.reportCompleting(req.getAgentRuntimeInfo(), req.getJobIdentifier(),
                req.getJobResult());

        return NOTHING;
    }

    public String reportCompleted(Request request, Response response) {
        ReportCompleteStatusRequest req = ReportCompleteStatusRequestRepresenter.fromJSON(request.body());
        ensureAgentIsMakingARequestForItself(req, request);

        buildRepositoryMessageProducer.reportCompleted(req.getAgentRuntimeInfo(), req.getJobIdentifier(),
                req.getJobResult());

        return NOTHING;
    }

    public String isIgnored(Request request, Response response) {
        IsIgnoredRequest isIgnoredRequest = IsIgnoredRequestRepresenter.fromJSON(request.body());
        ensureAgentIsMakingARequestForItself(isIgnoredRequest, request);

        boolean isIgnored = buildRepositoryMessageProducer.isIgnored(isIgnoredRequest.getAgentRuntimeInfo(), isIgnoredRequest.getJobIdentifier());

        return valueOf(isIgnored);
    }

    public String getCookie(Request request, Response response) {
        GetCookieRequest getCookieRequest = GetCookieRequestRepresenter.fromJSON(request.body());
        ensureAgentIsMakingARequestForItself(getCookieRequest, request);

        return buildRepositoryMessageProducer.getCookie(getCookieRequest.getAgentRuntimeInfo());
    }

    public String getWork(Request request, Response response) {
        GetWorkRequest workRequest = GetWorkRequestRepresenter.fromJSON(request.body());
        ensureAgentIsMakingARequestForItself(workRequest, request);

        Work work = buildRepositoryMessageProducer.getWork(workRequest.getAgentRuntimeInfo());

        return WorkRepresenter.toJSON(work);
    }

    public void ensureAgentIsMakingARequestForItself(AgentRequest agentRequest, Request request) {
        String uuidInRuntimeInfo = agentRequest.getAgentRuntimeInfo().getUUId();
        String uuidInRequest = request.headers("X-Agent-GUID");

        if (!StringUtils.equals(uuidInRequest, uuidInRuntimeInfo)) {
            String message = String.format("Agent with uuid: '%s' is attempting a request for agent: '%s'.", uuidInRequest,
                    uuidInRuntimeInfo);
            haltBecauseForbidden(message);
        }
    }
}
