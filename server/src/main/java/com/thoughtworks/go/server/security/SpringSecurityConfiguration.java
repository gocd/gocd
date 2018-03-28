/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.server.web.ApiSessionFilter;
import com.thoughtworks.go.server.web.FlashLoadingFilter;
import com.thoughtworks.go.server.web.GoUrlRewriteFilter;
import com.thoughtworks.go.server.web.i18n.LocaleResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@Order(1)
public class SpringSecurityConfiguration {
    @Autowired
    private ModeAwareFilter modeAwareFilter;
    @Autowired
    private ArtifactSizeEnforcementFilter artifactSizeEnforcementFilter;
    @Autowired
    private LocaleResolver i18nlocaleResolver;
    private Filter filterInvocationInterceptor = new NoOpFilter();
    @Autowired
    private FlashLoadingFilter flashLoader;
    @Autowired
    private DenyGoCDAccessForArtifactsFilter denyGoCDAccessForArtifactsFilter;

    @Autowired
    private GoUrlRewriteFilter urlRewriter;

    //    @Autowired
    private Filter basicAuthenticationAccessDenied = new NoOpFilter();
    //    @Autowired
    private Filter cruiseLoginOrBasicAuthentication = new NoOpFilter();
    @Autowired
    private AnonymousProcessingFilter anonymousProcessingFilter;
    @Autowired
    private ApiSessionFilter apiSessionFilter;
    //    @Autowired
    private Filter authenticationProcessingFilter = new NoOpFilter();
    @Autowired
    private RemoveAdminPermissionFilter removeAdminPermissionFilter;
    //    @Autowired
    private Filter agentRemotingFilterInvocationInterceptor = new NoOpFilter();
    @Autowired
    private ReAuthenticationFilter reAuthenticationFilter;
    //    @Autowired
//    private PreAuthenticatedRequestsProcessingFilter preAuthenticationFilter;
    @Autowired
    private WebBasedAuthenticationFilter webBasedAuthFilter;
    @Autowired
    private OauthAuthenticationFilter oauthProcessingFilter;
    @Autowired
    private DisallowExternalReAuthenticationFilter disallowExternalReAuthenticationFilter;

    @Bean(name = "filterChainProxy")
    public FilterChainProxy getFilterChainProxy() {
        List<SecurityFilterChain> listOfFilterChains = new ArrayList<>();
        listOfFilterChains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/api/config-repository.git/**"), modeAwareFilter, apiSessionFilter, removeAdminPermissionFilter, authenticationProcessingFilter, anonymousProcessingFilter, basicAuthenticationAccessDenied, denyGoCDAccessForArtifactsFilter, filterInvocationInterceptor));
        listOfFilterChains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/remoting/**"), modeAwareFilter, artifactSizeEnforcementFilter, i18nlocaleResolver, agentRemotingFilterInvocationInterceptor, flashLoader, urlRewriter));
        listOfFilterChains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/agent-websocket/**"), modeAwareFilter, artifactSizeEnforcementFilter, i18nlocaleResolver, agentRemotingFilterInvocationInterceptor));
        listOfFilterChains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/cctray.xml"), modeAwareFilter, i18nlocaleResolver, apiSessionFilter, removeAdminPermissionFilter, oauthProcessingFilter, authenticationProcessingFilter, reAuthenticationFilter, anonymousProcessingFilter, basicAuthenticationAccessDenied, denyGoCDAccessForArtifactsFilter, filterInvocationInterceptor, flashLoader, urlRewriter));
        listOfFilterChains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/api/**"), modeAwareFilter, i18nlocaleResolver, apiSessionFilter, removeAdminPermissionFilter, oauthProcessingFilter, authenticationProcessingFilter, reAuthenticationFilter, anonymousProcessingFilter, basicAuthenticationAccessDenied, denyGoCDAccessForArtifactsFilter, filterInvocationInterceptor, flashLoader, urlRewriter));
        listOfFilterChains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/files/**"), modeAwareFilter, artifactSizeEnforcementFilter, i18nlocaleResolver, removeAdminPermissionFilter, oauthProcessingFilter, authenticationProcessingFilter, reAuthenticationFilter, anonymousProcessingFilter, cruiseLoginOrBasicAuthentication, filterInvocationInterceptor, flashLoader, urlRewriter));
        listOfFilterChains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/**"), modeAwareFilter, i18nlocaleResolver, disallowExternalReAuthenticationFilter, removeAdminPermissionFilter, oauthProcessingFilter, webBasedAuthFilter, authenticationProcessingFilter, reAuthenticationFilter, anonymousProcessingFilter, cruiseLoginOrBasicAuthentication, denyGoCDAccessForArtifactsFilter, filterInvocationInterceptor, flashLoader, urlRewriter));
        return new FilterChainProxy(listOfFilterChains);
    }

    private static class NoOpFilter implements Filter {
        @Override
        public void init(FilterConfig filterConfig) throws ServletException {

        }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
            filterChain.doFilter(servletRequest, servletResponse);
        }

        @Override
        public void destroy() {

        }
    }
}
