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
package com.thoughtworks.go.server.newsecurity.filterchains;

import com.thoughtworks.go.server.newsecurity.filters.ModeAwareFilter;
import com.thoughtworks.go.server.newsecurity.filters.ThreadLocalUserFilter;
import com.thoughtworks.go.server.newsecurity.handlers.RequestRejectedExceptionHandler;
import com.thoughtworks.go.server.web.BackupFilter;
import com.thoughtworks.go.server.web.FlashLoadingFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component("mainFilterChain")
public class MainFilterChain extends FilterChainProxy {
    private static final RequestRejectedExceptionHandler REQUEST_REJECTED_EXCEPTION_HANDLER = new RequestRejectedExceptionHandler();

    @Autowired
    public MainFilterChain(BackupFilter backupFilter,
                           ModeAwareFilter modeAwareFilter,
                           CreateSessionFilterChain createSessionFilterChain,
                           RememberLastRequestUrlFilterChain rememberLastRequestUrlFilterChain,
                           AuthenticationFilterChain authenticationFilterChain,
                           ThreadLocalUserFilter threadLocalUserFilter,
                           UserEnabledCheckFilterChain userEnabledFilterChain,
                           AuthorizeFilterChain authorizeFilterChain,
                           DenyGoCDAccessForArtifactsFilterChain denyGoCDAccessForArtifactsFilterChain,
                           ArtifactSizeEnforcementFilterChain artifactSizeEnforcementFilterChain,
                           FlashLoadingFilter flashLoadingFilter
                           ) {

        super(FilterChainBuilder.newInstance()
                .addFilterChain("/**",
                        backupFilter,
                        modeAwareFilter,
                        createSessionFilterChain,
                        rememberLastRequestUrlFilterChain,
                        authenticationFilterChain,
                        threadLocalUserFilter,
                        userEnabledFilterChain,
                        authorizeFilterChain,
                        denyGoCDAccessForArtifactsFilterChain,
                        artifactSizeEnforcementFilterChain,
                        flashLoadingFilter)
                .build());
    }

    @Override
    public void doFilter(ServletRequest req,
                         ServletResponse res,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        try {
            super.doFilter(request, response, chain);
        } catch (RequestRejectedException e) {
            REQUEST_REJECTED_EXCEPTION_HANDLER.handle(request, response, e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (HttpRequestMethodNotSupportedException e) {
            REQUEST_REJECTED_EXCEPTION_HANDLER.handle(request, response, e.getMessage(), HttpStatus.METHOD_NOT_ALLOWED);
        }
    }
}
