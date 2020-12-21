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

package com.thoughtworks.go.agent;

import com.google.gson.Gson;
import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClient;
import com.thoughtworks.go.config.DefaultAgentRegistry;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.Serialization;
import com.thoughtworks.go.remote.request.*;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.URLService;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

import static com.thoughtworks.go.CurrentGoCDVersion.docsUrl;
import static com.thoughtworks.go.agent.ResponseHelpers.readBodyAsString;
import static com.thoughtworks.go.agent.ResponseHelpers.readBodyAsStringOrElse;
import static java.lang.String.format;

@Component
public class RemotingClient implements BuildRepositoryRemote {
    private static final Logger LOG = LoggerFactory.getLogger(RemotingClient.class);
    private static final String UUID_HEADER = "X-Agent-GUID";
    private static final String AUTH_HEADER = "Authorization";
    private static final Gson GSON = Serialization.instance();

    private final GoAgentServerHttpClient client;
    private final DefaultAgentRegistry agent;
    private final URLService urls;

    @Autowired
    public RemotingClient(GoAgentServerHttpClient client, DefaultAgentRegistry agent, URLService urls) {
        this.client = client;
        this.agent = agent;
        this.urls = urls;
    }

    @Override
    public AgentInstruction ping(AgentRuntimeInfo info) {
        return GSON.fromJson(post("ping", new PingRequest(info)), AgentInstruction.class);
    }

    @Override
    public Work getWork(AgentRuntimeInfo info) {
        return GSON.fromJson(post("get_work", new GetWorkRequest(info)), Work.class);
    }

    @Override
    public void reportCurrentStatus(AgentRuntimeInfo info, JobIdentifier jobId, JobState state) {
        post("report_current_status", new ReportCurrentStatusRequest(info, jobId, state));
    }

    @Override
    public void reportCompleting(AgentRuntimeInfo info, JobIdentifier jobId, JobResult result) {
        post("report_completing", new ReportCompleteStatusRequest(info, jobId, result));
    }

    @Override
    public void reportCompleted(AgentRuntimeInfo info, JobIdentifier jobId, JobResult result) {
        post("report_completed", new ReportCompleteStatusRequest(info, jobId, result));
    }

    @Override
    public boolean isIgnored(AgentRuntimeInfo info, JobIdentifier jobId) {
        // Boolean.parseBoolean is JSON compatible for this specific case, but probably faster/simpler than Gson
        return Boolean.parseBoolean(post("is_ignored", new IsIgnoredRequest(info, jobId)));
    }

    @Override
    public String getCookie(AgentRuntimeInfo info) {
        return GSON.fromJson(post("get_cookie", new GetCookieRequest(info)), String.class);
    }

    private String post(final String action, final AgentRequest payload) {
        try {
            try (CloseableHttpResponse response = client.execute(
                    injectCredentials(
                            postRequestFor(action, payload)
                    ))) {
                validateResponse(response, action);
                return readBodyAsString(response);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpRequestBase injectCredentials(final HttpRequestBase request) {
        request.setHeader(UUID_HEADER, agent.uuid());
        request.setHeader(AUTH_HEADER, agent.token());
        return request;
    }

    private void validateResponse(final HttpResponse response, final String action) throws IOException {
        final StatusLine status = response.getStatusLine();

        if (status.getStatusCode() >= 500) {
            logFailure(response, action);
            throw new ClientProtocolException(
                    format("The server returned an error with status code %d (%s); please check the server logs for the corresponding error.",
                            status.getStatusCode(), status.getReasonPhrase())
            );
        }

        if (status.getStatusCode() >= 400) {
            logFailure(response, action);
            throw new ClientProtocolException(String.join("\n   - ", List.of(
                    format("The server returned status code %d. Possible reasons include:", status.getStatusCode()),
                    "This agent has been deleted from the configuration",
                    "This agent is pending approval",
                    "There is possibly a reverse proxy (or load balancer) that has been misconfigured. See "
                            + docsUrl("/installation/configure-reverse-proxy.html#agents-and-reverse-proxies") +
                            " for details."
            )));
        }

        if (status.getStatusCode() >= 300) {
            throw new NoHttpResponseException(format("Did not receive successful HTTP response: status code = %d, status message = [%s]", status.getStatusCode(), status.getReasonPhrase()));
        }
    }

    private HttpRequestBase postRequestFor(String action, AgentRequest payload) {
        final HttpPost request = new HttpPost(urls.remotingUrlFor(action));
        request.addHeader("Accept", "application/vnd.go.cd+json");
        request.setEntity(new StringEntity(GSON.toJson(payload, AgentRequest.class), ContentType.APPLICATION_JSON));
        return request;
    }

    private void logFailure(final HttpResponse response, final String action) {
        final StatusLine status = response.getStatusLine();
        final String body = readBodyAsStringOrElse(response, "<ERROR: UNABLE TO READ RESPONSE BODY>");
        LOG.error(format("Server responded to action `%s` with: status[%d %s], body[%s]",
                action, status.getStatusCode(), status.getReasonPhrase(), body));
    }
}
