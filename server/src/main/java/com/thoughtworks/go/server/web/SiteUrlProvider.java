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
package com.thoughtworks.go.server.web;

import com.thoughtworks.go.server.service.ServerConfigService;
import com.thoughtworks.go.server.util.ServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.net.URISyntaxException;

@Component
public class SiteUrlProvider {
    private ServerConfigService urlProvider;
    private static final Logger LOGGER = LoggerFactory.getLogger(SiteUrlProvider.class.getName());

    @Autowired
    public SiteUrlProvider(ServerConfigService serverConfigService) {
        urlProvider = serverConfigService;
    }

    public String siteUrl(HttpServletRequest httpRequest) {
        String requestRootUrl = ServletHelper.getInstance().getRequest(httpRequest).getRootURL();
        String siteUrl = null;

        try {
            siteUrl = urlProvider.siteUrlFor(requestRootUrl, true);
            if (requestRootUrl.equals(siteUrl)) {
                siteUrl = urlProvider.siteUrlFor(requestRootUrl, false);
            }
        } catch (URISyntaxException e) {
            LOGGER.error("Error fetching site url, reasons: {}", e.getMessage(), e);
        }

        return siteUrl;
    }
}
