/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.security;

import com.thoughtworks.go.server.service.ArtifactsDirHolder;
import com.thoughtworks.go.server.util.LastOperationTime;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ArtifactSizeEnforcementFilter implements Filter {

    private ArtifactsDirHolder artifactsDirHolder;
    private SystemEnvironment systemEnvironment;
    private LastOperationTime lastOperationTime;
    private long totalAvailableSpace;
    private static final Logger LOG = LoggerFactory.getLogger(ArtifactSizeEnforcementFilter.class);


    @Autowired
    public ArtifactSizeEnforcementFilter(ArtifactsDirHolder artifactsDirHolder, SystemEnvironment systemEnvironment) {
        this.artifactsDirHolder = artifactsDirHolder;
        this.systemEnvironment = systemEnvironment;
        lastOperationTime = new LastOperationTime(systemEnvironment.getDiskSpaceCacheRefresherInterval());
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String headerValue = req.getHeader(SystemEnvironment.GO_ARTIFACT_PAYLOAD_SIZE_HEADER);

        if (lastOperationTime.pastWaitingPeriod()) {
            synchronized (this) {
                if (lastOperationTime.pastWaitingPeriod()) {
                    totalAvailableSpace = artifactsDirHolder.getArtifactsDir().getUsableSpace() - systemEnvironment.getArtifactReposiotryFullLimit() * GoConstants.MEGA_BYTE;
                    lastOperationTime.refresh();
                }
            }
        }

        if (!StringUtil.isBlank(headerValue) && Long.valueOf(headerValue) * 2 > totalAvailableSpace) {
            Long artifactSize = Long.valueOf(headerValue);
            LOG.error("[Artifact Upload] Artifact upload (Required Size {} * 2 = {}) was denied by the server because it has run out of disk space (Available Space {}).", artifactSize, artifactSize * 2, totalAvailableSpace);
            res.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        } else {
            chain.doFilter(req, response);
        }
    }

    public void init(FilterConfig filterConfig) throws ServletException {

    }

    public void destroy() {

    }
}
