/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.util;

import com.thoughtworks.go.agent.ServerUrlGenerator;
import com.thoughtworks.go.domain.JobIdentifier;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

import static java.lang.String.format;

@Component
public class URLService implements ServerUrlGenerator{
    private String baseRemotingURL;

    public URLService() {
        String url = new SystemEnvironment().getServiceUrl();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        baseRemotingURL = url;
    }

    public URLService(String baseRemotingURL) {
        this.baseRemotingURL = baseRemotingURL;
    }

    public String baseRemoteURL() {
        return baseRemotingURL;
    }

    public String getBuildRepositoryURL() {
        return baseRemotingURL + "/remoting/remoteBuildRepository";
    }

    public String getAgentRegistrationURL() {
        return baseRemotingURL + "/admin/agent";
    }

    public String getAgentLatestStatusUrl() {
        return baseRemotingURL + "/admin/latest-agent.status";
    }

    public String getUploadUrlOfAgent(JobIdentifier jobIdentifier, String filePath) {
        return getUploadUrlOfAgent(jobIdentifier, filePath, 1);
    }

    public String getComponentVersionsOnServerUrl() {
        return String.format("%s/%s", baseRemotingURL, "admin/component-versions-on-server");
    }


    // TODO - keep buildId for now because currently we do not support 'jobcounter'
    // and therefore cannot locate job correctly when it is rescheduled
    public String getUploadUrlOfAgent(JobIdentifier jobIdentifier, String filePath, int attempt) {
        return format("%s/%s/%s/%s?attempt=%d&buildId=%d", baseRemotingURL, "remoting", "files", jobIdentifier.artifactLocator(filePath), attempt, jobIdentifier.getBuildId());
    }

    /*
     * Server will use this method, the base url is in the request.
     */
    public String getRestfulArtifactUrl(JobIdentifier jobIdentifier, String filePath) {
        return format("/%s/%s", "files", jobIdentifier.artifactLocator(filePath));
    }


    public String getUploadBaseUrlOfAgent(JobIdentifier jobIdentifier) {
        return format("%s/%s/%s/%s", baseRemotingURL, "remoting", "files", jobIdentifier.artifactLocator(""));
    }

    /*
    * Agent will use this method, the baseUrl will be injected from config xml in agent side.
    *   This is used to fix security issues with the agent uploading artifacts when security is enabled.
    */
    public String getPropertiesUrl(JobIdentifier jobIdentifier, String propertyName) {
        return format("%s/%s/%s/%s",
                baseRemotingURL, "remoting", "properties", jobIdentifier.propertyLocator(propertyName));
    }

    public String serverUrlFor(String subPath) {
        return format("%s/%s", baseRemotingURL, subPath);
    }

    public String getAgentRemoteWebSocketUrl() {
        return format("%s/%s", getWebSocketBaseUrl(), "agent-websocket");
    }
    public String getWebSocketBaseUrl() {
        try {
            URI uri = new URI(baseRemotingURL);
            StringBuffer ret = new StringBuffer("wss://");
            ret.append(uri.getHost()).append(":").append(uri.getPort());
            if (uri.getPath() != null) {
                ret.append(uri.getPath());
            }
            return ret.toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid Go Server url", e);
        }
    }

    public String prefixPartialUrl(String url) {
        if(url.startsWith("/")) {
            return format("%s%s", baseRemoteURL(), url);
        }
        return url;
    }
}
