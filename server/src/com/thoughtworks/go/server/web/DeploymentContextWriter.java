/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.web;

import com.thoughtworks.go.server.util.ServletHelper;
import com.thoughtworks.go.server.util.ServletRequest;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.URISyntaxException;

/**
 * @understands sets https port the server is deployed on, in request attributes
 */
public class DeploymentContextWriter {

    public void writeSslPort(HttpServletRequest req) {
        req.setAttribute("ssl_port", env().getSslServerPort());
    }

    private SystemEnvironment env() {
        return new SystemEnvironment();
    }

    public void writeSecureSiteUrl(HttpServletRequest req) throws URISyntaxException {
        BaseUrlProvider provider = getBaseUrlProvider(req);
        if (provider == null) {
            throw new RuntimeException("Could not generate url. ServerConfigService not yet loaded.");
        }
        ServletRequest request = ServletHelper.getInstance().getRequest(req);
        String url = request.getUrl();
        if (provider.hasAnyUrlConfigured()) {
            try {
                String newUrl = provider.siteUrlFor(url, true);
                if (!url.equals(newUrl)) {
                    forceSsl(req, newUrl);
                }
            } catch (RuntimeException e) {
                req.setAttribute("ssl_unavailable_error", "true");
            }
        }
    }

    protected BaseUrlProvider getBaseUrlProvider(HttpServletRequest req) {
        WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(req.getSession().getServletContext());
        BaseUrlProvider provider = (BaseUrlProvider) context.getBean("serverConfigService");
        return provider;
    }

    private void forceSsl(HttpServletRequest req, String url) {
        req.setAttribute("secure_site", url);
        req.setAttribute("force_ssl", "true");
    }
}
