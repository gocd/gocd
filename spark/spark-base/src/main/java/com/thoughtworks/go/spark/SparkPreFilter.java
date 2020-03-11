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
package com.thoughtworks.go.spark;

import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.server.util.ServletHelper;
import com.thoughtworks.go.spark.spring.Application;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import spark.servlet.SparkApplication;
import spark.servlet.SparkFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.startsWithAny;

public class SparkPreFilter extends SparkFilter {

    private ServletHelper servletHelper;
    private WebApplicationContext wac;

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) req;
        boolean allowWithoutApiHeader = startsWithAny(request.getRequestURI().toLowerCase(),
            "/go/spark/api/plugin_images",
            "/go/spark/api/support",
            "/go/spark/api/v1/health",
            "/go/spark/api/internal/apis",
            "/go/spark/api/feed",
            "/go/spark/api/webhooks"
        );

        if (request.getRequestURI().startsWith("/go/spark/api/") &&
            !allowWithoutApiHeader &&
            noApiVersionInAcceptHeader((HttpServletRequest) req)) {
            render404((HttpServletResponse) resp);
            return;
        }
        String url = request.getRequestURI().replaceAll("^/go/spark/", "/go/");
        servletHelper.getRequest(request).setRequestURI(url);
        super.doFilter(req, resp, chain);
    }

    private void render404(HttpServletResponse response) throws IOException {
        response.setStatus(404);
        response.setCharacterEncoding("utf-8");
        response.setContentType("text/plain");
        response.getOutputStream().println("The url you are trying to reach appears to be incorrect.");
    }

    private boolean noApiVersionInAcceptHeader(HttpServletRequest req) {
        String acceptHeader = getAcceptHeader(req);
        if (isBlank(acceptHeader)) {
            return true;
        }


        return !ApiVersion.isValid(acceptHeader);
    }

    private String getAcceptHeader(HttpServletRequest req) {
        return req.getHeader("Accept");
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
        servletHelper = ServletHelper.getInstance();
        this.wac = WebApplicationContextUtils.getWebApplicationContext(config.getServletContext());
        super.init(config);
    }

    @Override
    protected SparkApplication[] getApplications(FilterConfig filterConfig) {
        return new SparkApplication[]{wac.getBean(Application.class)};
    }
}
