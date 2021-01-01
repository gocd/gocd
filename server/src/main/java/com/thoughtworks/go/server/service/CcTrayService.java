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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.domain.activity.ProjectStatus;
import com.thoughtworks.go.domain.cctray.CcTrayCache;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/* Understands how to serve a request for the CcTray XML for the current user. */
@Service
public class CcTrayService {
    private CcTrayCache ccTrayCache;
    private GoConfigService goConfigService;

    @Autowired
    public CcTrayService(CcTrayCache ccTrayCache, GoConfigService goConfigService) {
        this.ccTrayCache = ccTrayCache;
        this.goConfigService = goConfigService;
    }

    public Appendable renderCCTrayXML(String siteUrlPrefix, String userName, Appendable appendable, Consumer<String> etagConsumer) {
        boolean isSecurityEnabled = goConfigService.isSecurityEnabled();
        List<ProjectStatus> statuses = ccTrayCache.allEntriesInOrder();

        String hashCodes = statuses.stream().map(ProjectStatus::hashCode).map(Object::toString).collect(Collectors.joining("/"));
        String etag = DigestUtils.sha256Hex(siteUrlPrefix + "/" + hashCodes);
        etagConsumer.accept(etag);

        try {
            appendable.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            appendable.append("\n");
            appendable.append("<Projects>");
            appendable.append("\n");
            for (ProjectStatus status : statuses) {
                if (!isSecurityEnabled || status.canBeViewedBy(userName)) {
                    String xmlRepresentation = status.xmlRepresentation().replaceAll(ProjectStatus.SITE_URL_PREFIX, siteUrlPrefix);
                    if (!StringUtils.isBlank(xmlRepresentation)) {
                        appendable.append("  ").append(xmlRepresentation).append("\n");
                    }
                }
            }

            appendable.append("</Projects>");
        } catch (IOException e) {
            // ignore. `StringBuilder#append` does not throw
        }

        return appendable;
    }
}
