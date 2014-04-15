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

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.server.service.GoLicenseService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * @understands enforcing license and config validity
 */
public class ConfigValidityFilter implements Filter {
    private static final Log LOGGER = LogFactory.getLog(ConfigValidityFilter.class);

    private LicenseInterceptor licenseInterceptor;
    private boolean logRequestTimings;

    @Autowired ConfigValidityFilter(LicenseInterceptor licenseInterceptor) {
        this.licenseInterceptor = licenseInterceptor;
    }

    public void init(FilterConfig config) {
        WebApplicationContext appContext = WebApplicationContextUtils.getWebApplicationContext(config.getServletContext());
        if (this.licenseInterceptor == null) {
            GoLicenseService licenseService = (GoLicenseService) appContext.getBean("goLicenseService");
            licenseInterceptor = new LicenseInterceptor(licenseService);
        }
        logRequestTimings = new SystemEnvironment().getEnableRequestTimeLogging();
    }

    private boolean licenseValid(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException {
        try {
            return this.licenseInterceptor.preHandle(httpServletRequest, httpServletResponse, null);
        } catch (Exception e) {
            throw new ServletException("Exception while verifying license", e);
        }
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        final HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        try {
            final HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
            if (licenseValid(httpServletRequest, httpServletResponse)) {
                filterChain.doFilter(servletRequest, servletResponse);
            }
        } finally {
            if (logRequestTimings)
                LOGGER.warn(httpServletRequest.getRequestURI() + " took: " + (System.currentTimeMillis() - start) + " ms");
        }
    }

    public void destroy() {

    }
}
