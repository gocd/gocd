/*
 * Copyright 2024 Thoughtworks, Inc.
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

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.stereotype.Component;

import static java.lang.String.format;

@Component
public class URLService implements ServerUrlGenerator {
    private final String baseRemotingURL;

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

    public String remotingUrlFor(String action) {
        return format("%s/remoting/api/agent/%s", baseRemotingURL, action);
    }

    public String getAgentRegistrationURL() {
        return baseRemotingURL + "/admin/agent";
    }

    public String getTokenURL() {
        return baseRemotingURL + "/admin/agent/token";
    }

    public String getAgentLatestStatusUrl() {
        return baseRemotingURL + "/admin/latest-agent.status";
    }

    public String getUploadUrlOfAgent(JobIdentifier jobIdentifier, String filePath) {
        return getUploadUrlOfAgent(jobIdentifier, filePath, 1);
    }

    public String getUploadUrlOfAgent(JobIdentifier jobIdentifier, String filePath, int attempt) {
        return format("%s/remoting/files/%s?attempt=%d&buildId=%d", baseRemotingURL, jobIdentifier.artifactLocator(filePath), attempt, jobIdentifier.getBuildId());
    }

    /*
     * Server will use this method, the base url is in the request.
     */
    public String getRestfulArtifactUrl(JobIdentifier jobIdentifier, String filePath) {
        return format("/files/%s", jobIdentifier.artifactLocator(filePath));
    }

    @Override
    public String serverUrlFor(String subPath) {
        return format("%s/%s", baseRemotingURL, subPath);
    }


}
