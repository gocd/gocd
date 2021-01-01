/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.newsecurity.filters;

import com.thoughtworks.go.server.service.ArtifactsDirHolder;
import com.thoughtworks.go.server.util.LastOperationTime;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.HttpService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
public class ArtifactSizeEnforcementFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(ArtifactSizeEnforcementFilter.class);
    private final ArtifactsDirHolder artifactsDirHolder;
    private final SystemEnvironment systemEnvironment;
    // TODO: ketanpkr - DiskSpaceChecker already caches stuff, this class should not be needed.
    private LastOperationTime lastOperationTime;
    private long totalAvailableSpace;

    @Autowired
    public ArtifactSizeEnforcementFilter(ArtifactsDirHolder artifactsDirHolder, SystemEnvironment systemEnvironment) {
        this.artifactsDirHolder = artifactsDirHolder;
        this.systemEnvironment = systemEnvironment;
        this.lastOperationTime = new LastOperationTime(systemEnvironment.getDiskSpaceCacheRefresherInterval());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {
        String headerValue = request.getHeader(HttpService.GO_ARTIFACT_PAYLOAD_SIZE);
        if (isBlank(headerValue)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (lastOperationTime.pastWaitingPeriod()) {
            synchronized (this) {
                if (lastOperationTime.pastWaitingPeriod()) {
                    totalAvailableSpace = artifactsDirHolder.getArtifactsDir().getUsableSpace() - systemEnvironment.getArtifactReposiotryFullLimit() * GoConstants.MEGA_BYTE;
                    lastOperationTime.refresh();
                }
            }
        }

        if (Long.valueOf(headerValue) * 2 > totalAvailableSpace) {
            Long artifactSize = Long.valueOf(headerValue);
            LOG.error("[Artifact Upload] Artifact upload (Required Size {} * 2 = {}) was denied by the server because it has run out of disk space (Available Space {}).", artifactSize, artifactSize * 2, totalAvailableSpace);
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
