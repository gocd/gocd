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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.activity.ProjectStatus;
import com.thoughtworks.go.domain.cctray.CcTrayCache;
import com.thoughtworks.go.server.util.UserHelper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public String getCcTrayXml(String siteUrlPrefix) {
        String userName = CaseInsensitiveString.str(UserHelper.getUserName().getUsername());
        boolean isSecurityEnabled = goConfigService.isSecurityEnabled();

        List<ProjectStatus> statuses = ccTrayCache.allEntriesInOrder();

        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<Projects>\n");
        for (ProjectStatus status : statuses) {
            if (!isSecurityEnabled || status.canBeViewedBy(userName)) {

                String xmlRepresentation = status.xmlRepresentation();
                if (!StringUtils.isBlank(xmlRepresentation)) {
                    xml.append("  ").append(xmlRepresentation).append("\n");
                }

            }
        }

        return xml.append("</Projects>").toString().replaceAll(ProjectStatus.SITE_URL_PREFIX, siteUrlPrefix);
    }
}
